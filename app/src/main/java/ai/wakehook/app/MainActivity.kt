package ai.wakehook.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import ai.wakehook.app.ui.AppContainer
import ai.wakehook.app.ui.edit.AlarmEditScreen
import ai.wakehook.app.ui.edit.AlarmEditViewModel
import ai.wakehook.app.ui.list.AlarmListScreen
import ai.wakehook.app.ui.list.AlarmListViewModel
import ai.wakehook.app.ui.settings.PermissionState
import ai.wakehook.app.ui.settings.SettingsScreen
import ai.wakehook.app.ui.theme.WakeHookTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    private val requestNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* status re-read on resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as WakeHookApp).container
        if (Build.VERSION.SDK_INT >= 33) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            WakeHookTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "list") {
                    composable("list") {
                        val vm = AlarmListViewModel(container.repository, container.scheduler)
                        AlarmListScreen(vm,
                            onAdd = { nav.navigate("edit") },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
                            onSettings = { nav.navigate("settings") })
                    }
                    composable("edit") { EditRoute(nav, null) }
                    composable("edit?id={id}") { back ->
                        EditRoute(nav, back.arguments?.getString("id"))
                    }
                    composable("settings") {
                        SettingsScreen(
                            status = PermissionState.read(this@MainActivity),
                            onFixExactAlarm = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) },
                            onFixNotifications = { startActivity(appSettings()) },
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
                            })
                    }
                }
            }
        }
    }

    @Composable
    private fun EditRoute(nav: androidx.navigation.NavController, id: String?) {
        val vm = AlarmEditViewModel(container.repository, container.scheduler)
        AlarmEditScreen(vm, id) { nav.popBackStack() }
    }

    private fun appSettings() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"))
}
