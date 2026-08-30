package com.madruga665.bookmarks.data.repository

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.BookmarkEntity
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.remote.sync.SyncHttpClient
import com.madruga665.bookmarks.data.remote.sync.dto.BookmarkSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.CollectionSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.PairResponseDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {

    private val pairedDeviceDao: PairedDeviceDao = mockk(relaxed = true)
    private val collectionDao: CollectionDao = mockk(relaxed = true)
    private val bookmarkDao: BookmarkDao = mockk(relaxed = true)
    private val syncHttpClient: SyncHttpClient = mockk(relaxed = true)
    private val peerDiscoveryManager: PeerDiscoveryManager = mockk(relaxed = true)

    private val discoveredPeersFlow = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var syncRepository: SyncRepository

    @Before
    fun setUp() {
        every { peerDiscoveryManager.discoveredPeers } returns discoveredPeersFlow
        every { pairedDeviceDao.getAllPairedDevices() } returns flowOf(emptyList())

        syncRepository = SyncRepository(
            pairedDeviceDao = pairedDeviceDao,
            collectionDao = collectionDao,
            bookmarkDao = bookmarkDao,
            syncHttpClient = syncHttpClient,
            peerDiscoveryManager = peerDiscoveryManager,
            ioDispatcher = testDispatcher,
            localDeviceId = "test-mobile-id",
            localDeviceName = "Pixel Test Device"
        )
    }

    // ==========================================
    // 1. Pairing Tests (T014, T016)
    // ==========================================

    @Test
    fun `pairWithPeer success stores device and triggers initial sync`() = runTest(testDispatcher) {
        val peer = DiscoveredPeer(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            deviceType = "desktop",
            lastSeenTimestamp = 1788000000000L
        )

        val pairResponse = PairResponseDto(
            status = "PAIRED_SUCCESS",
            authToken = "auth-token-12345",
            responderDeviceId = "desktop-001",
            responderName = "KDE Plasma Desktop"
        )

        coEvery {
            syncHttpClient.pair(
                hostAddress = "192.168.1.50",
                httpPort = 43889,
                initiatorDeviceId = "test-mobile-id",
                initiatorName = "Pixel Test Device",
                verificationCode = "123456"
            )
        } returns Result.success(pairResponse)

        val pairedSlot = slot<PairedDeviceEntity>()
        coEvery { pairedDeviceDao.insertPairedDevice(capture(pairedSlot)) } returns Unit
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } answers {
            pairedSlot.captured
        }

        coEvery { collectionDao.getCollectionsModifiedSince(0L) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(0L) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(
                hostAddress = "192.168.1.50",
                httpPort = 43889,
                authToken = "auth-token-12345",
                payload = any()
            )
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 1788000100000L,
                collections = emptyList(),
                bookmarks = emptyList()
            )
        )

        val result = syncRepository.pairWithPeer(peer, "123456")

        assertTrue(result.isSuccess)
        val device = result.getOrNull()
        assertNotNull(device)
        assertEquals("desktop-001", device?.deviceId)
        assertEquals("KDE Plasma Desktop", device?.deviceName)
        assertEquals("auth-token-12345", device?.authToken)
        assertEquals(SyncStatus.SYNCED, syncRepository.syncStatus.value)

        coVerify { pairedDeviceDao.insertPairedDevice(any()) }
        coVerify { pairedDeviceDao.updateLastSyncTimestamp("desktop-001", 1788000100000L) }
    }

    @Test
    fun `pairWithPeer when peer rejects verification code returns failure and sets status error`() = runTest(testDispatcher) {
        val peer = DiscoveredPeer(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            deviceType = "desktop",
            lastSeenTimestamp = 1788000000000L
        )

        val pairResponse = PairResponseDto(
            status = "INVALID_CODE",
            authToken = "",
            responderDeviceId = "",
            responderName = ""
        )

        coEvery {
            syncHttpClient.pair(any(), any(), any(), any(), any())
        } returns Result.success(pairResponse)

        val result = syncRepository.pairWithPeer(peer, "000000")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, syncRepository.syncStatus.value)
        coVerify(exactly = 0) { pairedDeviceDao.insertPairedDevice(any()) }
    }

    @Test
    fun `pairWithPeer when network connection fails returns failure and sets status disconnected`() = runTest(testDispatcher) {
        val peer = DiscoveredPeer(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            deviceType = "desktop",
            lastSeenTimestamp = 1788000000000L
        )

        coEvery {
            syncHttpClient.pair(any(), any(), any(), any(), any())
        } returns Result.failure(ConnectException("Failed to connect to 192.168.1.50:43889"))

        val result = syncRepository.pairWithPeer(peer, "123456")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.DISCONNECTED, syncRepository.syncStatus.value)
        coVerify(exactly = 0) { pairedDeviceDao.insertPairedDevice(any()) }
    }

    // ==========================================
    // 2. Delta Gathering Tests (T014, T016)
    // ==========================================

    @Test
    fun `syncWithPairedDevice gathers local deltas modified after lastSyncTimestamp`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device

        val localCol = CollectionEntity(
            id = "col-delta-1",
            name = "Delta Col",
            linkCount = 2,
            subcollectionCount = 0,
            parentId = null,
            iconKey = "folder",
            colorAccent = "YELLOW",
            createdAt = 500L,
            updatedAt = 1200L, // > 1000L
            isDeleted = false
        )
        val localBm = BookmarkEntity(
            id = "bm-delta-1",
            url = "https://example.com/delta",
            title = "Delta Bookmark",
            description = "Desc",
            faviconUrl = null,
            thumbnailUrl = null,
            sourcePlatform = "example.com",
            collectionId = "col-delta-1",
            notes = "Notes",
            tags = "tag1,tag2",
            isPinned = true,
            createdAt = 600L,
            updatedAt = 1300L, // > 1000L
            syncStatus = "PENDING_SYNC",
            isDeleted = false
        )

        coEvery { collectionDao.getCollectionsModifiedSince(1000L) } returns listOf(localCol)
        coEvery { bookmarkDao.getBookmarksModifiedSince(1000L) } returns listOf(localBm)

        val payloadSlot = slot<SyncExchangeRequestDto>()
        coEvery {
            syncHttpClient.exchange(
                hostAddress = "192.168.1.50",
                httpPort = 43889,
                authToken = "auth-token-12345",
                payload = capture(payloadSlot)
            )
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = emptyList(),
                bookmarks = emptyList()
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals(1, summary?.collectionsSent)
        assertEquals(1, summary?.bookmarksSent)
        assertEquals(2000L, summary?.serverTimestamp)

        val capturedPayload = payloadSlot.captured
        assertEquals("test-mobile-id", capturedPayload.deviceId)
        assertEquals(1000L, capturedPayload.lastSyncTimestamp)
        assertEquals(1, capturedPayload.collections.size)
        assertEquals("col-delta-1", capturedPayload.collections[0].id)
        assertEquals(1, capturedPayload.bookmarks.size)
        assertEquals("bm-delta-1", capturedPayload.bookmarks[0].id)

        // Verify sent bookmarks were marked SYNCED
        coVerify { bookmarkDao.markBookmarksSynced(listOf("bm-delta-1")) }
        coVerify { pairedDeviceDao.updateLastSyncTimestamp("desktop-001", 2000L) }
        assertEquals(SyncStatus.SYNCED, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncWithPairedDevice includes local soft-deleted items in delta exchange`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device

        val deletedBm = BookmarkEntity(
            id = "bm-deleted-1",
            url = "https://example.com/deleted",
            title = "Deleted Title",
            faviconUrl = null,
            createdAt = 500L,
            updatedAt = 1400L,
            syncStatus = "PENDING_SYNC",
            isDeleted = true
        )

        coEvery { collectionDao.getCollectionsModifiedSince(1000L) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(1000L) } returns listOf(deletedBm)

        val payloadSlot = slot<SyncExchangeRequestDto>()
        coEvery {
            syncHttpClient.exchange(any(), any(), any(), capture(payloadSlot))
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = emptyList(),
                bookmarks = emptyList()
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        val payload = payloadSlot.captured
        assertEquals(1, payload.bookmarks.size)
        assertEquals("bm-deleted-1", payload.bookmarks[0].id)
        assertTrue(payload.bookmarks[0].isDeleted)
    }

    // ==========================================
    // 3. Last-Write-Wins Reconciliation Tests (T016, T017)
    // ==========================================

    @Test
    fun `reconciliation when remote record is newer overwrites local record`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        // Local has older timestamp (1200L)
        val localBookmark = BookmarkEntity(
            id = "bm-conflict-1",
            url = "https://example.com/old",
            title = "Old Local Title",
            faviconUrl = null,
            createdAt = 500L,
            updatedAt = 1200L,
            isDeleted = false
        )
        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-conflict-1") } returns localBookmark

        // Remote has newer timestamp (1500L)
        val remoteBookmark = BookmarkSyncDto(
            id = "bm-conflict-1",
            url = "https://example.com/newer",
            title = "New Remote Title",
            collectionId = "col_unsorted",
            createdAt = 500L,
            updatedAt = 1500L,
            isDeleted = false
        )

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = emptyList(),
                bookmarks = listOf(remoteBookmark)
            )
        )

        val insertedBookmarkSlot = slot<BookmarkEntity>()
        coEvery { bookmarkDao.insertBookmark(capture(insertedBookmarkSlot)) } returns Unit

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { bookmarkDao.insertBookmark(any()) }
        val saved = insertedBookmarkSlot.captured
        assertEquals("bm-conflict-1", saved.id)
        assertEquals("https://example.com/newer", saved.url)
        assertEquals("New Remote Title", saved.title)
        assertEquals(1500L, saved.updatedAt)
        assertEquals("SYNCED", saved.syncStatus)
    }

    @Test
    fun `reconciliation when local record is newer keeps local record`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        // Local has newer timestamp (1800L)
        val localBookmark = BookmarkEntity(
            id = "bm-conflict-2",
            url = "https://example.com/local-latest",
            title = "Local Newer Title",
            faviconUrl = null,
            createdAt = 500L,
            updatedAt = 1800L,
            isDeleted = false
        )
        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-conflict-2") } returns localBookmark

        // Remote has older timestamp (1300L)
        val remoteBookmark = BookmarkSyncDto(
            id = "bm-conflict-2",
            url = "https://example.com/remote-stale",
            title = "Remote Stale Title",
            collectionId = "col_unsorted",
            createdAt = 500L,
            updatedAt = 1300L,
            isDeleted = false
        )

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = emptyList(),
                bookmarks = listOf(remoteBookmark)
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        // BookmarkDao.insertBookmark should NOT be called because local was newer
        coVerify(exactly = 0) { bookmarkDao.insertBookmark(any()) }
    }

    @Test
    fun `reconciliation when timestamps are equal applies remote update`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        // Local has timestamp (1500L)
        val localBookmark = BookmarkEntity(
            id = "bm-conflict-eq",
            url = "https://example.com/same-ts",
            title = "Local Title",
            faviconUrl = null,
            createdAt = 500L,
            updatedAt = 1500L,
            isDeleted = false
        )
        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-conflict-eq") } returns localBookmark

        // Remote has exact same timestamp (1500L)
        val remoteBookmark = BookmarkSyncDto(
            id = "bm-conflict-eq",
            url = "https://example.com/same-ts",
            title = "Remote Overwrite Title",
            collectionId = "col_unsorted",
            createdAt = 500L,
            updatedAt = 1500L,
            isDeleted = false
        )

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = emptyList(),
                bookmarks = listOf(remoteBookmark)
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { bookmarkDao.insertBookmark(any()) }
    }

    @Test
    fun `reconciliation when local record does not exist inserts new remote record`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 0L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery { collectionDao.getCollectionByIdDirect("col-new-remote") } returns null
        coEvery { bookmarkDao.getBookmarkByIdDirectIncludingDeleted("bm-new-remote") } returns null

        val remoteCol = CollectionSyncDto(
            id = "col-new-remote",
            name = "Remote New Col",
            colorAccent = "PURPLE",
            iconKey = "folder",
            createdAt = 1000L,
            updatedAt = 1000L,
            isDeleted = false
        )
        val remoteBm = BookmarkSyncDto(
            id = "bm-new-remote",
            url = "https://newsite.com",
            title = "New Site",
            collectionId = "col-new-remote",
            createdAt = 1100L,
            updatedAt = 1100L,
            isDeleted = false
        )

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = listOf(remoteCol),
                bookmarks = listOf(remoteBm)
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { collectionDao.insertCollection(any()) }
        coVerify(exactly = 1) { bookmarkDao.insertBookmark(any()) }
    }

    @Test
    fun `reconciliation when remote record is deleted marks local record isDeleted true`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        val localCol = CollectionEntity(
            id = "col-to-delete",
            name = "Active Collection",
            linkCount = 1,
            colorAccent = "BLUE",
            iconKey = "folder",
            createdAt = 500L,
            updatedAt = 1000L,
            isDeleted = false
        )
        coEvery { collectionDao.getCollectionByIdDirect("col-to-delete") } returns localCol

        val remoteTombstoneCol = CollectionSyncDto(
            id = "col-to-delete",
            name = "Active Collection",
            colorAccent = "BLUE",
            iconKey = "folder",
            createdAt = 500L,
            updatedAt = 1600L, // Newer than local 1000L
            isDeleted = true
        )

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L,
                collections = listOf(remoteTombstoneCol),
                bookmarks = emptyList()
            )
        )

        val insertedColSlot = slot<CollectionEntity>()
        coEvery { collectionDao.insertCollection(capture(insertedColSlot)) } returns Unit

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { collectionDao.insertCollection(any()) }
        assertTrue(insertedColSlot.captured.isDeleted)
    }

    // ==========================================
    // 4. Offline Error Handling & Status Transitions (T016, T017)
    // ==========================================

    @Test
    fun `syncWithPairedDevice when ConnectException transitions to DISCONNECTED`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.failure(ConnectException("Connection refused to 192.168.1.50:43889"))

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.DISCONNECTED, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncWithPairedDevice when UnknownHostException transitions to DISCONNECTED`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "unresolved.local",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.failure(UnknownHostException("unresolved.local"))

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.DISCONNECTED, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncWithPairedDevice when SocketTimeoutException transitions to DISCONNECTED`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.failure(SocketTimeoutException("Read timeout"))

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.DISCONNECTED, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncWithPairedDevice when general server error transitions to ERROR`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.failure(IOException("HTTP 500 Internal Server Error"))

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncWithPairedDevice when peer returns non-success status transitions to ERROR`() = runTest(testDispatcher) {
        val device = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "UNAUTHORIZED",
                serverTimestamp = 0L
            )
        )

        val result = syncRepository.syncWithPairedDevice("desktop-001")

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.ERROR, syncRepository.syncStatus.value)
    }

    // ==========================================
    // 5. syncAll & unpairDevice Tests
    // ==========================================

    @Test
    fun `syncAll when no paired devices returns success and sets status IDLE`() = runTest(testDispatcher) {
        coEvery { pairedDeviceDao.getAllPairedDevicesList() } returns emptyList()

        val result = syncRepository.syncAll()

        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.IDLE, syncRepository.syncStatus.value)
    }

    @Test
    fun `syncAll when multiple paired devices syncs all successfully`() = runTest(testDispatcher) {
        val device1 = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "Desktop 1",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "token-1",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )
        val device2 = PairedDeviceEntity(
            deviceId = "desktop-002",
            deviceName = "Desktop 2",
            hostAddress = "192.168.1.51",
            httpPort = 43889,
            authToken = "token-2",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )

        coEvery { pairedDeviceDao.getAllPairedDevicesList() } returns listOf(device1, device2)
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns device1
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-002") } returns device2
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L
            )
        )

        val result = syncRepository.syncAll()

        assertTrue(result.isSuccess)
        assertEquals(SyncStatus.SYNCED, syncRepository.syncStatus.value)
        coVerify(exactly = 2) { pairedDeviceDao.updateLastSyncTimestamp(any(), 2000L) }
    }

    @Test
    fun `unpairDevice deletes device and resets status to IDLE when no devices left`() = runTest(testDispatcher) {
        coEvery { pairedDeviceDao.unpairDevice("desktop-001") } returns Unit
        coEvery { pairedDeviceDao.deletePairedDevice("desktop-001") } returns Unit
        coEvery { pairedDeviceDao.getAllPairedDevicesList() } returns emptyList()

        syncRepository.setSyncStatus(SyncStatus.SYNCED)
        syncRepository.unpairDevice("desktop-001")

        coVerify { pairedDeviceDao.unpairDevice("desktop-001") }
        coVerify { pairedDeviceDao.deletePairedDevice("desktop-001") }
        assertEquals(SyncStatus.IDLE, syncRepository.syncStatus.value)
    }

    // ==========================================
    // 6. AutoSync Trigger Tests (T018)
    // ==========================================

    @Test
    fun `startAutoSync when matching peer is discovered triggers syncWithPairedDevice`() = runTest(testDispatcher) {
        val pairedDevice = PairedDeviceEntity(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            authToken = "auth-token-12345",
            lastSyncTimestamp = 1000L,
            isPaired = true
        )

        coEvery { pairedDeviceDao.getAllPairedDevicesList() } returns listOf(pairedDevice)
        coEvery { pairedDeviceDao.getPairedDeviceByIdDirect("desktop-001") } returns pairedDevice
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()
        coEvery {
            syncHttpClient.exchange(any(), any(), any(), any())
        } returns Result.success(
            SyncExchangeResponseDto(
                status = "SUCCESS",
                serverTimestamp = 2000L
            )
        )

        val testScope = TestScope(testDispatcher)
        syncRepository.startAutoSync(testScope)

        val peer = DiscoveredPeer(
            deviceId = "desktop-001",
            deviceName = "KDE Plasma Desktop",
            hostAddress = "192.168.1.50",
            httpPort = 43889,
            deviceType = "desktop",
            lastSeenTimestamp = 1788000000000L
        )

        discoveredPeersFlow.value = listOf(peer)
        testScope.advanceUntilIdle()

        coVerify { syncHttpClient.exchange("192.168.1.50", 43889, "auth-token-12345", any()) }
        assertEquals(SyncStatus.SYNCED, syncRepository.syncStatus.value)
    }
}
