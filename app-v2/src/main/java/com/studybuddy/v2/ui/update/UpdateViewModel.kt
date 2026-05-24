package com.studybuddy.v2.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studybuddy.v2.data.model.AppVersion
import com.studybuddy.v2.data.repo.AppVersionRepo
import com.studybuddy.v2.data.update.AppUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val checking: Boolean = false,
    val latest: AppVersion? = null,           // 仅当有新版才填
    val downloadProgress: Float = 0f,         // 0-1
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloading: Boolean = false,
    val readyApk: java.io.File? = null,       // 下载完成
    val failed: String? = null,
    val noUpdate: Boolean = false,            // 已是最新（用于"立即检查"反馈）
    val currentVersionName: String = ""
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val versionRepo: AppVersionRepo,
    private val updater: AppUpdater
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(currentVersionName = versionRepo.currentVersionName) }
    }

    fun checkUpdate(manual: Boolean = false) {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update { it.copy(checking = true, noUpdate = false, failed = null) }
            val latest = versionRepo.checkUpdate()
            _state.update {
                it.copy(
                    checking = false,
                    latest = latest,
                    noUpdate = manual && latest == null
                )
            }
        }
    }

    fun startDownload() {
        val v = _state.value.latest ?: return
        if (_state.value.downloading) return
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, failed = null, readyApk = null) }
            updater.download(v.downloadUrl, v.versionCode, v.versionName).collect { s ->
                when (s) {
                    is AppUpdater.State.Downloading -> _state.update {
                        it.copy(
                            downloadedBytes = s.downloaded,
                            totalBytes = s.total,
                            downloadProgress = s.progress
                        )
                    }
                    is AppUpdater.State.Ready -> _state.update {
                        it.copy(downloading = false, readyApk = s.apkFile)
                    }
                    is AppUpdater.State.Failed -> _state.update {
                        it.copy(downloading = false, failed = s.reason)
                    }
                    else -> {}
                }
            }
        }
    }

    fun install() {
        val f = _state.value.readyApk ?: return
        try { updater.install(f) } catch (e: Exception) {
            _state.update { it.copy(failed = "安装失败：${e.message}") }
        }
    }

    fun dismiss() {
        _state.update {
            it.copy(latest = null, readyApk = null, failed = null, noUpdate = false,
                downloading = false, downloadProgress = 0f, downloadedBytes = 0, totalBytes = 0)
        }
    }
}
