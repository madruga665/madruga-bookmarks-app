package com.madruga665.bookmarks.ui.settings.sync

import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.repository.SyncStatus

/**
 * UI State for the KDE Desktop Companion Sync management screen.
 */
data class SyncSettingsUiState(
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val pairedDevices: List<PairedDeviceEntity> = emptyList(),
    val discoveredPeers: List<DiscoveredPeer> = emptyList(),
    val isPairingInProgress: Boolean = false,
    val pairingError: String? = null,
    val selectedPeerForPairing: DiscoveredPeer? = null,
    val isManualSyncing: Boolean = false,
    val verificationInputCode: String = "",
    val activeSyncingDeviceId: String? = null,
    val localIpAddress: String = "127.0.0.1",
    val manualHostInput: String = "",
    val manualPortInput: String = "43889"
)
