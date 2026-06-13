package com.example.topbooks.ui.community

import com.example.topbooks.data.model.Post as DataPost
import com.example.topbooks.data.model.PostType as DataPostType
import com.example.topbooks.data.model.Story as DataStory
import com.example.topbooks.data.model.User

fun DataPost.toUiPost(
    user: User? = null,
    isFriend: Boolean = false,
    isLikedByMe: Boolean = false,
    isSavedByMe: Boolean = false
): Post {
    val postType = when (this.type) {
        DataPostType.REVIEW.name -> PostType.REVIEW
        DataPostType.QUOTE.name -> PostType.QUOTE
        DataPostType.FINISHED.name -> PostType.FINISHED
        DataPostType.READING.name -> PostType.READING
        else -> PostType.REVIEW
    }

    val author = PostAuthor(
        id = this.userId,
        displayName = user?.displayName ?: this.userName,
        photoUrl = user?.photoURL ?: this.userPhotoUrl,
        isFriend = isFriend,
        isVerified = false
    )

    val book = if (this.bookId.isNotBlank()) {
        PostBook(
            id = this.bookId,
            title = this.bookTitle,
            author = this.bookAuthor,
            coverUrl = this.bookImageUrl
        )
    } else null

    val quoteSource = if (postType == PostType.QUOTE && this.bookTitle.isNotBlank()) {
        "${this.bookAuthor} · ${this.bookTitle}"
    } else null

    return Post(
        id = this.id,
        type = postType,
        author = author,
        book = book,
        createdAtMillis = this.createdAt?.time ?: System.currentTimeMillis(),
        rating = if (this.rating > 0) this.rating else null,
        body = if (postType == PostType.QUOTE) this.quote else this.text,
        quoteSource = quoteSource,
        likeCount = this.likes,
        commentCount = this.replyCount,
        isLikedByMe = isLikedByMe,
        isSavedByMe = isSavedByMe
    )
}

fun DataStory.toUiStoryItem(
    user: User? = null,
    isFriend: Boolean = false
): StoryItem {
    val author = PostAuthor(
        id = this.userId,
        displayName = user?.displayName ?: this.userName,
        photoUrl = user?.photoURL ?: this.userPhotoUrl,
        isFriend = isFriend,
        isVerified = false
    )

    val book = if (this.bookId.isNotBlank()) {
        PostBook(
            id = this.bookId,
            title = this.bookTitle,
            author = this.bookAuthor,
            coverUrl = this.bookImageUrl
        )
    } else null

    return StoryItem(
        author = author,
        currentBook = book,
        hasFinished = false
    )
}
