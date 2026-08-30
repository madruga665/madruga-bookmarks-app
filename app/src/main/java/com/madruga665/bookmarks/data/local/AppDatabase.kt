package com.madruga665.bookmarks.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections_table WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections_table WHERE id = :collectionId AND is_deleted = 0")
    fun getCollectionById(collectionId: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections_table WHERE id = :id")
    suspend fun getCollectionByIdDirect(id: String): CollectionEntity?

    @Query("SELECT * FROM collections_table WHERE updated_at > :sinceTimestamp")
    suspend fun getCollectionsModifiedSince(sinceTimestamp: Long): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    @Query("UPDATE collections_table SET link_count = link_count + 1 WHERE id = :collectionId")
    suspend fun incrementLinkCount(collectionId: String)

    @Query("UPDATE collections_table SET name = :name, color_accent = :colorAccent, icon_key = :iconKey, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCollection(id: String, name: String, colorAccent: String, iconKey: String, updatedAt: Long)

    @Query("UPDATE collections_table SET is_deleted = 1, updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun softDeleteCollectionById(collectionId: String, updatedAt: Long)

    @Query("DELETE FROM collections_table WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks_table WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks_table WHERE collection_id = :collectionId AND is_deleted = 0 ORDER BY is_pinned DESC, created_at DESC")
    fun getBookmarksByCollection(collectionId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks_table WHERE collection_id = :collectionId AND is_deleted = 0 ORDER BY is_pinned DESC, created_at DESC")
    suspend fun getBookmarksByCollectionList(collectionId: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks_table WHERE id = :bookmarkId AND is_deleted = 0")
    fun getBookmarkById(bookmarkId: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks_table WHERE id = :bookmarkId AND is_deleted = 0")
    suspend fun getBookmarkByIdDirect(bookmarkId: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks_table WHERE id = :id")
    suspend fun getBookmarkByIdDirectIncludingDeleted(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks_table WHERE updated_at > :sinceTimestamp")
    suspend fun getBookmarksModifiedSince(sinceTimestamp: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks_table SET sync_status = 'SYNCED' WHERE id IN (:bookmarkIds)")
    suspend fun markBookmarksSynced(bookmarkIds: List<String>)

    @Query("UPDATE bookmarks_table SET title = :title, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateBookmarkTitle(id: String, title: String?, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET notes = :notes, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateBookmarkNotes(id: String, notes: String?, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET tags = :tags, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateBookmarkTags(id: String, tags: String, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET is_pinned = :isPinned, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateBookmarkPinned(id: String, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET collection_id = :collectionId, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :id")
    suspend fun updateBookmarkCollection(id: String, collectionId: String, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET collection_id = 'col_unsorted' WHERE collection_id = :collectionId")
    suspend fun resetBookmarkCollectionId(collectionId: String)

    @Query("UPDATE bookmarks_table SET is_deleted = 1, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE id = :bookmarkId")
    suspend fun softDeleteBookmarkById(bookmarkId: String, updatedAt: Long)

    @Query("UPDATE bookmarks_table SET is_deleted = 1, updated_at = :updatedAt, sync_status = 'PENDING_SYNC' WHERE collection_id = :collectionId")
    suspend fun softDeleteBookmarksByCollectionId(collectionId: String, updatedAt: Long)

    @Query("DELETE FROM bookmarks_table WHERE collection_id = :collectionId")
    suspend fun deleteBookmarksByCollectionId(collectionId: String)

    @Query("DELETE FROM bookmarks_table WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: String)
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE collections_table ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bookmarks_table ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS paired_devices_table (
                device_id TEXT NOT NULL PRIMARY KEY,
                device_name TEXT NOT NULL,
                host_address TEXT NOT NULL,
                http_port INTEGER NOT NULL DEFAULT 43889,
                auth_token TEXT NOT NULL,
                last_sync_timestamp INTEGER NOT NULL DEFAULT 0,
                is_paired INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [CollectionEntity::class, BookmarkEntity::class, PairedDeviceEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
}
