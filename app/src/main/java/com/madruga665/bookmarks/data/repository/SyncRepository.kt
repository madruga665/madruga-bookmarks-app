package com.madruga665.bookmarks.data.repository

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.BookmarkEntity
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.CollectionEntity
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.AndroidSyncServer
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.remote.sync.SyncHttpClient
import com.madruga665.bookmarks.data.remote.sync.dto.BookmarkSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.CollectionSyncDto
import com.madruga665.bookmarks.data.remote.sync.dto.SyncExchangeRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronization state indicators matching Neobrutalism UI specifications.
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
    DISCONNECTED
}

/**
 * Summary of a completed synchronization cycle.
 */
data class SyncSummary(
    val collectionsSent: Int = 0,
    val bookmarksSent: Int = 0,
    val collectionsReceived: Int = 0,
    val bookmarksReceived: Int = 0,
    val serverTimestamp: Long = 0L
)

/**
 * Core engine for local Wi-Fi synchronization with companion desktop devices.
 * Handles device pairing, delta gathering, exchange transmission,
 * and deterministic Last-Write-Wins conflict resolution.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val pairedDeviceDao: PairedDeviceDao,
    private val collectionDao: CollectionDao,
    private val bookmarkDao: BookmarkDao,
    private val syncHttpClient: SyncHttpClient,
    private val peerDiscoveryManager: PeerDiscoveryManager,
    private val androidSyncServer: AndroidSyncServer? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val localDeviceId: String = UUID.randomUUID().toString(),
    val localDeviceName: String = "Android Device"
) {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val pairedDevices: Flow<List<PairedDeviceEntity>> = pairedDeviceDao.getAllPairedDevices()

    init {
        androidSyncServer?.startServer(localDeviceId, localDeviceName)
        startAutoSync()
    }

    fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    /**
     * Executes the pairing handshake with a discovered peer, persists the paired device,
     * and immediately schedules an initial synchronization exchange.
     */
    suspend fun pairWithPeer(
        peer: DiscoveredPeer,
        verificationCode: String
    ): Result<PairedDeviceEntity> = withContext(ioDispatcher) {
        _syncStatus.value = SyncStatus.SYNCING

        val pairResult = syncHttpClient.pair(
            hostAddress = peer.hostAddress,
            httpPort = peer.httpPort,
            initiatorDeviceId = localDeviceId,
            initiatorName = localDeviceName,
            verificationCode = verificationCode
        )

        if (pairResult.isFailure) {
            val error = pairResult.exceptionOrNull() ?: IOException("Pairing handshake failed")
            handleSyncError(error)
            return@withContext Result.failure(error)
        }

        val response = pairResult.getOrThrow()
        if (response.status != "PAIRED_SUCCESS") {
            val error = IllegalStateException("Pairing rejected with status: ${response.status}")
            handleSyncError(error)
            return@withContext Result.failure(error)
        }

        val targetDeviceId = response.responderDeviceId.ifBlank { peer.deviceId }
        val targetDeviceName = response.responderName.ifBlank { peer.deviceName }

        val pairedDevice = PairedDeviceEntity(
            deviceId = targetDeviceId,
            deviceName = targetDeviceName,
            hostAddress = peer.hostAddress,
            httpPort = peer.httpPort,
            authToken = response.authToken,
            lastSyncTimestamp = 0L,
            isPaired = true
        )

        pairedDeviceDao.insertPairedDevice(pairedDevice)

        // Trigger initial synchronization exchange
        val syncResult = syncWithPairedDevice(pairedDevice.deviceId)
        if (syncResult.isFailure) {
            // Pairing was stored, but initial sync had issues
            return@withContext Result.success(pairedDevice)
        }

        Result.success(pairedDevice)
    }

    /**
     * Executes delta synchronization with a specific paired device.
     * 1. Gathers local deltas (updatedAt > lastSyncTimestamp).
     * 2. Transmits payload via POST /api/v1/sync/exchange.
     * 3. Reconciles incoming deltas using Last-Write-Wins and soft-delete tombstones.
     * 4. Updates lastSyncTimestamp and syncStatus.
     */
    suspend fun syncWithPairedDevice(deviceId: String): Result<SyncSummary> = withContext(ioDispatcher) {
        val device = pairedDeviceDao.getPairedDeviceByIdDirect(deviceId)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Paired device not found or inactive: $deviceId")
            )

        if (!device.isPaired) {
            return@withContext Result.failure(
                IllegalStateException("Device is unpaired: $deviceId")
            )
        }

        // Check if discovered peer has a refreshed IP / port
        val livePeer = peerDiscoveryManager.discoveredPeers.value.firstOrNull { it.deviceId == deviceId }
        val hostAddress = livePeer?.hostAddress ?: device.hostAddress
        val httpPort = livePeer?.httpPort ?: device.httpPort

        if (livePeer != null && (livePeer.hostAddress != device.hostAddress || livePeer.httpPort != device.httpPort)) {
            pairedDeviceDao.updateDeviceAddress(deviceId, livePeer.hostAddress, livePeer.httpPort)
        }

        _syncStatus.value = SyncStatus.SYNCING

        // 1. Gather local deltas
        val localModifiedCollections = collectionDao.getCollectionsModifiedSince(device.lastSyncTimestamp)
        val localModifiedBookmarks = bookmarkDao.getBookmarksModifiedSince(device.lastSyncTimestamp)

        val collectionDtos = localModifiedCollections.map { col ->
            CollectionSyncDto(
                id = col.id,
                name = col.name,
                parentId = col.parentId,
                colorAccent = col.colorAccent,
                iconKey = col.iconKey,
                createdAt = col.createdAt,
                updatedAt = col.updatedAt,
                isDeleted = col.isDeleted
            )
        }

        val bookmarkDtos = localModifiedBookmarks.map { bm ->
            BookmarkSyncDto(
                id = bm.id,
                url = bm.url,
                title = bm.title,
                description = bm.description,
                faviconUrl = bm.faviconUrl,
                thumbnailUrl = bm.thumbnailUrl,
                sourcePlatform = bm.sourcePlatform,
                collectionId = bm.collectionId,
                notes = bm.notes,
                tags = bm.tags,
                isPinned = bm.isPinned,
                createdAt = bm.createdAt,
                updatedAt = bm.updatedAt,
                isDeleted = bm.isDeleted
            )
        }

        val requestPayload = SyncExchangeRequestDto(
            deviceId = localDeviceId,
            lastSyncTimestamp = device.lastSyncTimestamp,
            collections = collectionDtos,
            bookmarks = bookmarkDtos
        )

        // 2. Transmit exchange request
        val exchangeResult = syncHttpClient.exchange(
            hostAddress = hostAddress,
            httpPort = httpPort,
            authToken = device.authToken,
            payload = requestPayload
        )

        if (exchangeResult.isFailure) {
            val error = exchangeResult.exceptionOrNull() ?: IOException("Sync exchange request failed")
            handleSyncError(error)
            return@withContext Result.failure(error)
        }

        val response = exchangeResult.getOrThrow()
        if (response.status != "SUCCESS") {
            val error = IllegalStateException("Sync exchange rejected with status: ${response.status}")
            handleSyncError(error)
            return@withContext Result.failure(error)
        }

        // 3. Reconcile received collections using Last-Write-Wins
        var collectionsApplied = 0
        for (remoteCol in response.collections) {
            val localCol = collectionDao.getCollectionByIdDirect(remoteCol.id)
            if (localCol == null || remoteCol.updatedAt >= localCol.updatedAt) {
                val entityToSave = CollectionEntity(
                    id = remoteCol.id,
                    name = remoteCol.name,
                    linkCount = localCol?.linkCount ?: 0,
                    subcollectionCount = localCol?.subcollectionCount ?: 0,
                    parentId = remoteCol.parentId,
                    iconKey = remoteCol.iconKey,
                    colorAccent = remoteCol.colorAccent,
                    createdAt = remoteCol.createdAt,
                    updatedAt = remoteCol.updatedAt,
                    isDeleted = remoteCol.isDeleted
                )
                collectionDao.insertCollection(entityToSave)
                collectionsApplied++
            }
        }

        // 4. Reconcile received bookmarks using Last-Write-Wins
        var bookmarksApplied = 0
        for (remoteBm in response.bookmarks) {
            val localBm = bookmarkDao.getBookmarkByIdDirectIncludingDeleted(remoteBm.id)
            if (localBm == null || remoteBm.updatedAt >= localBm.updatedAt) {
                val entityToSave = BookmarkEntity(
                    id = remoteBm.id,
                    url = remoteBm.url,
                    title = remoteBm.title,
                    description = remoteBm.description,
                    faviconUrl = remoteBm.faviconUrl,
                    thumbnailUrl = remoteBm.thumbnailUrl,
                    sourcePlatform = remoteBm.sourcePlatform,
                    collectionId = remoteBm.collectionId,
                    notes = remoteBm.notes,
                    tags = remoteBm.tags,
                    isPinned = remoteBm.isPinned,
                    createdAt = remoteBm.createdAt,
                    updatedAt = remoteBm.updatedAt,
                    syncStatus = "SYNCED",
                    isDeleted = remoteBm.isDeleted
                )
                bookmarkDao.insertBookmark(entityToSave)
                bookmarksApplied++
            }
        }

        // 5. Mark local modified bookmarks as synced
        if (localModifiedBookmarks.isNotEmpty()) {
            val sentIds = localModifiedBookmarks.map { it.id }
            bookmarkDao.markBookmarksSynced(sentIds)
        }

        // 6. Update lastSyncTimestamp in PairedDeviceDao
        val newTimestamp = if (response.serverTimestamp > 0L) {
            response.serverTimestamp
        } else {
            System.currentTimeMillis()
        }
        pairedDeviceDao.updateLastSyncTimestamp(deviceId, newTimestamp)

        _syncStatus.value = SyncStatus.SYNCED

        val summary = SyncSummary(
            collectionsSent = collectionDtos.size,
            bookmarksSent = bookmarkDtos.size,
            collectionsReceived = collectionsApplied,
            bookmarksReceived = bookmarksApplied,
            serverTimestamp = newTimestamp
        )

        Result.success(summary)
    }

    /**
     * Executes synchronization with all active paired devices.
     */
    suspend fun syncAll(): Result<Unit> = withContext(ioDispatcher) {
        val pairedList = pairedDeviceDao.getAllPairedDevicesList()
        if (pairedList.isEmpty()) {
            _syncStatus.value = SyncStatus.IDLE
            return@withContext Result.success(Unit)
        }

        var lastError: Throwable? = null
        var anySuccess = false

        for (device in pairedList) {
            val result = syncWithPairedDevice(device.deviceId)
            if (result.isSuccess) {
                anySuccess = true
            } else {
                lastError = result.exceptionOrNull()
            }
        }

        if (anySuccess && lastError == null) {
            _syncStatus.value = SyncStatus.SYNCED
            Result.success(Unit)
        } else if (anySuccess) {
            Result.failure(lastError ?: IOException("Partial sync failure across paired devices"))
        } else {
            val error = lastError ?: IOException("Failed to sync with paired devices")
            handleSyncError(error)
            Result.failure(error)
        }
    }

    /**
     * Unpairs and removes a paired desktop device.
     */
    suspend fun unpairDevice(deviceId: String) = withContext(ioDispatcher) {
        pairedDeviceDao.unpairDevice(deviceId)
        pairedDeviceDao.deletePairedDevice(deviceId)

        val remaining = pairedDeviceDao.getAllPairedDevicesList()
        if (remaining.isEmpty()) {
            _syncStatus.value = SyncStatus.IDLE
        }
    }

    /**
     * Sets sync status manually (e.g. for testing or UI resets).
     */
    fun setSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
    }

    /**
     * Resets sync status to IDLE.
     */
    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }

    /**
     * Background observer that triggers auto-sync whenever a paired peer is rediscovered.
     */
    fun startAutoSync(scope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())) {
        scope.launch {
            peerDiscoveryManager.discoveredPeers.collect { peers ->
                if (peers.isNotEmpty()) {
                    val paired = pairedDeviceDao.getAllPairedDevicesList()
                    for (peer in peers) {
                        val match = paired.firstOrNull { it.deviceId == peer.deviceId }
                        if (match != null) {
                            syncWithPairedDevice(match.deviceId)
                        }
                    }
                }
            }
        }
    }

    private fun handleSyncError(e: Throwable) {
        when (e) {
            is ConnectException, is UnknownHostException, is SocketTimeoutException -> {
                _syncStatus.value = SyncStatus.DISCONNECTED
            }
            is IOException -> {
                val msg = e.message.orEmpty().lowercase()
                if (msg.contains("failed to connect") || msg.contains("unreachable") || msg.contains("timeout") || msg.contains("connection refused")) {
                    _syncStatus.value = SyncStatus.DISCONNECTED
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            }
            else -> {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }
}
