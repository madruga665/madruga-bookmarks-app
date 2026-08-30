package com.madruga665.bookmarks.data.remote.sync.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDtoTest {

    @Test
    fun collectionSyncDto_serializationAndDeserialization_withParent() {
        val collection = CollectionSyncDto(
            id = "col-1",
            name = "Articles",
            parentId = "col-root",
            colorAccent = "BLUE",
            iconKey = "folder",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = false
        )

        val json = collection.toJson()
        val parsed = CollectionSyncDto.fromJson(json)

        assertEquals("col-1", parsed.id)
        assertEquals("Articles", parsed.name)
        assertEquals("col-root", parsed.parentId)
        assertEquals("BLUE", parsed.colorAccent)
        assertEquals("folder", parsed.iconKey)
        assertEquals(1000L, parsed.createdAt)
        assertEquals(2000L, parsed.updatedAt)
        assertFalse(parsed.isDeleted)
    }

    @Test
    fun collectionSyncDto_serializationAndDeserialization_nullParent() {
        val collection = CollectionSyncDto(
            id = "col-2",
            name = "Root Collection",
            parentId = null,
            colorAccent = "YELLOW",
            iconKey = "bookmark",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = true
        )

        val json = collection.toJson()
        val parsed = CollectionSyncDto.fromJson(json)

        assertEquals("col-2", parsed.id)
        assertNull(parsed.parentId)
        assertTrue(parsed.isDeleted)
    }

    @Test
    fun bookmarkSyncDto_serializationAndDeserialization_fullFields() {
        val bookmark = BookmarkSyncDto(
            id = "bm-1",
            url = "https://example.com",
            title = "Example Title",
            description = "Description here",
            faviconUrl = "https://example.com/fav.png",
            thumbnailUrl = "https://example.com/thumb.jpg",
            sourcePlatform = "example.com",
            collectionId = "col-1",
            notes = "My notes",
            tags = "tech,news",
            isPinned = true,
            createdAt = 5000L,
            updatedAt = 6000L,
            isDeleted = false
        )

        val json = bookmark.toJson()
        val parsed = BookmarkSyncDto.fromJson(json)

        assertEquals("bm-1", parsed.id)
        assertEquals("https://example.com", parsed.url)
        assertEquals("Example Title", parsed.title)
        assertEquals("Description here", parsed.description)
        assertEquals("https://example.com/fav.png", parsed.faviconUrl)
        assertEquals("https://example.com/thumb.jpg", parsed.thumbnailUrl)
        assertEquals("example.com", parsed.sourcePlatform)
        assertEquals("col-1", parsed.collectionId)
        assertEquals("My notes", parsed.notes)
        assertEquals("tech,news", parsed.tags)
        assertTrue(parsed.isPinned)
        assertEquals(5000L, parsed.createdAt)
        assertEquals(6000L, parsed.updatedAt)
        assertFalse(parsed.isDeleted)
    }

    @Test
    fun bookmarkSyncDto_serializationAndDeserialization_nullFields() {
        val bookmark = BookmarkSyncDto(
            id = "bm-2",
            url = "https://minimal.com",
            title = null,
            description = null,
            faviconUrl = null,
            thumbnailUrl = null,
            sourcePlatform = null,
            collectionId = "col_unsorted",
            notes = null,
            tags = "",
            isPinned = false,
            createdAt = 7000L,
            updatedAt = 8000L,
            isDeleted = true
        )

        val json = bookmark.toJson()
        val parsed = BookmarkSyncDto.fromJson(json)

        assertEquals("bm-2", parsed.id)
        assertEquals("https://minimal.com", parsed.url)
        assertNull(parsed.title)
        assertNull(parsed.description)
        assertNull(parsed.faviconUrl)
        assertNull(parsed.thumbnailUrl)
        assertNull(parsed.sourcePlatform)
        assertEquals("col_unsorted", parsed.collectionId)
        assertNull(parsed.notes)
        assertEquals("", parsed.tags)
        assertFalse(parsed.isPinned)
        assertTrue(parsed.isDeleted)
    }

    @Test
    fun syncExchangeRequestDto_serializationAndDeserialization() {
        val request = SyncExchangeRequestDto(
            deviceId = "dev-abc",
            lastSyncTimestamp = 123456789L,
            collections = listOf(
                CollectionSyncDto(
                    id = "col-1",
                    name = "Dev",
                    colorAccent = "GREEN",
                    iconKey = "code",
                    createdAt = 100L,
                    updatedAt = 200L
                )
            ),
            bookmarks = listOf(
                BookmarkSyncDto(
                    id = "bm-1",
                    url = "https://kotlinlang.org",
                    title = "Kotlin",
                    collectionId = "col-1",
                    createdAt = 150L,
                    updatedAt = 250L
                )
            )
        )

        val json = request.toJson()
        val parsed = SyncExchangeRequestDto.fromJson(json)

        assertEquals("dev-abc", parsed.deviceId)
        assertEquals(123456789L, parsed.lastSyncTimestamp)
        assertEquals(1, parsed.collections.size)
        assertEquals("col-1", parsed.collections[0].id)
        assertEquals(1, parsed.bookmarks.size)
        assertEquals("https://kotlinlang.org", parsed.bookmarks[0].url)
    }

    @Test
    fun syncExchangeResponseDto_serializationAndDeserialization() {
        val response = SyncExchangeResponseDto(
            status = "SUCCESS",
            serverTimestamp = 987654321L,
            collections = listOf(
                CollectionSyncDto(
                    id = "col-remote",
                    name = "Remote Col",
                    colorAccent = "PURPLE",
                    iconKey = "star",
                    createdAt = 300L,
                    updatedAt = 400L
                )
            ),
            bookmarks = listOf(
                BookmarkSyncDto(
                    id = "bm-remote",
                    url = "https://kde.org",
                    title = "KDE",
                    collectionId = "col-remote",
                    createdAt = 350L,
                    updatedAt = 450L
                )
            )
        )

        val json = response.toJson()
        val parsed = SyncExchangeResponseDto.fromJson(json)

        assertEquals("SUCCESS", parsed.status)
        assertEquals(987654321L, parsed.serverTimestamp)
        assertEquals(1, parsed.collections.size)
        assertEquals("col-remote", parsed.collections[0].id)
        assertEquals(1, parsed.bookmarks.size)
        assertEquals("https://kde.org", parsed.bookmarks[0].url)
    }
}
