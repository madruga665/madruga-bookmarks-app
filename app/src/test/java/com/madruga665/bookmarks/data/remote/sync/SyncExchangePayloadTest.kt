package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.remote.sync.dto.BookmarkSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.CollectionSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeResponseDto
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncExchangePayloadTest {

    @Test
    fun syncExchangeRequestDto_encodesAndDecodes_withActiveAndTombstoneRecords() {
        val activeCollection = CollectionSyncDto(
            id = "col-active-1",
            name = "Design Inspiration",
            parentId = "col-root",
            colorAccent = "PURPLE",
            iconKey = "palette",
            createdAt = 1788000000000L,
            updatedAt = 1788000100000L,
            isDeleted = false
        )

        val tombstoneCollection = CollectionSyncDto(
            id = "col-deleted-2",
            name = "Old Archive",
            parentId = null,
            colorAccent = "YELLOW",
            iconKey = "folder",
            createdAt = 1788000000000L,
            updatedAt = 1788000200000L,
            isDeleted = true
        )

        val activeBookmark = BookmarkSyncDto(
            id = "bm-active-1",
            url = "https://kotlinlang.org",
            title = "Kotlin Programming Language",
            description = "Concise, cross-platform language",
            faviconUrl = "https://kotlinlang.org/favicon.ico",
            thumbnailUrl = "https://kotlinlang.org/assets/images/banner.png",
            sourcePlatform = "kotlinlang.org",
            collectionId = "col-active-1",
            notes = "Official website and docs",
            tags = "kotlin,dev,android",
            isPinned = true,
            createdAt = 1788000050000L,
            updatedAt = 1788000150000L,
            isDeleted = false
        )

        val tombstoneBookmark = BookmarkSyncDto(
            id = "bm-deleted-2",
            url = "https://example.com/deprecated",
            title = null,
            description = null,
            faviconUrl = null,
            thumbnailUrl = null,
            sourcePlatform = null,
            collectionId = "col_unsorted",
            notes = null,
            tags = "",
            isPinned = false,
            createdAt = 1788000010000L,
            updatedAt = 1788000250000L,
            isDeleted = true
        )

        val originalRequest = SyncExchangeRequestDto(
            deviceId = "device-android-pixel8",
            lastSyncTimestamp = 1788000000000L,
            collections = listOf(activeCollection, tombstoneCollection),
            bookmarks = listOf(activeBookmark, tombstoneBookmark)
        )

        val jsonString = originalRequest.toJson()
        val decodedRequest = SyncExchangeRequestDto.fromJson(jsonString)

        assertEquals("device-android-pixel8", decodedRequest.deviceId)
        assertEquals(1788000000000L, decodedRequest.lastSyncTimestamp)
        assertEquals(2, decodedRequest.collections.size)
        assertEquals(2, decodedRequest.bookmarks.size)

        // Validate Active Collection
        val col1 = decodedRequest.collections[0]
        assertEquals("col-active-1", col1.id)
        assertEquals("Design Inspiration", col1.name)
        assertEquals("col-root", col1.parentId)
        assertEquals("PURPLE", col1.colorAccent)
        assertEquals("palette", col1.iconKey)
        assertEquals(1788000000000L, col1.createdAt)
        assertEquals(1788000100000L, col1.updatedAt)
        assertFalse(col1.isDeleted)

        // Validate Tombstone Collection
        val col2 = decodedRequest.collections[1]
        assertEquals("col-deleted-2", col2.id)
        assertEquals("Old Archive", col2.name)
        assertNull(col2.parentId)
        assertEquals("YELLOW", col2.colorAccent)
        assertEquals("folder", col2.iconKey)
        assertEquals(1788000000000L, col2.createdAt)
        assertEquals(1788000200000L, col2.updatedAt)
        assertTrue(col2.isDeleted)

        // Validate Active Bookmark
        val bm1 = decodedRequest.bookmarks[0]
        assertEquals("bm-active-1", bm1.id)
        assertEquals("https://kotlinlang.org", bm1.url)
        assertEquals("Kotlin Programming Language", bm1.title)
        assertEquals("Concise, cross-platform language", bm1.description)
        assertEquals("https://kotlinlang.org/favicon.ico", bm1.faviconUrl)
        assertEquals("https://kotlinlang.org/assets/images/banner.png", bm1.thumbnailUrl)
        assertEquals("kotlinlang.org", bm1.sourcePlatform)
        assertEquals("col-active-1", bm1.collectionId)
        assertEquals("Official website and docs", bm1.notes)
        assertEquals("kotlin,dev,android", bm1.tags)
        assertTrue(bm1.isPinned)
        assertEquals(1788000050000L, bm1.createdAt)
        assertEquals(1788000150000L, bm1.updatedAt)
        assertFalse(bm1.isDeleted)

        // Validate Tombstone Bookmark
        val bm2 = decodedRequest.bookmarks[1]
        assertEquals("bm-deleted-2", bm2.id)
        assertEquals("https://example.com/deprecated", bm2.url)
        assertNull(bm2.title)
        assertNull(bm2.description)
        assertNull(bm2.faviconUrl)
        assertNull(bm2.thumbnailUrl)
        assertNull(bm2.sourcePlatform)
        assertEquals("col_unsorted", bm2.collectionId)
        assertNull(bm2.notes)
        assertEquals("", bm2.tags)
        assertFalse(bm2.isPinned)
        assertEquals(1788000010000L, bm2.createdAt)
        assertEquals(1788000250000L, bm2.updatedAt)
        assertTrue(bm2.isDeleted)
    }

    @Test
    fun syncExchangeResponseDto_encodesAndDecodes_withActiveAndTombstoneRecords() {
        val remoteCollection = CollectionSyncDto(
            id = "col-kde-1",
            name = "KDE Plasmoid Desktop",
            parentId = null,
            colorAccent = "BLUE",
            iconKey = "monitor",
            createdAt = 1788000100000L,
            updatedAt = 1788000300000L,
            isDeleted = false
        )

        val remoteDeletedCollection = CollectionSyncDto(
            id = "col-kde-deleted",
            name = "Deleted On Desktop",
            parentId = null,
            colorAccent = "RED",
            iconKey = "trash",
            createdAt = 1788000050000L,
            updatedAt = 1788000350000L,
            isDeleted = true
        )

        val remoteBookmark = BookmarkSyncDto(
            id = "bm-kde-1",
            url = "https://kde.org",
            title = "KDE - Experience Freedom!",
            description = "KDE is an international free software community",
            faviconUrl = "https://kde.org/favicon.ico",
            thumbnailUrl = "https://kde.org/banner.png",
            sourcePlatform = "kde.org",
            collectionId = "col-kde-1",
            notes = "Desktop environment",
            tags = "kde,linux,plasma",
            isPinned = true,
            createdAt = 1788000120000L,
            updatedAt = 1788000320000L,
            isDeleted = false
        )

        val remoteDeletedBookmark = BookmarkSyncDto(
            id = "bm-kde-deleted",
            url = "https://legacy.site.org",
            title = "Legacy Site",
            collectionId = "col_unsorted",
            createdAt = 1788000080000L,
            updatedAt = 1788000380000L,
            isDeleted = true
        )

        val originalResponse = SyncExchangeResponseDto(
            status = "SUCCESS",
            serverTimestamp = 1788000400000L,
            collections = listOf(remoteCollection, remoteDeletedCollection),
            bookmarks = listOf(remoteBookmark, remoteDeletedBookmark)
        )

        val jsonString = originalResponse.toJson()
        val decodedResponse = SyncExchangeResponseDto.fromJson(jsonString)

        assertEquals("SUCCESS", decodedResponse.status)
        assertEquals(1788000400000L, decodedResponse.serverTimestamp)
        assertEquals(2, decodedResponse.collections.size)
        assertEquals(2, decodedResponse.bookmarks.size)

        // Validate remote collection
        val col1 = decodedResponse.collections[0]
        assertEquals("col-kde-1", col1.id)
        assertEquals("KDE Plasmoid Desktop", col1.name)
        assertNull(col1.parentId)
        assertFalse(col1.isDeleted)

        // Validate remote tombstone collection
        val col2 = decodedResponse.collections[1]
        assertEquals("col-kde-deleted", col2.id)
        assertTrue(col2.isDeleted)

        // Validate remote bookmark
        val bm1 = decodedResponse.bookmarks[0]
        assertEquals("bm-kde-1", bm1.id)
        assertEquals("https://kde.org", bm1.url)
        assertEquals("KDE - Experience Freedom!", bm1.title)
        assertFalse(bm1.isDeleted)

        // Validate remote tombstone bookmark
        val bm2 = decodedResponse.bookmarks[1]
        assertEquals("bm-kde-deleted", bm2.id)
        assertEquals("https://legacy.site.org", bm2.url)
        assertTrue(bm2.isDeleted)
    }

    @Test
    fun syncExchangeRequestDto_handlesEmptyCollectionsAndBookmarks() {
        val request = SyncExchangeRequestDto(
            deviceId = "dev-empty",
            lastSyncTimestamp = 0L,
            collections = emptyList(),
            bookmarks = emptyList()
        )

        val json = request.toJson()
        val parsed = SyncExchangeRequestDto.fromJson(json)

        assertEquals("dev-empty", parsed.deviceId)
        assertEquals(0L, parsed.lastSyncTimestamp)
        assertTrue(parsed.collections.isEmpty())
        assertTrue(parsed.bookmarks.isEmpty())
    }

    @Test
    fun syncExchangeResponseDto_handlesEmptyCollectionsAndBookmarks() {
        val response = SyncExchangeResponseDto(
            status = "SUCCESS",
            serverTimestamp = 1788000000000L,
            collections = emptyList(),
            bookmarks = emptyList()
        )

        val json = response.toJson()
        val parsed = SyncExchangeResponseDto.fromJson(json)

        assertEquals("SUCCESS", parsed.status)
        assertEquals(1788000000000L, parsed.serverTimestamp)
        assertTrue(parsed.collections.isEmpty())
        assertTrue(parsed.bookmarks.isEmpty())
    }

    @Test
    fun syncExchangeRequestDto_fromJsonObjectWithMissingOptionalFields() {
        val rawJson = JSONObject().apply {
            put("deviceId", "device-minimal")
        }

        val parsed = SyncExchangeRequestDto.fromJson(rawJson)

        assertEquals("device-minimal", parsed.deviceId)
        assertEquals(0L, parsed.lastSyncTimestamp)
        assertTrue(parsed.collections.isEmpty())
        assertTrue(parsed.bookmarks.isEmpty())
    }

    @Test
    fun syncExchangeResponseDto_fromJsonObjectWithMissingOptionalFields() {
        val rawJson = JSONObject().apply {
            put("status", "SUCCESS")
        }

        val parsed = SyncExchangeResponseDto.fromJson(rawJson)

        assertEquals("SUCCESS", parsed.status)
        assertEquals(0L, parsed.serverTimestamp)
        assertTrue(parsed.collections.isEmpty())
        assertTrue(parsed.bookmarks.isEmpty())
    }
}
