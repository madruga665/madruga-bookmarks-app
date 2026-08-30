package com.madruga665.bookmarks.data.remote.sync.dto

import com.madruga665.bookmarks.data.local.BookmarkEntity
import com.madruga665.bookmarks.data.local.CollectionEntity
import org.json.JSONArray
import org.json.JSONObject


/**
 * Collection DTO matching bookmarks-sync-v1 schema.
 */
data class CollectionSyncDto(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val colorAccent: String,
    val iconKey: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("parentId", parentId ?: JSONObject.NULL)
        put("colorAccent", colorAccent)
        put("iconKey", iconKey)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("isDeleted", isDeleted)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): CollectionSyncDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): CollectionSyncDto =
            CollectionSyncDto(
                id = json.getString("id"),
                name = json.getString("name"),
                parentId = if (json.isNull("parentId") || !json.has("parentId")) null else json.optString("parentId").ifBlank { null },
                colorAccent = json.optString("colorAccent", "YELLOW"),
                iconKey = json.optString("iconKey", "folder"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                isDeleted = json.optBoolean("isDeleted", false)
            )
    }
}

/**
 * Bookmark DTO matching bookmarks-sync-v1 schema.
 */
data class BookmarkSyncDto(
    val id: String,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val faviconUrl: String? = null,
    val thumbnailUrl: String? = null,
    val sourcePlatform: String? = null,
    val collectionId: String = "col_unsorted",
    val notes: String? = null,
    val tags: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("title", title ?: JSONObject.NULL)
        put("description", description ?: JSONObject.NULL)
        put("faviconUrl", faviconUrl ?: JSONObject.NULL)
        put("thumbnailUrl", thumbnailUrl ?: JSONObject.NULL)
        put("sourcePlatform", sourcePlatform ?: JSONObject.NULL)
        put("collectionId", collectionId)
        put("notes", notes ?: JSONObject.NULL)
        put("tags", tags)
        put("isPinned", isPinned)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("isDeleted", isDeleted)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): BookmarkSyncDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): BookmarkSyncDto =
            BookmarkSyncDto(
                id = json.getString("id"),
                url = json.getString("url"),
                title = if (json.isNull("title") || !json.has("title")) null else json.optString("title"),
                description = if (json.isNull("description") || !json.has("description")) null else json.optString("description"),
                faviconUrl = if (json.isNull("faviconUrl") || !json.has("faviconUrl")) null else json.optString("faviconUrl"),
                thumbnailUrl = if (json.isNull("thumbnailUrl") || !json.has("thumbnailUrl")) null else json.optString("thumbnailUrl"),
                sourcePlatform = if (json.isNull("sourcePlatform") || !json.has("sourcePlatform")) null else json.optString("sourcePlatform"),
                collectionId = json.optString("collectionId", "col_unsorted").ifBlank { "col_unsorted" },
                notes = if (json.isNull("notes") || !json.has("notes")) null else json.optString("notes"),
                tags = json.optString("tags", ""),
                isPinned = json.optBoolean("isPinned", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                isDeleted = json.optBoolean("isDeleted", false)
            )
    }
}

/**
 * Request payload sent to POST /api/v1/sync/exchange.
 */
data class SyncExchangeRequestDto(
    val deviceId: String,
    val lastSyncTimestamp: Long,
    val collections: List<CollectionSyncDto> = emptyList(),
    val bookmarks: List<BookmarkSyncDto> = emptyList()
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("deviceId", deviceId)
        put("lastSyncTimestamp", lastSyncTimestamp)

        val collectionsArray = JSONArray()
        collections.forEach { collectionsArray.put(it.toJSONObject()) }
        put("collections", collectionsArray)

        val bookmarksArray = JSONArray()
        bookmarks.forEach { bookmarksArray.put(it.toJSONObject()) }
        put("bookmarks", bookmarksArray)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): SyncExchangeRequestDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): SyncExchangeRequestDto {
            val collections = mutableListOf<CollectionSyncDto>()
            val collectionsArray = json.optJSONArray("collections")
            if (collectionsArray != null) {
                for (i in 0 until collectionsArray.length()) {
                    val item = collectionsArray.optJSONObject(i)
                    if (item != null) {
                        collections.add(CollectionSyncDto.fromJson(item))
                    }
                }
            }

            val bookmarks = mutableListOf<BookmarkSyncDto>()
            val bookmarksArray = json.optJSONArray("bookmarks")
            if (bookmarksArray != null) {
                for (i in 0 until bookmarksArray.length()) {
                    val item = bookmarksArray.optJSONObject(i)
                    if (item != null) {
                        bookmarks.add(BookmarkSyncDto.fromJson(item))
                    }
                }
            }

            return SyncExchangeRequestDto(
                deviceId = json.getString("deviceId"),
                lastSyncTimestamp = json.optLong("lastSyncTimestamp", 0L),
                collections = collections,
                bookmarks = bookmarks
            )
        }
    }
}

