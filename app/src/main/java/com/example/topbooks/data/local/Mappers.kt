package com.example.topbooks.data.local

import com.example.topbooks.data.model.Book
import com.example.topbooks.data.model.Post
import com.example.topbooks.data.model.User
import java.util.Date

fun BookEntity.toDomain(): Book = Book(
    id = id,
    title = title,
    subtitle = subtitle,
    authors = authors,
    description = description,
    imageUrl = imageUrl,
    lanzamiento = lanzamiento,
    averageRating = averageRating,
    ratingsCount = ratingsCount,
    pageCount = pageCount,
    isMature = isMature,
    categories = categories,
    seriesName = seriesName,
    seriesIndex = seriesIndex,
    provider = provider,
    seriesEditorUid = seriesEditorUid,
    seriesEditorName = seriesEditorName,
    seriesEditorAvatar = seriesEditorAvatar,
    seriesEditDate = seriesEditDate,
    seriesUpvotes = seriesUpvotes,
    seriesDownvotes = seriesDownvotes,
    seriesVoters = seriesVoters
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    authors = authors,
    description = description,
    imageUrl = imageUrl,
    lanzamiento = lanzamiento,
    averageRating = averageRating,
    ratingsCount = ratingsCount,
    pageCount = pageCount,
    isMature = isMature,
    categories = categories,
    seriesName = seriesName,
    seriesIndex = seriesIndex,
    provider = provider,
    seriesEditorUid = seriesEditorUid,
    seriesEditorName = seriesEditorName,
    seriesEditorAvatar = seriesEditorAvatar,
    seriesEditDate = seriesEditDate,
    seriesUpvotes = seriesUpvotes,
    seriesDownvotes = seriesDownvotes,
    seriesVoters = seriesVoters
)

fun PostEntity.toDomain(): Post = Post(
    id = id,
    userId = userId,
    type = type,
    bookId = bookId,
    text = text,
    rating = rating,
    quote = quote,
    chapter = chapter,
    likes = likes,
    likedBy = likedBy,
    savedBy = savedBy,
    reactions = reactions,
    replyCount = replyCount,
    createdAt = if (createdAtMillis > 0) Date(createdAtMillis) else null,
    userName = userName,
    userPhotoUrl = userPhotoUrl,
    bookTitle = bookTitle,
    bookAuthor = bookAuthor,
    bookImageUrl = bookImageUrl
)

fun Post.toEntity(): PostEntity = PostEntity(
    id = id,
    userId = userId,
    type = type,
    bookId = bookId,
    text = text,
    rating = rating,
    quote = quote,
    chapter = chapter,
    likes = likes,
    likedBy = likedBy,
    savedBy = savedBy,
    reactions = reactions,
    replyCount = replyCount,
    createdAtMillis = createdAt?.time ?: 0L,
    userName = userName,
    userPhotoUrl = userPhotoUrl,
    bookTitle = bookTitle,
    bookAuthor = bookAuthor,
    bookImageUrl = bookImageUrl
)

fun UserEntity.toDomain(): User = User(
    uid = uid,
    displayName = displayName,
    displayNameLowercase = displayNameLowercase,
    email = email,
    photoURL = photoURL,
    role = role,
    bio = bio,
    isTutorialCompleted = isTutorialCompleted,
    favoriteGenres = favoriteGenres,
    favoriteBooks = favoriteBooks,
    preferences = preferences,
    lastLogin = Date(lastLoginMillis),
    reviewsCount = reviewsCount,
    bookmarksCount = bookmarksCount,
    commentsCount = commentsCount,
    friendsCount = friendsCount,
    booksCompleted = booksCompleted,
    fcmToken = fcmToken,
    createdAt = createdAtMillis?.let { Date(it) }
)

fun User.toEntity(): UserEntity = UserEntity(
    uid = uid,
    displayName = displayName,
    displayNameLowercase = displayNameLowercase,
    email = email,
    photoURL = photoURL,
    role = role,
    bio = bio,
    isTutorialCompleted = isTutorialCompleted,
    favoriteGenres = favoriteGenres,
    favoriteBooks = favoriteBooks,
    preferences = preferences,
    lastLoginMillis = lastLogin.time,
    reviewsCount = reviewsCount,
    bookmarksCount = bookmarksCount,
    commentsCount = commentsCount,
    friendsCount = friendsCount,
    booksCompleted = booksCompleted,
    fcmToken = fcmToken,
    createdAtMillis = createdAt?.time
)
