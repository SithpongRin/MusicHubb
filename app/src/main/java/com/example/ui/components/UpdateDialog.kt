package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.download.MediaExtractor
import com.example.data.update.UpdateDownloadState
import com.example.domain.models.AppVersionManifest

@Composable
fun UpdateDialog(
    manifest: AppVersionManifest,
    downloadState: UpdateDownloadState,
    isKhmer: Boolean,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit,
    onOpenPermissionSettings: () -> Unit,
    canInstallPackages: Boolean,
    onDismiss: () -> Unit
) {
    val changelogList = if (isKhmer && manifest.changelogKm.isNotEmpty()) {
        manifest.changelogKm
    } else {
        manifest.changelogEn
    }

    AlertDialog(
        onDismissRequest = {
            if (!manifest.forceUpdate) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.update_dialog_msg, manifest.versionName),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (changelogList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_dialog_changelog),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    changelogList.forEach { item ->
                        Text(
                            text = "- $item",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadState) {
                    is UpdateDownloadState.Idle -> {
                        // Ready to start
                    }
                    is UpdateDownloadState.Downloading -> {
                        val downloadedStr = MediaExtractor.formatFileSize(downloadState.downloadedBytes)
                        val totalStr = MediaExtractor.formatFileSize(downloadState.totalBytes)
                        val percent = (downloadState.progress * 100).toInt()

                        Text(
                            text = stringResource(R.string.updating_download_progress, downloadedStr, totalStr, percent),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is UpdateDownloadState.VerifyingChecksum -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.verifying_sha256),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        if (!canInstallPackages) {
                            Text(
                                text = stringResource(R.string.install_permission_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is UpdateDownloadState.Idle -> {
                    Button(onClick = onStartDownload) {
                        Text(stringResource(R.string.action_update_now))
                    }
                }
                is UpdateDownloadState.Downloading -> {
                    OutlinedButton(onClick = onCancelDownload) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    if (canInstallPackages) {
                        Button(onClick = { onInstall(downloadState.apkFile) }) {
                            Text(stringResource(R.string.action_install))
                        }
                    } else {
                        Button(onClick = onOpenPermissionSettings) {
                            Text(stringResource(R.string.action_open_settings))
                        }
                    }
                }
                is UpdateDownloadState.Error -> {
                    Button(onClick = onStartDownload) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (!manifest.forceUpdate && downloadState !is UpdateDownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_later))
                }
            }
        }
    )
}
