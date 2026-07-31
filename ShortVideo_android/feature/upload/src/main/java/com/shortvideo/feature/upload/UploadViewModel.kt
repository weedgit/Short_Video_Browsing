package com.shortvideo.feature.upload

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.PublishVideoRequest
import com.shortvideo.domain.model.UploadStatus
import com.shortvideo.domain.model.VideoFileInfo
import com.shortvideo.domain.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UploadStep {
    SOURCE,
    PREVIEW,
    PUBLISH,
    PROGRESS,
    COMPLETE,
}

data class UploadUiState(
    val step: UploadStep = UploadStep.SOURCE,
    val selectedVideo: VideoFileInfo? = null,
    val description: String = "",
    val hashtagsText: String = "",
    val category: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val uploadId: String? = null,
    val uploadStatus: UploadStatus? = null,
    val bytesUploaded: Long = 0,
    val fileSizeBytes: Long = 0,
    val showMobileDataDialog: Boolean = false,
    val pendingPublish: PublishVideoRequest? = null,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val uploadRepository: UploadRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiState
                .map { it.uploadId }
                .distinctUntilChanged()
                .flatMapLatest { uploadId ->
                    if (uploadId == null) {
                        uploadRepository.observeActiveSession()
                    } else {
                        uploadRepository.observeSession(uploadId)
                    }
                }
                .collect { session ->
                    if (session == null) return@collect
                    _uiState.update { state ->
                        state.copy(
                            uploadId = session.uploadId,
                            uploadStatus = session.status,
                            bytesUploaded = session.bytesUploaded,
                            fileSizeBytes = session.fileSizeBytes,
                            step = when (session.status) {
                                UploadStatus.PUBLISHED -> UploadStep.COMPLETE
                                UploadStatus.FAILED -> UploadStep.PROGRESS
                                else -> UploadStep.PROGRESS
                            },
                            errorMessage = session.errorMessage,
                        )
                    }
                }
        }
    }

    fun onVideoSelected(uri: Uri, persistReadPermission: Boolean = false) {
        if (persistReadPermission) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            uploadRepository.inspectVideo(uri.toString())
                .onSuccess { video ->
                    _uiState.update {
                        it.copy(
                            selectedVideo = video,
                            step = UploadStep.PREVIEW,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to use this video",
                        )
                    }
                }
        }
    }

    fun onPreviewConfirmed() {
        _uiState.update { it.copy(step = UploadStep.PUBLISH, errorMessage = null) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun onHashtagsChanged(value: String) {
        _uiState.update { it.copy(hashtagsText = value, errorMessage = null) }
    }

    fun onCategoryChanged(value: String) {
        _uiState.update { it.copy(category = value, errorMessage = null) }
    }

    fun onPublishClicked() {
        val video = _uiState.value.selectedVideo
        if (video == null) {
            _uiState.update { it.copy(errorMessage = "Select a video first.") }
            return
        }
        if (_uiState.value.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Description is required.") }
            return
        }

        val publish = PublishVideoRequest(
            description = _uiState.value.description.trim(),
            hashtags = parseHashtags(_uiState.value.hashtagsText),
            category = _uiState.value.category.trim().ifBlank { null },
        )

        if (isOnMobileData()) {
            _uiState.update { it.copy(showMobileDataDialog = true, pendingPublish = publish) }
            return
        }

        startUpload(video, publish)
    }

    fun onMobileDataConfirmed() {
        val video = _uiState.value.selectedVideo ?: return
        val publish = _uiState.value.pendingPublish ?: return
        _uiState.update { it.copy(showMobileDataDialog = false, pendingPublish = null) }
        startUpload(video, publish)
    }

    fun onMobileDataDismissed() {
        _uiState.update { it.copy(showMobileDataDialog = false, pendingPublish = null) }
    }

    fun onBackToSource() {
        _uiState.update {
            UploadUiState(step = UploadStep.SOURCE)
        }
    }

    fun onRetryUpload() {
        val uploadId = _uiState.value.uploadId ?: return
        viewModelScope.launch {
            uploadRepository.scheduleUpload(uploadId)
        }
    }

    fun onCancelUpload() {
        val uploadId = _uiState.value.uploadId ?: return
        viewModelScope.launch {
            uploadRepository.cancelUpload(uploadId)
            onBackToSource()
        }
    }

    fun onDone() {
        onBackToSource()
    }

    private fun startUpload(video: VideoFileInfo, publish: PublishVideoRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            uploadRepository.createUploadSession(video, publish)
                .onSuccess { session ->
                    uploadRepository.scheduleUpload(session.uploadId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = UploadStep.PROGRESS,
                            uploadId = session.uploadId,
                            uploadStatus = session.status,
                            fileSizeBytes = session.fileSizeBytes,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to start upload",
                        )
                    }
                }
        }
    }

    private fun isOnMobileData(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun parseHashtags(raw: String): List<String> =
        raw.split(",", " ", "#")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { tag -> if (tag.startsWith("#")) tag else "#$tag" }
            .distinct()
}
