package com.taskflow.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {

    private const val GITHUB_USER = "usabbir446"
    private const val GITHUB_REPO = "task-manager"
    val CURRENT_VERSION_CODE: Int get() = com.taskflow.app.BuildConfig.VERSION_CODE
    val CURRENT_VERSION_NAME: String get() = com.taskflow.app.BuildConfig.VERSION_NAME

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionName: String,
        val latestVersionCode: Int,
        val downloadUrl: String?,
        val releaseNotes: String?,
        val errorMessage: String? = null
    )

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        // 1. Try reading version.json from main branch
        try {
            val manifestUrl = URL("https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/version.json")
            val conn = (manifestUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val remoteCode = json.optInt("versionCode", 0)
                val remoteName = json.optString("versionName", "Latest")
                val downloadUrl = json.optString("downloadUrl", "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/main/app-release.apk")
                val releaseNotes = json.optString("releaseNotes", "Bug fixes and performance improvements")

                if (remoteCode > CURRENT_VERSION_CODE) {
                    return@withContext UpdateInfo(
                        isUpdateAvailable = true,
                        latestVersionName = remoteName,
                        latestVersionCode = remoteCode,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                } else {
                    return@withContext UpdateInfo(
                        isUpdateAvailable = false,
                        latestVersionName = remoteName,
                        latestVersionCode = remoteCode,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Check GitHub Releases API
        try {
            val apiUrl = URL("https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest")
            val connection = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TaskFlow-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "1.0")
                val releaseNotes = json.optString("body", "Bug fixes and improvements")

                var apkDownloadUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                return@withContext UpdateInfo(
                    isUpdateAvailable = apkDownloadUrl != null,
                    latestVersionName = tagName,
                    latestVersionCode = CURRENT_VERSION_CODE + 1,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = releaseNotes
                )
            }
        } catch (e: Exception) {
            return@withContext UpdateInfo(
                isUpdateAvailable = false,
                latestVersionName = CURRENT_VERSION_NAME,
                latestVersionCode = CURRENT_VERSION_CODE,
                downloadUrl = null,
                releaseNotes = null,
                errorMessage = e.localizedMessage
            )
        }

        UpdateInfo(
            isUpdateAvailable = false,
            latestVersionName = CURRENT_VERSION_NAME,
            latestVersionCode = CURRENT_VERSION_CODE,
            downloadUrl = null,
            releaseNotes = null
        )
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "TaskFlow_update.apk"
            val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val targetFile = File(targetDir, fileName)
            if (targetFile.exists()) targetFile.delete()

            var currentUrl = downloadUrl
            var redirectCount = 0
            var connection: HttpURLConnection

            // Handle HTTP 301/302/307 redirects from GitHub / raw
            while (true) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "TaskFlow-App")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    currentUrl = newUrl
                    redirectCount++
                    if (redirectCount > 5) throw Exception("Too many redirects")
                } else {
                    break
                }
            }

            val fileLength = connection.contentLength
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val percent = ((total * 100) / fileLength).toInt()
                            withContext(Dispatchers.Main) { onProgress(percent) }
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                installDownloadedApk(context, targetFile)
            }
            true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }

    fun installDownloadedApk(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "Downloaded installer file missing", Toast.LENGTH_SHORT).show()
            return
        }

        // Android 8.0+ Check for REQUEST_INSTALL_PACKAGES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Please allow 'Install unknown apps' to update TaskFlow", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installer failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
