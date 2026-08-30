package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.remote.sync.dto.BookmarkSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.CollectionSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class SyncHttpClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var syncHttpClient: SyncHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        syncHttpClient = SyncHttpClient(okHttpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `pair sends correct request and returns success on valid verification code`() = runTest {
        val responseJson = JSONObject().apply {
            put("status", "PAIRED_SUCCESS")
            put("authToken", "auth_token_secret_123")
            put("responderDeviceId", "desktop-guid-001")
            put("responderName", "KDE Plasma Desktop")
        }.toString()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val result = syncHttpClient.pair(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "482910"
        )

        assertTrue(result.isSuccess)
        val pairResponse = result.getOrNull()
        assertNotNull(pairResponse)
        assertEquals("PAIRED_SUCCESS", pairResponse?.status)
        assertEquals("auth_token_secret_123", pairResponse?.authToken)
        assertEquals("desktop-guid-001", pairResponse?.responderDeviceId)
        assertEquals("KDE Plasma Desktop", pairResponse?.responderName)

        val recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)
        assertEquals("POST", recordedRequest?.method)
        assertEquals("/api/v1/sync/pair", recordedRequest?.path)
        assertTrue(recordedRequest?.getHeader("Content-Type")?.startsWith("application/json") == true)

        val recordedBody = JSONObject(recordedRequest?.body?.readUtf8() ?: "{}")
        assertEquals("mobile-guid-001", recordedBody.getString("initiatorDeviceId"))
        assertEquals("Pixel 8 Pro", recordedBody.getString("initiatorName"))
        assertEquals("482910", recordedBody.getString("verificationCode"))
    }

    @Test
    fun `pair returns INVALID_CODE status when peer rejects verification code`() = runTest {
        val responseJson = JSONObject().apply {
            put("status", "INVALID_CODE")
            put("authToken", "")
            put("responderDeviceId", "")
            put("responderName", "")
        }.toString()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val result = syncHttpClient.pair(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "000000"
        )

        assertTrue(result.isSuccess)
        val pairResponse = result.getOrNull()
        assertNotNull(pairResponse)
        assertEquals("INVALID_CODE", pairResponse?.status)
        assertEquals("", pairResponse?.authToken)
    }

    @Test
    fun `pair returns REJECTED status when user denies pairing dialog on peer`() = runTest {
        val responseJson = JSONObject().apply {
            put("status", "REJECTED")
            put("authToken", "")
            put("responderDeviceId", "")
            put("responderName", "")
        }.toString()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val result = syncHttpClient.pair(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "482910"
        )

        assertTrue(result.isSuccess)
        val pairResponse = result.getOrNull()
        assertNotNull(pairResponse)
        assertEquals("REJECTED", pairResponse?.status)
    }

    @Test
    fun `pair returns failure on network connection error or unreachable host`() = runTest {
        val result = syncHttpClient.pair(
            hostAddress = "192.0.2.1", // non-routable TEST-NET-1 IP
            httpPort = 65432,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "482910"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `pair returns failure on server timeout or disconnect`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        val result = syncHttpClient.pair(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "482910"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `pair returns failure on invalid non-json response body`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val result = syncHttpClient.pair(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            initiatorDeviceId = "mobile-guid-001",
            initiatorName = "Pixel 8 Pro",
            verificationCode = "482910"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `exchange sends authorization header and payload and returns success response`() = runTest {
        val responseJson = JSONObject().apply {
            put("status", "SUCCESS")
            put("serverTimestamp", 1725000000000L)
            put("collections", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "col_work")
                    put("name", "Work")
                    put("colorAccent", "BLUE")
                    put("iconKey", "briefcase")
                    put("createdAt", 1725000000000L)
                    put("updatedAt", 1725000000000L)
                    put("isDeleted", false)
                })
            })
            put("bookmarks", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "bm_1")
                    put("url", "https://kotlinlang.org")
                    put("title", "Kotlin")
                    put("collectionId", "col_work")
                    put("createdAt", 1725000000000L)
                    put("updatedAt", 1725000000000L)
                    put("isDeleted", false)
                })
            })
        }.toString()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        val payload = SyncExchangeRequestDto(
            deviceId = "mobile-guid-001",
            lastSyncTimestamp = 1724000000000L,
            collections = listOf(
                CollectionSyncDto(
                    id = "col_local",
                    name = "Local Collection",
                    colorAccent = "GREEN",
                    iconKey = "folder",
                    createdAt = 1724500000000L,
                    updatedAt = 1724500000000L
                )
            ),
            bookmarks = listOf(
                BookmarkSyncDto(
                    id = "bm_local",
                    url = "https://android.com",
                    title = "Android",
                    collectionId = "col_local",
                    createdAt = 1724500000000L,
                    updatedAt = 1724500000000L
                )
            )
        )

        val result = syncHttpClient.exchange(
            hostAddress = mockWebServer.hostName,
            httpPort = mockWebServer.port,
            authToken = "auth_token_secret_123",
            payload = payload
        )

        assertTrue(result.isSuccess)
        val exchangeResponse = result.getOrNull()
        assertNotNull(exchangeResponse)
        assertEquals("SUCCESS", exchangeResponse?.status)
        assertEquals(1725000000000L, exchangeResponse?.serverTimestamp)
        assertEquals(1, exchangeResponse?.collections?.size)
        assertEquals("col_work", exchangeResponse?.collections?.first()?.id)
        assertEquals(1, exchangeResponse?.bookmarks?.size)
        assertEquals("https://kotlinlang.org", exchangeResponse?.bookmarks?.first()?.url)

        val recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)
        assertEquals("POST", recordedRequest?.method)
        assertEquals("/api/v1/sync/exchange", recordedRequest?.path)
        assertEquals("Bearer auth_token_secret_123", recordedRequest?.getHeader("Authorization"))
        assertTrue(recordedRequest?.getHeader("Content-Type")?.startsWith("application/json") == true)
    }
}
