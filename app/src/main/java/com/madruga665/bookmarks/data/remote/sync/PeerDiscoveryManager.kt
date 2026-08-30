package com.madruga665.bookmarks.data.remote.sync

import com.madruga665.bookmarks.data.remote.sync.dto.DiscoveryBeaconPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discovered peer representation on the local network.
 */
data class DiscoveredPeer(
    val deviceId: String,
    val deviceName: String,
    val hostAddress: String,
    val httpPort: Int,
    val deviceType: String,
    val lastSeenTimestamp: Long
)

/**
 * Manages local Wi-Fi UDP beacon discovery on port 43888.
 */
@Singleton
class PeerDiscoveryManager @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val DISCOVERY_PORT = 43888
        const val DEFAULT_HTTP_PORT = 43889
        const val PROTOCOL_VERSION = "bookmarks-sync-v1"
        const val STALE_TIMEOUT_MS = 15_000L
        const val CLEANUP_INTERVAL_MS = 3_000L
        const val BUFFER_SIZE = 2048
    }

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private var scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var listenJob: Job? = null
    private var periodicJob: Job? = null
    private var socket: DatagramSocket? = null

    private var currentDeviceId: String? = null
    private var currentDeviceName: String? = null

    @Synchronized
    fun startDiscovery(myDeviceId: String, myDeviceName: String) {
        stopDiscovery()

        currentDeviceId = myDeviceId
        currentDeviceName = myDeviceName

        if (!scope.isActive) {
            scope = CoroutineScope(ioDispatcher + SupervisorJob())
        }

        listenJob = scope.launch {
            listenForBeacons(myDeviceId)
        }

        periodicJob = scope.launch {
            broadcastBeacon(myDeviceId, myDeviceName)
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                cleanupStalePeers()
            }
        }
    }

    @Synchronized
    fun stopDiscovery() {
        listenJob?.cancel()
        listenJob = null
        periodicJob?.cancel()
        periodicJob = null

        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null

        scope.coroutineContext.cancelChildren()
    }

    fun broadcastBeacon(
        deviceId: String? = currentDeviceId,
        deviceName: String? = currentDeviceName
    ) {
        val targetDeviceId = deviceId ?: return
        val targetDeviceName = deviceName ?: return

        scope.launch {
            sendBroadcastPacket(targetDeviceId, targetDeviceName)
        }
    }

    suspend fun sendBroadcastPacket(
        deviceId: String,
        deviceName: String,
        targetPort: Int = DISCOVERY_PORT,
        httpPort: Int = DEFAULT_HTTP_PORT
    ) = withContext(ioDispatcher) {
        val payload = DiscoveryBeaconPayload(
            protocol = PROTOCOL_VERSION,
            deviceId = deviceId,
            deviceName = deviceName,
            httpPort = httpPort,
            deviceType = "mobile"
        )
        val data = payload.toJson().toByteArray(Charsets.UTF_8)

        var sendSocket: DatagramSocket? = null
        try {
            sendSocket = DatagramSocket().apply {
                broadcast = true
            }
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(data, data.size, broadcastAddress, targetPort)
            sendSocket.send(packet)
        } catch (_: Exception) {
            // Ignored if network interface does not support broadcast or Wi-Fi is offline
        } finally {
            try {
                sendSocket?.close()
            } catch (_: Exception) {}
        }
    }

    private suspend fun listenForBeacons(myDeviceId: String) = withContext(ioDispatcher) {
        val buffer = ByteArray(BUFFER_SIZE)
        var serverSocket: DatagramSocket? = null
        try {
            serverSocket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 2000
                bind(InetSocketAddress(DISCOVERY_PORT))
            }
            socket = serverSocket

            while (isActive && !serverSocket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    serverSocket.receive(packet)
                    val senderAddress = packet.address?.hostAddress ?: continue
                    val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    processIncomingBeacon(message, senderAddress, myDeviceId)
                } catch (_: java.net.SocketTimeoutException) {
                    // Timeout expected to periodically check isActive
                } catch (e: SocketException) {
                    if (!isActive || serverSocket.isClosed) break
                } catch (_: Exception) {
                    if (!isActive) break
                }
            }
        } catch (_: Exception) {
            // Port unavailable or socket error
        } finally {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            if (socket === serverSocket) {
                socket = null
            }
        }
    }

    fun processIncomingBeacon(
        message: String,
        hostAddress: String,
        myDeviceId: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        return try {
            val payload = DiscoveryBeaconPayload.fromJson(message)

            if (payload.protocol != PROTOCOL_VERSION) {
                return false
            }

            if (payload.deviceId == myDeviceId || payload.deviceId.isBlank()) {
                return false
            }

            val peer = DiscoveredPeer(
                deviceId = payload.deviceId,
                deviceName = payload.deviceName,
                hostAddress = hostAddress,
                httpPort = payload.httpPort,
                deviceType = payload.deviceType,
                lastSeenTimestamp = timestamp
            )

            updatePeer(peer)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    private fun updatePeer(peer: DiscoveredPeer) {
        val current = _discoveredPeers.value.toMutableList()
        val index = current.indexOfFirst { it.deviceId == peer.deviceId }
        if (index >= 0) {
            current[index] = peer
        } else {
            current.add(peer)
        }
        _discoveredPeers.value = current
    }

    @Synchronized
    fun cleanupStalePeers(now: Long = System.currentTimeMillis()) {
        val current = _discoveredPeers.value
        val filtered = current.filter { now - it.lastSeenTimestamp <= STALE_TIMEOUT_MS }
        if (filtered.size != current.size) {
            _discoveredPeers.value = filtered
        }
    }

    @Synchronized
    fun clearPeers() {
        _discoveredPeers.value = emptyList()
    }
}
