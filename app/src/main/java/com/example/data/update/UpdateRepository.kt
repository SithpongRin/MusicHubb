package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.domain.models.AppVersionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long, val progress: Float) : UpdateDownloadState()
    object VerifyingChecksum : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

class UpdateRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private var currentDownloadCall: okhttp3.Call? = null

    val currentVersionCode: Int = BuildConfig.VERSION_CODE
    val currentVersionName: String = BuildConfig.VERSION_NAME

    suspend fun checkForUpdates(manifestUrl: String = DEFAULT_UPDATE_URL): Result<AppVersionManifest?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(manifestUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
            }

            val jsonStr = response.body?.string() ?: return@withContext Result.failure(IllegalStateException("Empty body"))
            val json = JSONObject(jsonStr)

            val remoteVersionCode = json.getInt("versionCode")
            val remoteVersionName = json.getString("versionName")
            val apkUrl = json.getString("apkUrl")
            val sha256 = json.getString("sha256")
            val forceUpdate = json.optBoolean("forceUpdate", false)
            val minSupportedVersionCode = json.optInt("minSupportedVersionCode", 1)

            val changelogObj = json.optJSONObject("changelog")
            val changelogEn = mutableListOf<String>()
            val changelogKm = mutableListOf<String>()

            changelogObj?.optJSONArray("en")?.let { arr ->
                for (i in 0 until arr.length()) changelogEn.add(arr.getString(i))
            }
            changelogObj?.optJSONArray("km")?.let { arr ->
                for (i in 0 until arr.length()) changelogKm.add(arr.getString(i))
            }

            val manifest = AppVersionManifest(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                apkUrl = apkUrl,
                sha256 = sha256,
                changelogEn = changelogEn,
                changelogKm = changelogKm,
                forceUpdate = forceUpdate || currentVersionCode < minSupportedVersionCode,
                minSupportedVersionCode = minSupportedVersionCode
            )

            if (isUpdateAvailable(remoteVersionCode)) {
                Result.success(manifest)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isUpdateAvailable(remoteVersionCode: Int): Boolean {
        return remoteVersionCode > currentVersionCode
    }

    fun isUpdateRequired(manifest: AppVersionManifest): Boolean {
        return manifest.forceUpdate || currentVersionCode < manifest.minSupportedVersionCode
    }

    suspend fun downloadAndVerifyApk(apkUrl: String, expectedSha256: String) = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val destinationFile = File(updatesDir, "MusicHub-update.apk")
        if (destinationFile.exists()) destinationFile.delete()

        try {
            val request = Request.Builder().url(apkUrl).build()
            val call = okHttpClient.newCall(request)
            currentDownloadCall = call
            val response = call.execute()

            if (!response.isSuccessful) {
                _downloadState.value = UpdateDownloadState.Error("HTTP error: ${response.code}")
                return@withContext
            }

            val body = response.body ?: run {
                _downloadState.value = UpdateDownloadState.Error("Empty response body")
                return@withContext
            }

            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloaded = 0L

            _downloadState.value = UpdateDownloadState.Downloading(0L, totalBytes, 0f)

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                _downloadState.value = UpdateDownloadState.Downloading(downloaded, totalBytes, progress)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Verify SHA-256
            _downloadState.value = UpdateDownloadState.VerifyingChecksum
            val calculatedHash = calculateSha256(destinationFile)

            if (expectedSha256.isNotBlank() && !expectedSha256.equals("REPLACE_WITH_REAL_SHA256", ignoreCase = true)) {
                if (!calculatedHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    destinationFile.delete()
                    _downloadState.value = UpdateDownloadState.Error("SHA-256 checksum mismatch")
                    return@withContext
                }
            }

            _downloadState.value = UpdateDownloadState.ReadyToInstall(destinationFile)
        } catch (e: Exception) {
            destinationFile.delete()
            _downloadState.value = UpdateDownloadState.Error(e.message ?: "Download failed")
        } finally {
            currentDownloadCall = null
        }
    }

    fun cancelDownload() {
        currentDownloadCall?.cancel()
        currentDownloadCall = null
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        const val DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/sithpongrin4/MusicHub/main/version.json"

        fun calculateSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
