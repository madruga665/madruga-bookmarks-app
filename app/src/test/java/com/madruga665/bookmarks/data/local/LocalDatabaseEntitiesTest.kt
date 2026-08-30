package com.madruga665.bookmarks.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDatabaseEntitiesTest {

    @Test
    fun collectionEntity_defaultIsDeleted_isFalse() {
        val collection = CollectionEntity(
            id = "col_1",
            name = "Work",
            linkCount = 3,
            iconKey = "folder",
            colorAccent = "YELLOW",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        assertFalse(collection.isDeleted)
        assertEquals("col_1", collection.id)
    }

    @Test
    fun bookmarkEntity_defaultIsDeleted_isFalse() {
        val bookmark = BookmarkEntity(
            id = "bm_1",
            url = "https://example.com",
            title = "Example",
            faviconUrl = "https://example.com/favicon.ico",
            createdAt = 1000L
        )
        assertFalse(bookmark.isDeleted)
        assertEquals("PENDING_SYNC", bookmark.syncStatus)
    }

    @Test
    fun pairedDeviceEntity_defaultsAndProperties() {
        val device = PairedDeviceEntity(
            deviceId = "dev_123",
            deviceName = "Arch Linux Plasma 6",
            hostAddress = "192.168.1.50",
            authToken = "sec_token_xyz"
        )
        assertEquals("dev_123", device.deviceId)
        assertEquals("Arch Linux Plasma 6", device.deviceName)
        assertEquals("192.168.1.50", device.hostAddress)
        assertEquals(43889, device.httpPort)
        assertEquals("sec_token_xyz", device.authToken)
        assertEquals(0L, device.lastSyncTimestamp)
        assertTrue(device.isPaired)
    }

    @Test
    fun migration_4_5_executesExpectedSqlStatements() {
        val mockDb: SupportSQLiteDatabase = mockk(relaxed = true)

        MIGRATION_4_5.migrate(mockDb)

        verify(exactly = 1) {
            mockDb.execSQL("ALTER TABLE collections_table ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        }
        verify(exactly = 1) {
            mockDb.execSQL("ALTER TABLE bookmarks_table ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        }
        verify(exactly = 1) {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS paired_devices_table") })
        }
    }
}
