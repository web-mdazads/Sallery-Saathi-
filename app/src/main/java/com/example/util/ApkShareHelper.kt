package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ApkShareHelper {

    fun shareInstalledApk(
        context: Context,
        scope: CoroutineScope,
        onStatusToast: (String) -> Unit = {}
    ) {
        scope.launch {
            val caption = "Install Salary Saathi — smart salary-cycle tracker, UPI receipt manager & AI companion crafted by MD Azad (https://mdazads.vercel.app)"
            val apkUri = withContext(Dispatchers.IO) {
                runCatching {
                    val sourcePath = context.applicationInfo.publicSourceDir
                        ?: context.applicationInfo.sourceDir
                    if (sourcePath.isNullOrBlank()) return@runCatching null
                    val sourceFile = File(sourcePath)
                    if (!sourceFile.exists() || !sourceFile.canRead()) return@runCatching null

                    val shareDir = File(context.cacheDir, "shared_apk").apply { mkdirs() }
                    val targetApk = File(shareDir, "Salary_Saathi_v1.0.apk")
                    sourceFile.inputStream().use { input ->
                        targetApk.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        targetApk
                    )
                }.getOrNull()
            }

            runCatching {
                if (apkUri != null) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.android.package-archive"
                        putExtra(Intent.EXTRA_STREAM, apkUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Salary Saathi Android App (APK)")
                        putExtra(Intent.EXTRA_TEXT, caption)
                        clipData = ClipData.newRawUri("Salary_Saathi_v1.0.apk", apkUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(sendIntent, "Send Salary Saathi APK").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(chooser)
                    onStatusToast("Sharing Salary Saathi APK…")
                } else {
                    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Salary Saathi — Personal Salary Companion")
                        putExtra(Intent.EXTRA_TEXT, caption)
                    }
                    val chooser = Intent.createChooser(fallbackIntent, "Share Salary Saathi").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    onStatusToast("Sharing Salary Saathi…")
                }
            }.onFailure {
                onStatusToast("Unable to open share sheet on this device.")
            }
        }
    }
}
