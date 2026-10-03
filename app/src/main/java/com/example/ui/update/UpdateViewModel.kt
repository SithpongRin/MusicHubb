package com.example.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.update.UpdateDownloadState
import com.example.data.update.UpdateRepository
import com.example.domain.models.AppVersionManifest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    val downloadState: StateFlow<UpdateDownloadState> = updateRepository.downloadState

    fun downloadAndVerify(manifest: AppVersionManifest) {
        viewModelScope.launch {
            updateRepository.downloadAndVerifyApk(manifest.apkUrl, manifest.sha256)
        }
    }

    fun cancelDownload() {
        updateRepository.cancelDownload()
    }

    fun canInstall(): Boolean {
        return updateRepository.canInstallPackages()
    }

    fun getPermissionIntent() = updateRepository.getInstallPermissionIntent()

    fun install(apkFile: File) {
        updateRepository.installApk(apkFile)
    }
}
