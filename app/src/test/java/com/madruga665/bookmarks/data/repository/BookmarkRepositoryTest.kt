package com.madruga665.bookmarks.data.repository

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.BookmarkEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BookmarkRepositoryTest {

    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { bookmarkDao.getAllBookmarks() } returns flowOf(emptyList())
    }

    @Test
    fun deleteBookmark_performsSoftDelete() = runTest {
        coEvery { bookmarkDao.softDeleteBookmarkById(any(), any()) } returns Unit
        val repository = BookmarkRepository(bookmarkDao)

        repository.deleteBookmark("bm-123")

        coVerify(exactly = 1) { bookmarkDao.softDeleteBookmarkById("bm-123", any()) }
    }

    @Test
    fun getModifiedBookmarksSince_delegatesToDao() = runTest {
        val modifiedList = listOf(
            BookmarkEntity(
                id = "bm-1",
                url = "https://kotlinlang.org",
                title = "Kotlin",
                faviconUrl = "https://kotlinlang.org/favicon.ico",
                collectionId = "col-1",
                createdAt = 1000L,
                updatedAt = 3000L,
                isDeleted = false
            )
        )
        coEvery { bookmarkDao.getBookmarksModifiedSince(2000L) } returns modifiedList
        val repository = BookmarkRepository(bookmarkDao)

        val result = repository.getModifiedBookmarksSince(2000L)

        assertEquals(1, result.size)
        assertEquals("bm-1", result[0].id)
        coVerify(exactly = 1) { bookmarkDao.getBookmarksModifiedSince(2000L) }
    }

    @Test
    fun upsertBookmarkFromSync_insertsWhenNotPresent() = runTest {
        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-new") } returns null
        coEvery { bookmarkDao.insertBookmark(any()) } returns Unit
        val repository = BookmarkRepository(bookmarkDao)

        val incoming = BookmarkEntity(
            id = "bm-new",
            url = "https://example.com",
            title = "New Title",
            faviconUrl = null,
            collectionId = "col-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = false
        )

        repository.upsertBookmarkFromSync(incoming)

        coVerify(exactly = 1) { bookmarkDao.insertBookmark(incoming) }
    }

    @Test
    fun upsertBookmarkFromSync_overwritesWhenIncomingIsNewer() = runTest {
        val local = BookmarkEntity(
            id = "bm-existing",
            url = "https://example.com",
            title = "Local Title",
            faviconUrl = null,
            collectionId = "col-1",
            createdAt = 1000L,
            updatedAt = 2000L,
            isDeleted = false
        )
        val incoming = local.copy(title = "Remote Title", updatedAt = 3000L)

        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-existing") } returns local
        coEvery { bookmarkDao.insertBookmark(any()) } returns Unit
        val repository = BookmarkRepository(bookmarkDao)

        repository.upsertBookmarkFromSync(incoming)

        coVerify(exactly = 1) { bookmarkDao.insertBookmark(incoming) }
    }

    @Test
    fun upsertBookmarkFromSync_doesNotOverwriteWhenIncomingIsOlderOrEqual() = runTest {
        val local = BookmarkEntity(
            id = "bm-existing",
            url = "https://example.com",
            title = "Local Title",
            faviconUrl = null,
            collectionId = "col-1",
            createdAt = 1000L,
            updatedAt = 4000L,
            isDeleted = false
        )
        val olderIncoming = local.copy(title = "Old Remote Title", updatedAt = 3000L)

        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-existing") } returns local
        val repository = BookmarkRepository(bookmarkDao)

        repository.upsertBookmarkFromSync(olderIncoming)

        coVerify(exactly = 0) { bookmarkDao.insertBookmark(any()) }
    }
}
