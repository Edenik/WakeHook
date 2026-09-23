package ai.wakehook.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ai.wakehook.app.ui.settings.PermissionStatus
import ai.wakehook.app.ui.AgentPrompt
import ai.wakehook.app.ui.AppContainer
import ai.wakehook.app.ui.edit.AlarmEditScreen
import ai.wakehook.app.ui.edit.AlarmEditViewModel
import ai.wakehook.app.ui.list.AlarmListScreen
import ai.wakehook.app.ui.list.AlarmListViewModel
import ai.wakehook.app.ui.settings.PermissionState
import ai.wakehook.app.ui.settings.SettingsScreen
import ai.wakehook.app.ui.onboarding.OnboardingFlow
import ai.wakehook.app.ui.onboarding.OnboardingState
import ai.wakehook.app.ui.theme.WakeHookTheme
import ai.wakehook.app.sync.AndroidAgentNotifier
import ai.wakehook.app.sync.GoogleDriveProvider
import ai.wakehook.app.sync.SyncEngine
import ai.wakehook.app.sync.SyncState
import ai.wakehook.app.sync.SyncTrigger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private var permissionStatus by mutableStateOf<PermissionStatus?>(null)

    private val requestNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionStatus = PermissionState.read(this) }

    /** Requests only `drive.file` — the app can see files it creates, nothing else in the user's Drive. */
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso)
    }

    /**
     * Handles the Google Sign-In result. Signing in WILL fail until a real OAuth client is
     * registered for this app (see `docs/superpowers/plans/oauth-setup.md`) — that failure path
     * must stay graceful (a toast, no crash), which is exactly what's tested here by hand since
     * it needs a live account.
     */
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val provider = GoogleDriveProvider(this, account)
            container.syncProvider = provider
            SyncState(this).setConnected(true)

            CoroutineScope(Dispatchers.IO).launch {
                val engine = SyncEngine(
                    provider = provider,
                    repo = container.repository,
                    scheduler = container.scheduler,
                    tombstones = container.tombstones,
                    notifier = AndroidAgentNotifier(applicationContext),
                    state = SyncState(applicationContext),
                )
                try {
                    engine.firstConnect()
                } catch (e: Exception) {
                    // First-sync network/API failures shouldn't crash the app; periodic sync retries.
                }
            }
            SyncTrigger.schedulePeriodic(container.appContext)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed (${e.statusCode})", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0E0F14.toInt()
        window.navigationBarColor = 0xFF0E0F14.toInt()
        container = (application as WakeHookApp).container
        seedLocalExamplesOnce()
        permissionStatus = PermissionState.read(this)

        setContent {
            WakeHookTheme {
                val nav = rememberNavController()
                val syncPrefs = remember { getSharedPreferences(SyncState.PREFS_NAME, MODE_PRIVATE) }
                var driveConnected by remember { mutableStateOf(SyncState(this@MainActivity).connected()) }
                var lastSync by remember { mutableLongStateOf(SyncState(this@MainActivity).lastSync()) }
                DisposableEffect(syncPrefs) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                        driveConnected = SyncState(this@MainActivity).connected()
                        lastSync = SyncState(this@MainActivity).lastSync()
                    }
                    syncPrefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose { syncPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
                }
                val start = if (OnboardingState(this@MainActivity).isDone()) "list" else "onboarding"
                NavHost(nav, startDestination = start) {
                    composable("onboarding") {
                        OnboardingFlow(
                            status = permissionStatus ?: PermissionState.read(this@MainActivity),
                            driveConnected = driveConnected,
                            onFixExactAlarm = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) },
                            onFixNotifications = { requestNotificationPermission() },
                            onFixBattery = {
                                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:$packageName")))
                            },
                            onFixFullScreen = {
                                startActivity(
                                    if (Build.VERSION.SDK_INT >= 34)
                                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
                                    else appSettings()
                                )
                            },
                            onConnectDrive = { signInLauncher.launch(googleSignInClient.signInIntent) },
                            onCopyPrompt = { copyAgentPrompt() },
                            onRevoke = { revokeAccess() },
                            onFinish = {
                                OnboardingState(this@MainActivity).setDone()
                                nav.navigate("list") { popUpTo("onboarding") { inclusive = true } }
                            },
                        )
                    }
                    composable("list") {
                        val vm: AlarmListViewModel = viewModel(factory = viewModelFactory {
                            initializer { AlarmListViewModel(container.repository, container.scheduler, container.tombstones) {
                                SyncTrigger.now(container.appContext)
                            } }
                        })
                        AlarmListScreen(vm,
                            onAdd = { nav.navigate("edit") },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
                            onSettings = { nav.navigate("settings") },
                            exactAlarmAllowed = permissionStatus?.exactAlarm ?: false)
                    }
                    composable("edit") { EditRoute(nav, null) }
                    composable("edit?id={id}") { back ->
                        EditRoute(nav, back.arguments?.getString("id"))
                    }
                    composable("settings") {
                        SettingsScreen(
                            status = permissionStatus ?: PermissionState.read(this@MainActivity),
                            onFixExactAlarm = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) },
                            onFixNotifications = { requestNotificationPermission() },
                            onFixBattery = {
                                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:$packageName")))
                            },
                            onFixFullScreenIntent = {
                                startActivity(
                                    if (Build.VERSION.SDK_INT >= 34)
                                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:$packageName"))
                                    else appSettings()
                                )
                            },
                            driveConnected = driveConnected,
                            lastSyncMillis = lastSync,
                            onConnectDrive = { signInLauncher.launch(googleSignInClient.signInIntent) },
                            onSyncNow = { SyncTrigger.now(container.appContext) },
                            onCopyPrompt = { copyAgentPrompt() },
                            onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }

    @Composable
    private fun EditRoute(nav: androidx.navigation.NavController, id: String?) {
        val vm: AlarmEditViewModel = viewModel(factory = viewModelFactory {
            initializer { AlarmEditViewModel(container.repository, container.scheduler) {
                SyncTrigger.now(container.appContext)
            } }
        })
        AlarmEditScreen(vm, id, onDone = { nav.popBackStack() }, onFixExactAlarm = {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
        })
    }

    override fun onResume() {
        super.onResume()
        permissionStatus = PermissionState.read(this)
        SyncTrigger.now(container.appContext)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !PermissionState.read(this).notifications)
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        else startActivity(appSettings())
    }

    private fun appSettings() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"))

    private fun copyAgentPrompt() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("WakeHook agent prompt", AgentPrompt.build(null)))
        Toast.makeText(this, getString(R.string.copied_agent_prompt), Toast.LENGTH_SHORT).show()
    }

    /** One-tap revoke — opens the Google account permissions page so the user can cut WakeHook off. */
    private fun revokeAccess() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/permissions")))
    }

    /**
     * First launch: drop the disabled example alarms into the local DB so the list is never blank,
     * even before Google Drive is connected. Stable seed ids dedupe with the Drive seed once
     * connected. Runs once (flag-guarded), only if the DB is empty. Lives here (not in the
     * Application) so it never runs in unit tests, which construct WakeHookApp but not MainActivity.
     */
    private fun seedLocalExamplesOnce() {
        val prefs = getSharedPreferences("wakehook_local_seed", MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (container.repository.getAll().isEmpty()) {
                    ai.wakehook.app.sync.Seed.exampleAlarms().forEach { container.repository.upsert(it) }
                }
            } finally {
                prefs.edit().putBoolean("done", true).apply()
            }
        }
    }
}
