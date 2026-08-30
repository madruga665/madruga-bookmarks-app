package com.madruga665.bookmarks.data.repository

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionRepositoryTest {

    private val collectionDao: CollectionDao = mockk()
    private val bookmarkDao: BookmarkDao = mockk()

    @org.junit.Before
    fun setUp() {
        every { collectionDao.getAllCollections() } returns flowOf(emptyList())
        every { bookmarkDao.getAllBookmarks() } returns flowOf(emptyList())
    }

    @Test
    fun collections_whenEmpty_returnsDefaultSampleCollections() = runTest {
        io.mockk.coEvery { collectionDao.insertCollections(any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        val result = repository.collections.first()
        assertEquals(1, result.size)
        assertEquals("Unsorted", result[0].name)
    }

    @Test
    fun createCollection_withValidInput_insertsAndReturnsEntity() = runTest {
        io.mockk.coEvery { collectionDao.insertCollection(any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        val entity = repository.createCollection("Design Systems", "#9B51E0", "palette")
        org.junit.Assert.assertNotNull(entity)
        assertEquals("Design Systems", entity?.name)
        assertEquals("#9B51E0", entity?.colorAccent)
        assertEquals("palette", entity?.iconKey)
    }

    @Test
    fun createCollection_withBlankIconKey_defaultsToFolder() = runTest {
        io.mockk.coEvery { collectionDao.insertCollection(any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        val entity = repository.createCollection("Docs", "#FFE600", "  ")
        org.junit.Assert.assertNotNull(entity)
        assertEquals("Docs", entity?.name)
        assertEquals("folder", entity?.iconKey)
    }

    @Test
    fun createCollection_withBlankName_returnsNull() = runTest {
        val repository = CollectionRepository(collectionDao, bookmarkDao)
        val entity = repository.createCollection("   ", "#FFE600", "folder")
        org.junit.Assert.assertNull(entity)
    }

    @Test
    fun createCollection_withNameExceeding40Chars_returnsNull() = runTest {
        val repository = CollectionRepository(collectionDao, bookmarkDao)
        val longName = "A".repeat(41)
        val entity = repository.createCollection(longName, "#FFE600", "folder")
        org.junit.Assert.assertNull(entity)
    }

    @Test
    fun deleteCollection_performsSoftDeleteOnBookmarksAndCollection() = runTest {
        io.mockk.coEvery { bookmarkDao.softDeleteBookmarksByCollectionId(any(), any()) } returns Unit
        io.mockk.coEvery { collectionDao.softDeleteCollectionById(any(), any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        repository.deleteCollection("col-to-delete")

        io.mockk.coVerify {
            bookmarkDao.softDeleteBookmarksByCollectionId("col-to-delete", any())
            collectionDao.softDeleteCollectionById("col-to-delete", any())
        }
    }

    @Test
    fun getModifiedCollectionsSince_delegatesToDao() = runTest {
        val sampleList = listOf(
            com.madruga665.bookmarks.data.local.CollectionEntity(
                id = "col-mod",
                name = "Modified",
                linkCount = 1,
                iconKey = "folder",
                colorAccent = "YELLOW",
                createdAt = 100L,
                updatedAt = 500L,
                isDeleted = false
            )
        )
        io.mockk.coEvery { collectionDao.getCollectionsModifiedSince(200L) } returns sampleList
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        val result = repository.getModifiedCollectionsSince(200L)
        assertEquals(1, result.size)
        assertEquals("col-mod", result[0].id)
    }

    @Test
    fun upsertCollectionFromSync_insertsWhenNotPresent() = runTest {
        io.mockk.coEvery { collectionDao.getCollectionByIdDirect("col-sync-new") } returns null
        io.mockk.coEvery { collectionDao.insertCollection(any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        val incoming = com.madruga665.bookmarks.data.local.CollectionEntity(
            id = "col-sync-new",
            name = "Synced Col",
            linkCount = 0,
            iconKey = "star",
            colorAccent = "BLUE",
            createdAt = 100L,
            updatedAt = 300L,
            isDeleted = false
        )

        repository.upsertCollectionFromSync(incoming)
        io.mockk.coVerify(exactly = 1) { collectionDao.insertCollection(incoming) }
    }

    @Test
    fun upsertCollectionFromSync_overwritesWhenIncomingIsNewer() = runTest {
        val local = com.madruga665.bookmarks.data.local.CollectionEntity(
            id = "col-sync-existing",
            name = "Local Name",
            linkCount = 0,
            iconKey = "folder",
            colorAccent = "YELLOW",
            createdAt = 100L,
            updatedAt = 200L,
            isDeleted = false
        )
        val incoming = local.copy(name = "Remote Name", updatedAt = 300L)

        io.mockk.coEvery { collectionDao.getCollectionByIdDirect("col-sync-existing") } returns local
        io.mockk.coEvery { collectionDao.insertCollection(any()) } returns Unit
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        repository.upsertCollectionFromSync(incoming)
        io.mockk.coVerify(exactly = 1) { collectionDao.insertCollection(incoming) }
    }

    @Test
    fun upsertCollectionFromSync_doesNotOverwriteWhenIncomingIsOlderOrEqual() = runTest {
        val local = com.madruga665.bookmarks.data.local.CollectionEntity(
            id = "col-sync-existing",
            name = "Local Name",
            linkCount = 0,
            iconKey = "folder",
            colorAccent = "YELLOW",
            createdAt = 100L,
            updatedAt = 500L,
            isDeleted = false
        )
        val olderIncoming = local.copy(name = "Old Remote Name", updatedAt = 400L)

        io.mockk.coEvery { collectionDao.getCollectionByIdDirect("col-sync-existing") } returns local
        val repository = CollectionRepository(collectionDao, bookmarkDao)

        repository.upsertCollectionFromSync(olderIncoming)
        io.mockk.coVerify(exactly = 0) { collectionDao.insertCollection(any()) }
    }
}
