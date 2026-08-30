# Android Mobile Sync Component Interfaces

**Feature**: `015-desktop-companion-sync`
**Date**: 2026-08-30

## 1. Network Discovery Service

```kotlin
interface PeerDiscoveryService {
    val discoveredPeers: StateFlow<List<DiscoveredPeer>>
    fun startDiscovery()
    fun stopDiscovery()
    fun broadcastPresence()
}

data class DiscoveredPeer(
    val deviceId: String,
    val deviceName: String,
    val hostAddress: String,
    val httpPort: Int,
    val deviceType: String,
    val lastSeenTimestamp: Long
)
```

## 2. Sync HTTP Client

```kotlin
interface SyncHttpClient {
    suspend fun pair(
        hostAddress: String,
        httpPort: Int,
        initiatorDeviceId: String,
        initiatorName: String,
        verificationCode: String
    ): Result<PairResponseDto>

    suspend fun exchange(
        hostAddress: String,
        httpPort: Int,
        authToken: String,
        payload: SyncExchangeRequestDto
    ): Result<SyncExchangeResponseDto>
}
```

## 3. Synchronization Repository / Engine

```kotlin
interface SyncRepository {
    val syncStatus: StateFlow<SyncStatus>
    val pairedDevices: Flow<List<PairedDeviceEntity>>
    
    suspend fun pairWithPeer(peer: DiscoveredPeer, verificationCode: String): Result<Unit>
    suspend fun syncWithPairedDevice(deviceId: String): Result<SyncSummary>
    suspend fun syncAll(): Result<Unit>
    suspend fun unpairDevice(deviceId: String)
}

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
    DISCONNECTED
}
```
