package com.madruga665.bookmarks.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections_table")
data class CollectionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "link_count") val linkCount: Int,
    @ColumnInfo(name = "subcollection_count") val subcollectionCount: Int = 0,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_accent") val colorAccent: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)

@Entity(tableName = "bookmarks_table")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "favicon_url") val faviconUrl: String?,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @ColumnInfo(name = "source_platform") val sourcePlatform: String? = null,
    @ColumnInfo(name = "collection_id") val collectionId: String = "col_unsorted",
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "tags") val tags: String = "",
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING_SYNC",
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)

@Entity(tableName = "paired_devices_table")
data class PairedDeviceEntity(
    @PrimaryKey @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "device_name") val deviceName: String,
    @ColumnInfo(name = "host_address") val hostAddress: String,
    @ColumnInfo(name = "http_port") val httpPort: Int = 43889,
    @ColumnInfo(name = "auth_token") val authToken: String,
    @ColumnInfo(name = "last_sync_timestamp") val lastSyncTimestamp: Long = 0L,
    @ColumnInfo(name = "is_paired") val isPaired: Boolean = true
)
