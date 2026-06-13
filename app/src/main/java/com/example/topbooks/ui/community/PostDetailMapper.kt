package com.example.topbooks.ui.community

import com.example.topbooks.data.model.Post as DataPost
import com.example.topbooks.data.model.PostReply as DataPostReply
import com.example.topbooks.data.model.User

fun DataPostReply.toUiPostReply(
    user: User? = null,
    isFromOriginalAuthor: Boolean = false,
    isLikedByMe: Boolean = false
): PostReply {
    val author = PostAuthor(
        id = this.userId,
        displayName = user?.displayName ?: this.userName,
        photoUrl = user?.photoURL ?: this.userPhotoUrl,
        isFriend = false,
        isVerified = false
    )

    return PostReply(
        id = this.id,
        author = author,
        body = this.text,
        createdAtMillis = this.createdAt?.time ?: System.currentTimeMillis(),
        likeCount = this.likes,
        isLikedByMe = isLikedByMe,
        isFromOriginalAuthor = isFromOriginalAuthor
    )
}

fun buildReactionsFromPost(post: DataPost, myUid: String): List<Reaction> {
    val reactionsMap = post.reactions
    val emojisToShow = (TOP_FIXED_REACTIONS + reactionsMap.keys).distinct()

    return emojisToShow.mapNotNull { emoji ->
        val usersWhoReacted = reactionsMap[emoji] ?: emptyList()
        val count = usersWhoReacted.size
        val reactedByMe = myUid in usersWhoReacted

        if (emoji in TOP_FIXED_REACTIONS || count > 0) {
            Reaction(
                emoji = emoji,
                count = count,
                reactedByMe = reactedByMe
            )
        } else null
    }.sortedWith(
        compareByDescending<Reaction> { it.emoji in TOP_FIXED_REACTIONS }
            .thenByDescending { it.count }
    )
}
