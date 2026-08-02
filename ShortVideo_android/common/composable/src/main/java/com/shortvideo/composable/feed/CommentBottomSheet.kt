package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    commentCount: Long,
    comments: List<VideoComment>,
    onDismiss: () -> Unit,
    onSubmit: (text: String, parentId: String?) -> Unit,
    onReport: (title: String, content: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<VideoComment?>(null) }
    var expandedReplyIds by remember { mutableStateOf(setOf<String>()) }
    var showReportDialog by remember { mutableStateOf(false) }

    if (showReportDialog) {
        ReportVideoDialog(
            onDismiss = { showReportDialog = false },
            onSend = { title, content ->
                onReport(title, content)
                showReportDialog = false
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 620.dp)
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text(
                    text = "$commentCount comments",
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    text = "Report",
                    color = PrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { showReportDialog = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            HorizontalDivider(color = colors.outline.copy(alpha = 0.6f))
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "Be the first to comment",
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                items(comments, key = { it.id }) { comment ->
                    val replies = comment.replies
                    val expanded = expandedReplyIds.contains(comment.id) || replies.size <= 2
                    CommentRow(
                        comment = comment,
                        isReply = false,
                        onReply = { replyTarget = it },
                    )
                    if (replies.isNotEmpty()) {
                        if (!expanded) {
                            Text(
                                text = "View ${replies.size} replies",
                                color = colors.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(start = 52.dp, top = 6.dp)
                                    .clickable {
                                        expandedReplyIds = expandedReplyIds + comment.id
                                    },
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(start = 44.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                replies.forEach { reply ->
                                    CommentRow(
                                        comment = reply,
                                        isReply = true,
                                        onReply = {
                                            replyTarget = comment.copy(
                                                authorName = reply.authorName,
                                            )
                                        },
                                    )
                                }
                                if (replies.size > 2) {
                                    Text(
                                        text = "Hide replies",
                                        color = colors.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clickable {
                                                expandedReplyIds = expandedReplyIds - comment.id
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            replyTarget?.let { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Replying to ${target.authorName}",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { replyTarget = null },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                val canSend = draft.isNotBlank()
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (replyTarget != null) {
                                "Reply to ${replyTarget!!.authorName}"
                            } else {
                                "Add comment..."
                            },
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(percent = 50),
                    colors = themedOutlinedFieldColors(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    val parentId = replyTarget?.let { target ->
                                        target.parentId ?: target.id
                                    }
                                    onSubmit(text, parentId)
                                    draft = ""
                                    replyTarget = null
                                }
                            },
                            enabled = canSend,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send comment",
                                tint = if (canSend) {
                                    PrimaryColor
                                } else {
                                    colors.onSurface.copy(alpha = 0.35f)
                                },
                            )
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReportVideoDialog(
    onDismiss: () -> Unit,
    onSend: (title: String, content: String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val canSend = title.isNotBlank() && content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurface,
        title = {
            Text(
                text = "Report",
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = themedOutlinedFieldColors(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    minLines = 3,
                    maxLines = 6,
                    colors = themedOutlinedFieldColors(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSend) onSend(title.trim(), content.trim())
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryColor,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.surfaceVariant,
                    disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
                ),
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun themedOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    focusedBorderColor = PrimaryColor,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = PrimaryColor,
    focusedLabelColor = PrimaryColor,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun CommentRow(
    comment: VideoComment,
    isReply: Boolean,
    onReply: (VideoComment) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = comment.authorAvatarUrl,
            contentDescription = comment.authorName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(if (isReply) 28.dp else 36.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.authorName,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            if (!comment.replyToAuthorName.isNullOrBlank() && isReply) {
                Text(
                    text = "Replying to ${comment.replyToAuthorName}",
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                text = comment.text,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 14.sp,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (comment.createdAtLabel.isNotBlank()) {
                    Text(
                        text = comment.createdAtLabel,
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Reply",
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onReply(comment) },
                )
            }
        }
    }
}
