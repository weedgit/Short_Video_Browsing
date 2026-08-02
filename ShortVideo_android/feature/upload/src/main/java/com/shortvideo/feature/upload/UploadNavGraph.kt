package com.shortvideo.feature.upload

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.shortvideo.core.DestinationRoute
import com.shortvideo.domain.model.UploadStatus
import com.shortvideo.domain.model.VideoCategories
import com.shortvideo.feature.upload.camera.CameraCaptureScreen

@Composable
fun UploadScreen(
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showMobileDataDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onMobileDataDismissed,
            title = { Text("Upload on mobile data?") },
            text = { Text("This video may use a large amount of mobile data. Continue anyway?") },
            confirmButton = {
                TextButton(onClick = viewModel::onMobileDataConfirmed) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onMobileDataDismissed) {
                    Text("Cancel")
                }
            },
        )
    }

    when (uiState.step) {
        UploadStep.SOURCE -> UploadSourceStep(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onVideoSelected = { uri, persist -> viewModel.onVideoSelected(uri, persist) },
        )
        UploadStep.PREVIEW -> UploadPreviewStep(
            videoUri = uiState.selectedVideo?.uri.orEmpty(),
            onConfirm = viewModel::onPreviewConfirmed,
            onBack = viewModel::onBackToSource,
        )
        UploadStep.PUBLISH -> UploadPublishStep(
            description = uiState.description,
            hashtagsText = uiState.hashtagsText,
            category = uiState.category,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onHashtagsChanged = viewModel::onHashtagsChanged,
            onCategoryChanged = viewModel::onCategoryChanged,
            onPublish = viewModel::onPublishClicked,
            onBack = { viewModel.onPreviewConfirmed() },
        )
        UploadStep.PROGRESS, UploadStep.COMPLETE -> UploadProgressStep(
            status = uiState.uploadStatus,
            bytesUploaded = uiState.bytesUploaded,
            fileSizeBytes = uiState.fileSizeBytes,
            errorMessage = uiState.errorMessage,
            onRetry = viewModel::onRetryUpload,
            onCancel = viewModel::onCancelUpload,
            onDone = viewModel::onDone,
        )
    }
}

@Composable
private fun UploadSourceStep(
    isLoading: Boolean,
    errorMessage: String?,
    onVideoSelected: (Uri, Boolean) -> Unit,
) {
    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraCaptureScreen(
            onVideoCaptured = { uri ->
                showCamera = false
                onVideoSelected(uri, false)
            },
            onCancel = { showCamera = false },
        )
        return
    }

    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* handled per action */ }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onVideoSelected(uri, false)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onVideoSelected(uri, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Create", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Record a new video or choose one from your library.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                permissionLauncher.launch(permissions.toTypedArray())
                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Choose from library")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                permissionLauncher.launch(permissions.toTypedArray())
                documentLauncher.launch(
                    arrayOf("video/*"),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Browse files")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                permissionLauncher.launch(permissions.toTypedArray())
                showCamera = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Record video")
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun UploadPreviewStep(
    videoUri: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val player = androidx.compose.runtime.remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }

    androidx.compose.runtime.DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Choose another video")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadPublishStep(
    description: String,
    hashtagsText: String,
    category: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDescriptionChanged: (String) -> Unit,
    onHashtagsChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Publish details", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = hashtagsText,
            onValueChange = onHashtagsChanged,
            label = { Text("Hashtags") },
            placeholder = { Text("#fun #shortvideo") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded },
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                placeholder = { Text("Select a category") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                VideoCategories.ALL.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onCategoryChanged(option)
                            categoryExpanded = false
                        },
                    )
                }
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = onPublish,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Starting..." else "Publish")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to preview")
        }
    }
}

@Composable
private fun UploadProgressStep(
    status: UploadStatus?,
    bytesUploaded: Long,
    fileSizeBytes: Long,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val progress = if (fileSizeBytes > 0) {
        bytesUploaded.toFloat() / fileSizeBytes.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(statusLabel(status), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("${formatMegabytes(bytesUploaded)} / ${formatMegabytes(fileSizeBytes)}")

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        when (status) {
            UploadStatus.FAILED -> {
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
            UploadStatus.PUBLISHED -> {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
            else -> {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel upload")
                }
            }
        }
    }
}

private fun statusLabel(status: UploadStatus?): String =
    when (status) {
        UploadStatus.DRAFT -> "Preparing upload"
        UploadStatus.UPLOADING -> "Uploading"
        UploadStatus.UPLOADED -> "Upload complete"
        UploadStatus.PROCESSING -> "Processing"
        UploadStatus.PUBLISHED -> "Published"
        UploadStatus.FAILED -> "Upload failed"
        null -> "Upload"
    }

private fun formatMegabytes(bytes: Long): String =
    String.format("%.1f MB", bytes / (1024f * 1024f))

fun NavGraphBuilder.uploadNavGraph() {
    composable(DestinationRoute.UPLOAD_ROUTE) {
        UploadScreen()
    }
}
