package com.shortvideo.composable.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shortvideo.domain.model.VideoComment
import com.shortvideo.theme.PrimaryColor
import com.shortvideo.theme.SurfaceElevated
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    commentCount: Long,
    comments: List<VideoComment>,
    onDismiss: () -> Unit,
    onSubmit: (text: String, parentId: String?) -> Unit,
    onReport: (comment: VideoComment, reason: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<VideoComment?>(null) }
    var expandedReplyIds by remember { mutableStateOf(setOf<String>()) }
    var reportTarget by remember { mutableStateOf<VideoComment?>(null) }

    fun hideKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { it }
            .collect { hideKeyboard() }
    }

    ModalBottomSheet(
        onDismissRequest = {
            hideKeyboard()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "$commentCount comments",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { hideKeyboard() },
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            LazyColumn(
                state = listState,
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
                    val expanded = expandedReplyIds.contains(comment.id)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CommentRow(
                            comment = comment,
                            isReply = false,
                            onReply = {
                                hideKeyboard()
                                replyTarget = it
                            },
                            onReport = {
                                hideKeyboard()
                                reportTarget = it
                            },
                        )
                        if (replies.isNotEmpty()) {
                            if (!expanded) {
                                Text(
                                    text = "View ${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .padding(start = 52.dp, top = 6.dp)
                                        .clickable {
                                            hideKeyboard()
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
                                                hideKeyboard()
                                                replyTarget = comment.copy(
                                                    authorName = reply.authorName,
                                                )
                                            },
                                            onReport = {
                                                hideKeyboard()
                                                reportTarget = it
                                            },
                                        )
                                    }
                                    Text(
                                        text = "Hide replies",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clickable {
                                                hideKeyboard()
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
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Replying to ${target.authorName}",
                        color = Color.White.copy(alpha = 0.85f),
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

            val canPost = draft.trim().isNotEmpty()
            fun postComment() {
                val text = draft.trim()
                if (text.isEmpty()) return
                val parentId = replyTarget?.let { target ->
                    target.parentId ?: target.id
                }
                onSubmit(text, parentId)
                if (parentId != null) {
                    expandedReplyIds = expandedReplyIds + parentId
                }
                draft = ""
                replyTarget = null
                hideKeyboard()
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
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
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                    cursorColor = PrimaryColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1C1C1C),
                    unfocusedContainerColor = Color(0xFF1C1C1C),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { postComment() },
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { postComment() },
                        enabled = canPost,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (canPost) PrimaryColor else Color.White.copy(alpha = 0.15f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Post comment",
                            tint = if (canPost) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    reportTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report comment?") },
            text = {
                Text("Report ${target.authorName}'s comment as inappropriate.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReport(target, "Inappropriate comment")
                        reportTarget = null
                    },
                ) {
                    Text("Report", color = PrimaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CommentRow(
    comment: VideoComment,
    isReply: Boolean,
    onReply: (VideoComment) -> Unit,
    onReport: (VideoComment) -> Unit,
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
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
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
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onReply(comment) },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Report",
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onReport(comment) },
                )
            }
        }
    }
}
