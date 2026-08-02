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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SurfaceElevated

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
        containerColor = SurfaceElevated,
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
                    color = Color.White,
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
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
                            color = Color.White.copy(alpha = 0.6f),
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
                                color = Color.White.copy(alpha = 0.7f),
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
                                        color = Color.White.copy(alpha = 0.7f),
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
                        color = Color.White.copy(alpha = 0.7f),
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
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
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
                )
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
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (draft.isBlank()) Color.White.copy(alpha = 0.35f) else PrimaryColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
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
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val canSend = title.isNotBlank() && content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "Report",
                color = Color.White,
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
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSend) onSend(title.trim(), content.trim())
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
    )
}

@Composable
private fun CommentRow(
    comment: VideoComment,
    isReply: Boolean,
    onReply: (VideoComment) -> Unit,
) {
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
                .background(Color(0xFF2A2A2A)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.authorName,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            if (!comment.replyToAuthorName.isNullOrBlank() && isReply) {
                Text(
                    text = "Replying to ${comment.replyToAuthorName}",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                text = comment.text,
                color = Color.White.copy(alpha = 0.95f),
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
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = "Reply",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onReply(comment) },
                )
            }
        }
    }
}