/**
 * Response received from POST /api/v1/sync/exchange.
 */
data class SyncExchangeResponseDto(
    val status: String,
    val serverTimestamp: Long,
    val collections: List<CollectionSyncDto> = emptyList(),
    val bookmarks: List<BookmarkSyncDto> = emptyList()
) {
    fun toJSONObject(): JSONObject = JSONObject().apply {
        put("status", status)
        put("serverTimestamp", serverTimestamp)

        val collectionsArray = JSONArray()
        collections.forEach { collectionsArray.put(it.toJSONObject()) }
        put("collections", collectionsArray)

        val bookmarksArray = JSONArray()
        bookmarks.forEach { bookmarksArray.put(it.toJSONObject()) }
        put("bookmarks", bookmarksArray)
    }

    fun toJson(): String = toJSONObject().toString()

    companion object {
        fun fromJson(jsonStr: String): SyncExchangeResponseDto =
            fromJson(JSONObject(jsonStr))

        fun fromJson(json: JSONObject): SyncExchangeResponseDto {
            val collections = mutableListOf<CollectionSyncDto>()
            val collectionsArray = json.optJSONArray("collections")
            if (collectionsArray != null) {
                for (i in 0 until collectionsArray.length()) {
                    val item = collectionsArray.optJSONObject(i)
                    if (item != null) {
                        collections.add(CollectionSyncDto.fromJson(item))
                    }
                }
            }

            val bookmarks = mutableListOf<BookmarkSyncDto>()
            val bookmarksArray = json.optJSONArray("bookmarks")
            if (bookmarksArray != null) {
                for (i in 0 until bookmarksArray.length()) {
                    val item = bookmarksArray.optJSONObject(i)
                    if (item != null) {
                        bookmarks.add(BookmarkSyncDto.fromJson(item))
                    }
                }
            }

            return SyncExchangeResponseDto(
                status = json.getString("status"),
                serverTimestamp = json.optLong("serverTimestamp", 0L),
                collections = collections,
                bookmarks = bookmarks
            )
        }
    }
}

fun CollectionSyncDto.toEntity(linkCount: Int = 0, subcollectionCount: Int = 0): CollectionEntity =
    CollectionEntity(
        id = id,
        name = name,
        parentId = parentId,
        linkCount = linkCount,
        subcollectionCount = subcollectionCount,
        iconKey = iconKey,
        colorAccent = colorAccent,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

fun CollectionSyncDto.Companion.fromEntity(entity: CollectionEntity): CollectionSyncDto =
    CollectionSyncDto(
        id = entity.id,
        name = entity.name,
        parentId = entity.parentId,
        colorAccent = entity.colorAccent,
        iconKey = entity.iconKey,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        isDeleted = entity.isDeleted
    )

fun BookmarkSyncDto.toEntity(): BookmarkEntity =
    BookmarkEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        faviconUrl = faviconUrl,
        thumbnailUrl = thumbnailUrl,
        sourcePlatform = sourcePlatform,
        collectionId = collectionId,
        notes = notes,
        tags = tags,
        isPinned = isPinned,
        syncStatus = "SYNCED",
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

fun BookmarkSyncDto.Companion.fromEntity(entity: BookmarkEntity): BookmarkSyncDto =
    BookmarkSyncDto(
        id = entity.id,
        url = entity.url,
        title = entity.title,
        description = entity.description,
        faviconUrl = entity.faviconUrl,
        thumbnailUrl = entity.thumbnailUrl,
        sourcePlatform = entity.sourcePlatform,
        collectionId = entity.collectionId,
        notes = entity.notes,
        tags = entity.tags,
        isPinned = entity.isPinned,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        isDeleted = entity.isDeleted
    )

