package com.madruga665.bookmarks.ui.settings.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.repository.SyncRepository
import com.madruga665.bookmarks.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing peer discovery, verification code handshakes,
 * manual sync triggers, and paired device lifecycle.
 */
@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val peerDiscoveryManager: PeerDiscoveryManager
) : ViewModel() {

    private val _selectedPeerForPairing = MutableStateFlow<DiscoveredPeer?>(null)
    private val _isPairingInProgress = MutableStateFlow(false)
    private val _pairingError = MutableStateFlow<String?>(null)
    private val _isManualSyncing = MutableStateFlow(false)
    private val _verificationInputCode = MutableStateFlow("")
    private val _activeSyncingDeviceId = MutableStateFlow<String?>(null)

    private val _manualHostInput = MutableStateFlow("")
    private val _manualPortInput = MutableStateFlow("43889")

    val uiState: StateFlow<SyncSettingsUiState> = combine(
        combine(
            syncRepository.syncStatus,
            syncRepository.pairedDevices,
            peerDiscoveryManager.discoveredPeers
        ) { status, paired, discovered ->
            Triple(status, paired, discovered)
        },
        _selectedPeerForPairing,
        _isPairingInProgress,
        _pairingError,
        combine(
            _isManualSyncing,
            _verificationInputCode,
            _activeSyncingDeviceId,
            _manualHostInput,
            _manualPortInput
        ) { isManual, code, activeId, host, port ->
            ManualInputState(isManual, code, activeId, host, port)
        }
    ) { (status, paired, discovered), selectedPeer, isPairing, pairingErr, manualState ->
        SyncSettingsUiState(
            syncStatus = status,
            pairedDevices = paired,
            discoveredPeers = discovered,
            selectedPeerForPairing = selectedPeer,
            isPairingInProgress = isPairing,
            pairingError = pairingErr,
            isManualSyncing = manualState.isManual,
            verificationInputCode = manualState.code,
            activeSyncingDeviceId = manualState.activeId,
            localIpAddress = syncRepository.getLocalIpAddress(),
            manualHostInput = manualState.host,
            manualPortInput = manualState.port
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncSettingsUiState()
    )



    init {
        startDiscovery()
    }

    /**
     * Starts UDP beacon listener and periodic broadcaster for companion desktop discovery.
     */
    fun startDiscovery() {
        peerDiscoveryManager.startDiscovery(
            myDeviceId = syncRepository.localDeviceId,
            myDeviceName = syncRepository.localDeviceName
        )
    }

    /**
     * Stops UDP broadcast and background listening.
     */
    fun stopDiscovery() {
        peerDiscoveryManager.stopDiscovery()
    }

    /**
     * Opens pairing verification prompt for a discovered desktop companion.
     */
    fun initiatePairing(peer: DiscoveredPeer) {
        _selectedPeerForPairing.value = peer
        _pairingError.value = null
        _verificationInputCode.value = ""
    }

    /**
     * Updates current verification code typed in the pairing dialog.
     */
    fun onVerificationCodeChange(code: String) {
        _verificationInputCode.value = code
        if (_pairingError.value != null) {
            _pairingError.value = null
        }
    }

    /**
     * Confirms and sends the pairing verification code to the desktop peer.
     */
    fun confirmPairing(code: String = _verificationInputCode.value) {
        val peer = _selectedPeerForPairing.value ?: return
        if (code.isBlank()) return

        viewModelScope.launch {
            _isPairingInProgress.value = true
            _pairingError.value = null

            val result = syncRepository.pairWithPeer(
                peer = peer,
                verificationCode = code.trim().uppercase()
            )

            _isPairingInProgress.value = false
            if (result.isSuccess) {
                _selectedPeerForPairing.value = null
                _verificationInputCode.value = ""
                _pairingError.value = null
            } else {
                _pairingError.value = result.exceptionOrNull()?.message ?: "Pairing failed. Please verify the 6-digit code."
            }
        }
    }

    /**
     * Dismisses the pairing verification dialog.
     */
    fun dismissPairingDialog() {
        _selectedPeerForPairing.value = null
        _pairingError.value = null
        _verificationInputCode.value = ""
        _isPairingInProgress.value = false
    }

    /**
     * Triggers a manual delta synchronization cycle with all paired devices.
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            _isManualSyncing.value = true
            syncRepository.syncAll()
            _isManualSyncing.value = false
        }
    }

    /**
     * Triggers synchronization with a specific paired device.
     */
    fun syncWithDevice(deviceId: String) {
        viewModelScope.launch {
            _activeSyncingDeviceId.value = deviceId
            _isManualSyncing.value = true
            syncRepository.syncWithPairedDevice(deviceId)
            _isManualSyncing.value = false
            _activeSyncingDeviceId.value = null
        }
    }

    /**
     * Unpairs and deletes authentication token for a companion desktop device.
     */
    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            syncRepository.unpairDevice(deviceId)
        }
    }

    fun onManualHostChange(host: String) {
        _manualHostInput.value = host
    }

    fun onManualPortChange(port: String) {
        _manualPortInput.value = port
    }

    fun pairWithManualHost(host: String = _manualHostInput.value, portStr: String = _manualPortInput.value) {
        val trimmedHost = host.trim()
        if (trimmedHost.isBlank()) return
        val port = portStr.trim().toIntOrNull() ?: 43889

        val peer = DiscoveredPeer(
            deviceId = "manual_${trimmedHost.replace('.', '_')}",
            deviceName = "KDE Desktop ($trimmedHost)",
            hostAddress = trimmedHost,
            httpPort = port,
            deviceType = "desktop",
            lastSeenTimestamp = System.currentTimeMillis()
        )
        initiatePairing(peer)
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}

private data class ManualInputState(
    val isManual: Boolean,
    val code: String,
    val activeId: String?,
    val host: String,
    val port: String
)

