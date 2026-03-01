package com.example.topbooks.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.topbooks.R
import com.example.topbooks.data.model.Comment
import com.example.topbooks.data.model.Journal
import com.example.topbooks.data.model.Review
import com.example.topbooks.ui.components.TopBar
import com.example.topbooks.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UserListScreen(
    type: String,
    userId: String,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onJournalClick: (String) -> Unit = {},
    onCommentClick: (String, String) -> Unit = { _, _ -> },
    viewModel: UserListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val auth = FirebaseAuth.getInstance()
    val isMe = auth.currentUser?.uid == userId

    LaunchedEffect(type, userId) {
        viewModel.loadList(type, userId)
    }

    // 🔥 ACTUALIZADO: Añadido el título para "pending"
    val title = when(type) {
        "friends" -> stringResource(R.string.userlist_title_friends)
        "reviews" -> stringResource(R.string.userlist_title_reviews)
        "read" -> stringResource(R.string.userlist_title_read)
        "pending" -> stringResource(R.string.progress_section_pending)
        "journals" -> stringResource(R.string.userlist_title_journals)
        "bookmarks" -> stringResource(R.string.userlist_title_bookmarks)
        "comments" -> stringResource(R.string.userlist_title_comments)
        "favorites" -> stringResource(R.string.userlist_title_favorites)
        else -> stringResource(R.string.userlist_title_default)
    }

    Scaffold(
        containerColor = ColorBackGroundGeneral,
        topBar = { TopBar(onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = title,
                fontFamily = CenturyGotic,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTituloTopBooks
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = ColorArcMediumBrown) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // 🔥 ACTUALIZADO: Añadido "pending" dibujando BookItem
                    when(type) {
                        "friends" -> items(state.friends) { FriendItem(it, onUserClick) }
                        "read" -> items(state.readBooks) { BookItem(it, onBookClick) }
                        "pending" -> items(state.pendingBooks) { BookItem(it, onBookClick) }
                        "favorites" -> items(state.favorites) { BookItem(it, onBookClick) }
                        "bookmarks" -> items(state.bookmarks) { BookmarkListItem(it, onBookClick, viewModel, isMe) }
                        "journals" -> items(state.journals) {
                            JournalListItem(
                                journal = it,
                                onClick = onJournalClick,
                                onDelete = { viewModel.deleteJournal(it.bookId) },
                                isMe = isMe
                            )
                        }
                        "reviews" -> items(state.reviews) {
                            ReviewListItem(
                                review = it,
                                onBookClick = onBookClick,
                                onDelete = { viewModel.deleteReview(it.id) },
                                isMe = isMe
                            )
                        }
                        "comments" -> items(state.comments) {
                            CommentListItem(
                                comment = it,
                                onCommentClick = onCommentClick,
                                onDelete = { viewModel.deleteComment(it.commentId) },
                                isMe = isMe
                            )
                        }
                    }

                    // 🔥 ACTUALIZADO: Añadido "pending" a la comprobación de vacío
                    val isEmpty = when(type) {
                        "friends" -> state.friends.isEmpty()
                        "read" -> state.readBooks.isEmpty()
                        "pending" -> state.pendingBooks.isEmpty()
                        "favorites" -> state.favorites.isEmpty()
                        "bookmarks" -> state.bookmarks.isEmpty()
                        "reviews" -> state.reviews.isEmpty()
                        "comments" -> state.comments.isEmpty()
                        "journals" -> state.journals.isEmpty()
                        else -> true
                    }

                    if (isEmpty) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                                Text(stringResource(R.string.userlist_empty_message), color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalListItem(
    journal: Journal,
    onClick: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    isMe: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.journal_delete_title), fontFamily = CenturyGotic, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
            text = { Text(stringResource(R.string.userlist_delete_journal_body), color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.userlist_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.userlist_action_cancel), color = Color.Gray) }
            },
            containerColor = Color.White, shape = RoundedCornerShape(24.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(journal.bookId) },
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = journal.bookImageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journal.bookTitle.ifEmpty { stringResource(R.string.journal_default_book_title) },
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(text = stringResource(R.string.journal_format_prefix, journal.format), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row {
                    repeat(5) { i ->
                        val color = if(i < journal.mainRating) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f)
                        Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (isMe && onDelete != null) {
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.userlist_action_delete), tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun BookmarkListItem(bookmark: BookmarkUI, onBookClick: (String) -> Unit, viewModel: UserListViewModel, isMe: Boolean) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditBookmarkDialog(
            bookmark = bookmark,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateBookmark(updated)
                showEditDialog = false
            },
            onDelete = {
                viewModel.removeBookmark(bookmark.bookId)
                showEditDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (isMe) showEditDialog = true else onBookClick(bookmark.bookId)
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Bookmark, contentDescription = null, tint = ColorArcMediumBrown, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.bookTitle,
                    fontWeight = FontWeight.Bold,
                    color = ColorArcDarkBrown,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onBookClick(bookmark.bookId) }
                )
                Spacer(Modifier.height(4.dp))
                Text("«${bookmark.quote}»", fontStyle = FontStyle.Italic, color = Color.DarkGray, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val chapText = if (bookmark.chapter.isNotBlank()) stringResource(R.string.userlist_bookmark_cap, bookmark.chapter) else ""
                    val pageText = if (bookmark.page.isNotBlank()) stringResource(R.string.userlist_bookmark_pag, bookmark.page) else ""
                    val combined = listOf(chapText, pageText).filter { it.isNotBlank() }.joinToString(" • ")

                    Text(combined, color = ColorArcMediumBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (!bookmark.isPublic && isMe) {
                        Icon(Icons.Default.Lock, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditBookmarkDialog(bookmark: BookmarkUI, onDismiss: () -> Unit, onSave: (BookmarkUI) -> Unit, onDelete: () -> Unit) {
    var p by remember { mutableStateOf(bookmark.page) }
    var c by remember { mutableStateOf(bookmark.chapter) }
    var q by remember { mutableStateOf(bookmark.quote) }
    var pub by remember { mutableStateOf(bookmark.isPublic) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.userlist_edit_bookmark_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = p, onValueChange = { if(it.all { char -> char.isDigit() }) p = it }, label = { Text(stringResource(R.string.userlist_bookmark_page)) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = c, onValueChange = { c = it }, label = { Text(stringResource(R.string.userlist_bookmark_chapter)) }, modifier = Modifier.weight(2f), singleLine = true, shape = RoundedCornerShape(12.dp))
                }
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text(stringResource(R.string.userlist_bookmark_quote)) }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp))
                DialogPrivacyToggleButton(isPublic = pub, onToggle = { pub = it })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(bookmark.copy(page = p, chapter = c, quote = q, isPublic = pub)) }, enabled = p.isNotEmpty() && q.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = ColorArcMediumBrown), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.userlist_action_save)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.userlist_action_delete), color = Color.Red.copy(0.7f)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.userlist_action_cancel), color = Color.Gray) }
            }
        },
        containerColor = Color.White, shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DialogPrivacyToggleButton(isPublic: Boolean, onToggle: (Boolean) -> Unit) {
    val pubCol by animateColorAsState(if (isPublic) ColorArcMediumBrown else Color.Transparent)
    val privCol by animateColorAsState(if (!isPublic) ColorArcMediumBrown else Color.Transparent)
    val pubText by animateColorAsState(if (isPublic) Color.White else Color.Gray)
    val privText by animateColorAsState(if (!isPublic) Color.White else Color.Gray)
    Surface(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), color = ColorHeaderBeige.copy(0.5f), border = androidx.compose.foundation.BorderStroke(1.dp, ColorArcMediumBrown.copy(0.3f))) {
        Row {
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(pubCol).clickable { onToggle(true) }, Alignment.Center) {
                Row { Icon(Icons.Default.Call, null, tint = pubText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.userlist_privacy_public), color = pubText, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(privCol).clickable { onToggle(false) }, Alignment.Center) {
                Row { Icon(Icons.Default.Lock, null, tint = privText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.userlist_privacy_private), color = privText, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun FriendItem(user: SimpleUser, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { if(user.uid.isNotEmpty()) onClick(user.uid) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.photo.ifEmpty { "https://via.placeholder.com/150" }, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Text(user.name, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 16.sp)
        }
    }
}

@Composable
fun BookItem(book: SimpleBook, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { if(book.id.isNotEmpty()) onClick(book.id) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = book.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(45.dp, 65.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Text(book.title, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown, fontSize = 15.sp)
        }
    }
}

@Composable
fun ReviewListItem(
    review: Review,
    onBookClick: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    isMe: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.userlist_delete_review_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
            text = { Text(stringResource(R.string.userlist_delete_review_body)) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.userlist_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.userlist_action_cancel), color = Color.Gray) }
            },
            containerColor = Color.White, shape = RoundedCornerShape(24.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = review.bookImageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)).clickable { if(review.bookId.isNotEmpty()) onBookClick(review.bookId) },
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.bookTitle.ifEmpty { stringResource(R.string.userlist_book_prefix, review.bookId) },
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.clickable { if(review.bookId.isNotEmpty()) onBookClick(review.bookId) }
                )
                Spacer(Modifier.height(4.dp))
                Text(text = review.text, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row {
                    repeat(5) { i ->
                        val color = if(i < review.rating) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f)
                        Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (isMe && onDelete != null) {
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.userlist_action_delete), tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun CommentListItem(
    comment: Comment,
    onCommentClick: (String, String) -> Unit,
    onDelete: () -> Unit,
    isMe: Boolean
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.userlist_delete_comment_title), fontFamily = GuardianCity, fontWeight = FontWeight.Bold, color = ColorArcDarkBrown) },
            text = { Text(stringResource(R.string.userlist_delete_comment_body)) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.userlist_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.userlist_action_cancel), color = Color.Gray) }
            },
            containerColor = Color.White, shape = RoundedCornerShape(24.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCommentClick(comment.bookId, comment.commentId) },
        colors = CardDefaults.cardColors(containerColor = ColorArcMediumBrown),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = comment.bookImageUrl,
                contentDescription = null,
                modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.bookTitle.ifEmpty { stringResource(R.string.userlist_book_prefix, comment.bookId) },
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(text = "«${comment.text}»", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontStyle = FontStyle.Italic, maxLines = 3, overflow = TextOverflow.Ellipsis)

                if (comment.replies.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = stringResource(R.string.userlist_replies_count, comment.replies.size), color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isMe) {
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.userlist_action_delete), tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}