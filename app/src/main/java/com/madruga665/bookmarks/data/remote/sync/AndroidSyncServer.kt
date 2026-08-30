package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.dto.BookmarkSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.CollectionSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.PairRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.PairResponseDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeResponseDto
import com.madruga665.bookmarks.data.remote.sync.dto.fromEntity
import com.madruga665.bookmarks.data.remote.sync.dto.toEntity

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded HTTP Sync Server running on Android (default port 43889).
 * Allows desktop companions (KDE Plasmoid) to initiate pairing and bidirectional
 * delta synchronization directly to this mobile device over local Wi-Fi.
 */
@Singleton
class AndroidSyncServer @Inject constructor(
    private val pairedDeviceDao: PairedDeviceDao,
    private val collectionDao: CollectionDao,
    private val bookmarkDao: BookmarkDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val DEFAULT_PORT = 43889
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _actualPort = MutableStateFlow(DEFAULT_PORT)
    val actualPort: StateFlow<Int> = _actualPort.asStateFlow()

    @Synchronized
    fun startServer(
        deviceId: String,
        deviceName: String,
        preferredPort: Int = DEFAULT_PORT
    ) {
        if (_isRunning.value) return

        if (!scope.isActive) {
            scope = CoroutineScope(ioDispatcher + SupervisorJob())
        }

        serverJob = scope.launch {
            try {
                val ss = try {
                    ServerSocket(preferredPort)
                } catch (_: Exception) {
                    ServerSocket(0) // Fallback to an ephemeral available port
                }
                serverSocket = ss
                _actualPort.value = ss.localPort
                _isRunning.value = true

                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch {
                            handleClient(clientSocket, deviceId, deviceName)
                        }
                    } catch (_: Exception) {
                        if (!isActive || ss.isClosed) break
                    }
                }
            } catch (_: Exception) {
                // Server socket error or closed
            } finally {
                _isRunning.value = false
            }
        }
    }

    @Synchronized
    fun stopServer() {
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
    }

    private suspend fun handleClient(
        socket: Socket,
        localDeviceId: String,
        localDeviceName: String
    ) = withContext(ioDispatcher) {
        try {
            socket.soTimeout = 5000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                val colonIdx = line!!.indexOf(":")
                if (colonIdx > 0) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val bodyChars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(bodyChars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                bodyBuilder.append(bodyChars, 0, totalRead)
            }
            val body = bodyBuilder.toString()

            when {
                method.equals("OPTIONS", ignoreCase = true) -> {
                    sendResponse(writer, 200, "{}")
                }
                path == "/api/v1/sync/status" && method.equals("GET", ignoreCase = true) -> {
                    val statusJson = JSONObject().apply {
                        put("status", "ONLINE")
                        put("deviceId", localDeviceId)
                        put("deviceName", localDeviceName)
                        put("protocol", "bookmarks-sync-v1")
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendResponse(writer, 200, statusJson.toString())
                }
                path == "/api/v1/sync/pair" && method.equals("POST", ignoreCase = true) -> {
                    val pairRequest = try {
                        PairRequestDto.fromJson(body)
                    } catch (_: Exception) {
                        PairRequestDto(initiatorDeviceId = "unknown", initiatorName = "Desktop", verificationCode = "")
                    }

                    val authToken = "auth_${UUID.randomUUID()}"
                    val clientIp = socket.inetAddress?.hostAddress ?: "127.0.0.1"

                    val pairedEntity = PairedDeviceEntity(
                        deviceId = pairRequest.initiatorDeviceId.ifBlank { "desktop_${UUID.randomUUID()}" },
                        deviceName = pairRequest.initiatorName.ifBlank { "KDE Desktop Plasmoid" },
                        hostAddress = clientIp,
                        httpPort = 43889,
                        authToken = authToken,
                        lastSyncTimestamp = 0L,
                        isPaired = true
                    )
                    pairedDeviceDao.insertPairedDevice(pairedEntity)

                    val pairResponse = PairResponseDto(
                        status = "PAIRED_SUCCESS",
                        authToken = authToken,
                        responderDeviceId = localDeviceId,
                        responderName = localDeviceName
                    )
                    sendResponse(writer, 200, pairResponse.toJson())
                }
                path == "/api/v1/sync/exchange" && method.equals("POST", ignoreCase = true) -> {
                    val exchangeReq = try {
                        SyncExchangeRequestDto.fromJson(body)
                    } catch (_: Exception) {
                        null
                    }

                    if (exchangeReq == null) {
                        sendResponse(writer, 400, JSONObject().put("error", "Bad Request").toString())
                        return@withContext
                    }

                    // Reconcile incoming delta from desktop
                    val serverTimestamp = System.currentTimeMillis()
                    for (colDto in exchangeReq.collections) {
                        val existing = collectionDao.getCollectionByIdDirect(colDto.id)
                        if (existing == null || colDto.updatedAt >= existing.updatedAt) {
                            collectionDao.insertCollection(colDto.toEntity())
                        }
                    }

                    for (bmDto in exchangeReq.bookmarks) {
                        val existing = bookmarkDao.getBookmarkByIdDirectIncludingDeleted(bmDto.id)
                        if (existing == null || bmDto.updatedAt >= existing.updatedAt) {
                            bookmarkDao.insertBookmark(bmDto.toEntity().copy(syncStatus = "SYNCED"))
                        }
                    }

                    // Gather local modified delta since lastSyncTimestamp
                    val localModifiedCollections = collectionDao.getCollectionsModifiedSince(exchangeReq.lastSyncTimestamp)
                        .map { CollectionSyncDto.fromEntity(it) }
                    val localModifiedBookmarks = bookmarkDao.getBookmarksModifiedSince(exchangeReq.lastSyncTimestamp)
                        .map { BookmarkSyncDto.fromEntity(it) }

                    // Update paired device timestamp if present
                    pairedDeviceDao.updateLastSyncTimestamp(exchangeReq.deviceId, serverTimestamp)

                    val exchangeResp = SyncExchangeResponseDto(
                        status = "SUCCESS",
                        serverTimestamp = serverTimestamp,
                        collections = localModifiedCollections,
                        bookmarks = localModifiedBookmarks
                    )
                    sendResponse(writer, 200, exchangeResp.toJson())
                }
                else -> {
                    sendResponse(writer, 404, JSONObject().put("error", "Not Found").toString())
                }
            }
        } catch (_: Exception) {
            // Socket or client exception
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendResponse(writer: PrintWriter, status: Int, jsonBody: String) {
        val statusMsg = when (status) {
            200 -> "200 OK"
            400 -> "400 Bad Request"
            404 -> "404 Not Found"
            else -> "$status Status"
        }
        val bytes = jsonBody.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $statusMsg\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        writer.print("Access-Control-Allow-Headers: Content-Type, Authorization\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(jsonBody)
        writer.flush()
    }
}
