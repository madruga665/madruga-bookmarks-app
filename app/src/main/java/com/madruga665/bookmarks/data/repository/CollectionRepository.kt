package com.madruga665.bookmarks.data.repository

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.CollectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val bookmarkDao: BookmarkDao
) {
    private var isSeeded = false

    val collections: Flow<List<CollectionEntity>> = combine(
        collectionDao.getAllCollections(),
        bookmarkDao.getAllBookmarks()
    ) { dbCollections, bookmarks ->
        if (dbCollections.isEmpty() && !isSeeded) {
            isSeeded = true
            collectionDao.insertCollections(defaultCollections())
            defaultCollections().map { collection ->
                val actualCount = bookmarks.count { it.collectionId == collection.id }
                collection.copy(linkCount = actualCount)
            }
        } else {
            isSeeded = true
            dbCollections.map { collection ->
                val actualCount = bookmarks.count { it.collectionId == collection.id }
                collection.copy(linkCount = actualCount)
            }
        }
    }

    fun getCollectionById(collectionId: String): Flow<CollectionEntity?> = combine(
        collectionDao.getCollectionById(collectionId),
        bookmarkDao.getBookmarksByCollection(collectionId)
    ) { collection, bookmarks ->
        collection?.copy(linkCount = bookmarks.size)
    }

    suspend fun createCollection(
        name: String,
        colorAccent: String,
        iconKey: String = "folder"
    ): CollectionEntity? {
        if (name.isBlank() || name.length > 40) return null
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            linkCount = 0,
            subcollectionCount = 0,
            parentId = null,
            iconKey = if (iconKey.isNotBlank()) iconKey.trim() else "folder",
            colorAccent = colorAccent.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        collectionDao.insertCollection(entity)
        return entity
    }

    suspend fun updateCollection(id: String, name: String, colorAccent: String, iconKey: String) {
        if (name.isBlank()) return
        collectionDao.updateCollection(
            id = id,
            name = name.trim(),
            colorAccent = colorAccent.uppercase(),
            iconKey = iconKey.lowercase(),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteCollection(collectionId: String) {
        val now = System.currentTimeMillis()
        bookmarkDao.softDeleteBookmarksByCollectionId(collectionId, now)
        collectionDao.softDeleteCollectionById(collectionId, now)
    }

    suspend fun getModifiedCollectionsSince(sinceTimestamp: Long): List<CollectionEntity> {
        return collectionDao.getCollectionsModifiedSince(sinceTimestamp)
    }

    suspend fun upsertCollectionFromSync(collection: CollectionEntity) {
        val existing = collectionDao.getCollectionByIdDirect(collection.id)
        if (existing == null || collection.updatedAt > existing.updatedAt) {
            collectionDao.insertCollection(collection)
        }
    }

    private fun defaultCollections(): List<CollectionEntity> = listOf(
        CollectionEntity(
            id = "col_unsorted",
            name = "Unsorted",
            linkCount = 0,
            subcollectionCount = 0,
            parentId = null,
            iconKey = "folder",
            colorAccent = "YELLOW",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )
}
