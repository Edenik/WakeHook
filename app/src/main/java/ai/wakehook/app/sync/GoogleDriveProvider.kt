package ai.wakehook.app.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real [SyncProvider] backed by Google Drive REST v3, scoped to `drive.file` (the app can only
 * see files it created). Stores everything inside an app-owned `WakeHook/` folder.
 *
 * The live round-trip (folder/file creation, read/write against an actual Drive account) is not
 * covered by automated tests here — it requires a registered OAuth client + a real Google
 * account (see `docs/superpowers/plans/oauth-setup.md`). This class is exercised only by
 * construction/compile-level checks; the sync engine itself is tested via [FakeSyncProvider].
 */
class GoogleDriveProvider(
    private val context: Context,
    private val account: GoogleSignInAccount,
) : SyncProvider {

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val drive: Drive by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE),
        ).apply { selectedAccount = account.account }

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential,
        ).setApplicationName(APPLICATION_NAME).build()
    }

    private var folderId: String?
        get() = prefs.getString(KEY_FOLDER_ID, null)
        set(value) { prefs.edit().putString(KEY_FOLDER_ID, value).apply() }

    private var jsonFileId: String?
        get() = prefs.getString(KEY_JSON_FILE_ID, null)
        set(value) { prefs.edit().putString(KEY_JSON_FILE_ID, value).apply() }

    override suspend fun ensureFolderAndFiles(seedJson: String, seedMd: String) = withContext(Dispatchers.IO) {
        val folder = findOrCreateFolder()
        folderId = folder

        val existingJson = findFileInFolder(folder, JSON_FILE_NAME)
        val jsonId = if (existingJson != null) {
            existingJson
        } else {
            createFile(folder, JSON_FILE_NAME, JSON_MIME_TYPE, seedJson)
        }
        jsonFileId = jsonId

        val existingMd = findFileInFolder(folder, MD_FILE_NAME)
        if (existingMd == null) {
            createFile(folder, MD_FILE_NAME, MD_MIME_TYPE, seedMd)
        }
        Unit
    }

    override suspend fun readJson(): RemoteFile? = withContext(Dispatchers.IO) {
        val id = jsonFileId ?: run {
            val folder = folderId ?: findOrCreateFolder().also { folderId = it }
            findFileInFolder(folder, JSON_FILE_NAME)
        } ?: return@withContext null

        jsonFileId = id

        val meta = try {
            drive.files().get(id).setFields("id, modifiedTime, version").execute()
        } catch (e: Exception) {
            return@withContext null
        }

        val content = drive.files().get(id).executeMediaAsInputStream()
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        RemoteFile(content, etagOf(meta))
    }

    override suspend fun writeJson(content: String): String = withContext(Dispatchers.IO) {
        val id = jsonFileId ?: run {
            val folder = folderId ?: findOrCreateFolder().also { folderId = it }
            findFileInFolder(folder, JSON_FILE_NAME) ?: createFile(folder, JSON_FILE_NAME, JSON_MIME_TYPE, content)
        }
        jsonFileId = id

        val body = ByteArrayContent(JSON_MIME_TYPE, content.toByteArray(Charsets.UTF_8))
        val updated = drive.files().update(id, null, body).setFields("id, modifiedTime, version").execute()
        etagOf(updated)
    }

    override suspend fun isConnected(): Boolean = withContext(Dispatchers.IO) {
        account.account != null
    }

    private fun findOrCreateFolder(): String {
        val existing = drive.files().list()
            .setQ("name='$FOLDER_NAME' and mimeType='$FOLDER_MIME_TYPE' and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
            .files
            ?.firstOrNull()
        if (existing != null) return existing.id

        val metadata = DriveFile().apply {
            name = FOLDER_NAME
            mimeType = FOLDER_MIME_TYPE
        }
        return drive.files().create(metadata).setFields("id").execute().id
    }

    private fun findFileInFolder(folderId: String, name: String): String? {
        return drive.files().list()
            .setQ("name='$name' and '$folderId' in parents and trashed=false")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
            .files
            ?.firstOrNull()
            ?.id
    }

    private fun createFile(folderId: String, name: String, mimeType: String, content: String): String {
        val metadata = DriveFile().apply {
            this.name = name
            this.mimeType = mimeType
            parents = listOf(folderId)
        }
        val body = ByteArrayContent(mimeType, content.toByteArray(Charsets.UTF_8))
        return drive.files().create(metadata, body).setFields("id").execute().id
    }

    private fun etagOf(file: DriveFile): String = file.modifiedTime?.toString() ?: file.version?.toString() ?: "0"

    companion object {
        private const val PREFS_NAME = "wakehook_drive"
        private const val KEY_FOLDER_ID = "folder_id"
        private const val KEY_JSON_FILE_ID = "json_file_id"

        private const val APPLICATION_NAME = "WakeHook"
        private const val FOLDER_NAME = "WakeHook"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val JSON_FILE_NAME = "wakehook.json"
        private const val MD_FILE_NAME = "wakehook.md"
        private const val JSON_MIME_TYPE = "application/json"
        private const val MD_MIME_TYPE = "text/markdown"
    }
}
