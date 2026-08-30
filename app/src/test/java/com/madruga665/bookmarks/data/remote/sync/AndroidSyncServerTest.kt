package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.remote.sync.dto.PairRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSyncServerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val pairedDeviceDao = mockk<PairedDeviceDao>(relaxed = true)
    private val collectionDao = mockk<CollectionDao>(relaxed = true)
    private val bookmarkDao = mockk<BookmarkDao>(relaxed = true)

    private lateinit var syncServer: AndroidSyncServer
    private val okHttpClient = OkHttpClient()

    private var testPort: Int = 0

    @Before
    fun setUp() {
        // Find an open port
        val ephemeral = ServerSocket(0)
        testPort = ephemeral.localPort
        ephemeral.close()

        syncServer = AndroidSyncServer(
            pairedDeviceDao = pairedDeviceDao,
            collectionDao = collectionDao,
            bookmarkDao = bookmarkDao,
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO
        )

        syncServer.startServer(
            deviceId = "test-mobile-device",
            deviceName = "Android Test Device",
            preferredPort = testPort
        )
        Thread.sleep(100)
    }

    @After
    fun tearDown() {
        syncServer.stopServer()
    }

    @Test
    fun getStatus_returnsOnlineStatus() {
        val actualPort = syncServer.actualPort.value
        val request = Request.Builder()
            .url("http://127.0.0.1:$actualPort/api/v1/sync/status")
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        assertEquals(200, response.code)

        val json = JSONObject(response.body?.string() ?: "{}")
        assertEquals("ONLINE", json.getString("status"))
        assertEquals("test-mobile-device", json.getString("deviceId"))
        assertEquals("bookmarks-sync-v1", json.getString("protocol"))
    }

    @Test
    fun postPair_savesPairedDeviceAndReturnsToken() = runTest {
        val actualPort = syncServer.actualPort.value
        val pairReq = PairRequestDto(
            initiatorDeviceId = "kde-desktop-uuid",
            initiatorName = "KDE Desktop Plasmoid",
            verificationCode = "123456"
        )

        val request = Request.Builder()
            .url("http://127.0.0.1:$actualPort/api/v1/sync/pair")
            .post(pairReq.toJson().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        assertEquals(200, response.code)

        val json = JSONObject(response.body?.string() ?: "{}")
        assertEquals("PAIRED_SUCCESS", json.getString("status"))
        assertTrue(json.getString("authToken").isNotBlank())
        assertEquals("test-mobile-device", json.getString("responderDeviceId"))

        coVerify { pairedDeviceDao.insertPairedDevice(any()) }
    }

    @Test
    fun postExchange_reconcilesDeltaAndReturnsSuccess() = runTest {
        val actualPort = syncServer.actualPort.value
        coEvery { collectionDao.getCollectionsModifiedSince(any()) } returns emptyList()
        coEvery { bookmarkDao.getBookmarksModifiedSince(any()) } returns emptyList()

        val exchangeReq = SyncExchangeRequestDto(
            deviceId = "kde-desktop-uuid",
            lastSyncTimestamp = 0L,
            collections = emptyList(),
            bookmarks = emptyList()
        )

        val request = Request.Builder()
            .url("http://127.0.0.1:$actualPort/api/v1/sync/exchange")
            .header("Authorization", "Bearer test-auth-token")
            .post(exchangeReq.toJson().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        assertEquals(200, response.code)

        val json = JSONObject(response.body?.string() ?: "{}")
        assertEquals("SUCCESS", json.getString("status"))
        assertTrue(json.has("collections"))
        assertTrue(json.has("bookmarks"))
    }
}
