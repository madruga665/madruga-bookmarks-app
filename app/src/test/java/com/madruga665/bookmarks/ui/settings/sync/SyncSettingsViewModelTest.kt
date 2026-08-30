package com.madruga665.bookmarks.ui.settings.sync

import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.repository.SyncRepository
import com.madruga665.bookmarks.data.repository.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val peerDiscoveryManager: PeerDiscoveryManager = mockk(relaxed = true)

    private val syncStatusFlow = MutableStateFlow(SyncStatus.IDLE)
    private val pairedDevicesFlow = MutableStateFlow<List<PairedDeviceEntity>>(emptyList())
    private val discoveredPeersFlow = MutableStateFlow<List<DiscoveredPeer>>(emptyList())

    private val samplePeer = DiscoveredPeer(
        deviceId = "desktop-uuid-1",
        deviceName = "Arch Linux Desktop",
        hostAddress = "192.168.1.105",
        httpPort = 43889,
        deviceType = "desktop",
        lastSeenTimestamp = System.currentTimeMillis()
    )

    private val samplePairedDevice = PairedDeviceEntity(
        deviceId = "desktop-uuid-1",
        deviceName = "Arch Linux Desktop",
        hostAddress = "192.168.1.105",
        httpPort = 43889,
        authToken = "test-token-xyz",
        lastSyncTimestamp = 100000L,
        isPaired = true
    )

    private lateinit var viewModel: SyncSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { syncRepository.localDeviceId } returns "mobile-uuid-123"
        every { syncRepository.localDeviceName } returns "Android Pixel Test"
        every { syncRepository.syncStatus } returns syncStatusFlow
        every { syncRepository.pairedDevices } returns pairedDevicesFlow
        every { peerDiscoveryManager.discoveredPeers } returns discoveredPeersFlow

        viewModel = SyncSettingsViewModel(
            syncRepository = syncRepository,
            peerDiscoveryManager = peerDiscoveryManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_startsDiscoveryAutomatically() {
        verify(atLeast = 1) {
            peerDiscoveryManager.startDiscovery("mobile-uuid-123", "Android Pixel Test")
        }
    }

    @Test
    fun uiState_combinesStatusPairedAndDiscoveredFlows() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        syncStatusFlow.value = SyncStatus.SYNCED
        pairedDevicesFlow.value = listOf(samplePairedDevice)
        discoveredPeersFlow.value = listOf(samplePeer)

        val state = viewModel.uiState.value
        assertEquals(SyncStatus.SYNCED, state.syncStatus)
        assertEquals(1, state.pairedDevices.size)
        assertEquals("Arch Linux Desktop", state.pairedDevices[0].deviceName)
        assertEquals(1, state.discoveredPeers.size)
        assertEquals("desktop-uuid-1", state.discoveredPeers[0].deviceId)
        assertNull(state.selectedPeerForPairing)
        assertFalse(state.isPairingInProgress)
        assertNull(state.pairingError)
    }

    @Test
    fun startDiscovery_delegatesToPeerDiscoveryManager() {
        viewModel.startDiscovery()
        verify {
            peerDiscoveryManager.startDiscovery("mobile-uuid-123", "Android Pixel Test")
        }
    }

    @Test
    fun stopDiscovery_delegatesToPeerDiscoveryManager() {
        viewModel.stopDiscovery()
        verify(atLeast = 1) {
            peerDiscoveryManager.stopDiscovery()
        }
    }

    @Test
    fun initiatePairing_setsSelectedPeerAndClearsError() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.initiatePairing(samplePeer)

        val state = viewModel.uiState.value
        assertEquals(samplePeer, state.selectedPeerForPairing)
        assertEquals("", state.verificationInputCode)
        assertNull(state.pairingError)
    }

    @Test
    fun onVerificationCodeChange_updatesCode() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.initiatePairing(samplePeer)
        viewModel.onVerificationCodeChange("123456")

        val state = viewModel.uiState.value
        assertEquals("123456", state.verificationInputCode)
    }

    @Test
    fun confirmPairing_success_clearsDialogAndStoresDevice() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        coEvery {
            syncRepository.pairWithPeer(samplePeer, "654321")
        } returns Result.success(samplePairedDevice)

        viewModel.initiatePairing(samplePeer)
        viewModel.onVerificationCodeChange("654321")
        viewModel.confirmPairing("654321")

        val state = viewModel.uiState.value
        assertFalse(state.isPairingInProgress)
        assertNull(state.selectedPeerForPairing)
        assertNull(state.pairingError)
        assertEquals("", state.verificationInputCode)

        coVerify(exactly = 1) {
            syncRepository.pairWithPeer(samplePeer, "654321")
        }
    }

    @Test
    fun confirmPairing_failure_setsPairingError() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        coEvery {
            syncRepository.pairWithPeer(samplePeer, "000000")
        } returns Result.failure(IOException("Invalid verification code"))

        viewModel.initiatePairing(samplePeer)
        viewModel.onVerificationCodeChange("000000")
        viewModel.confirmPairing("000000")

        val state = viewModel.uiState.value
        assertFalse(state.isPairingInProgress)
        assertNotNull(state.selectedPeerForPairing)
        assertEquals("Invalid verification code", state.pairingError)

        coVerify(exactly = 1) {
            syncRepository.pairWithPeer(samplePeer, "000000")
        }
    }

    @Test
    fun dismissPairingDialog_resetsPairingState() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.initiatePairing(samplePeer)
        viewModel.onVerificationCodeChange("123456")
        viewModel.dismissPairingDialog()

        val state = viewModel.uiState.value
        assertNull(state.selectedPeerForPairing)
        assertNull(state.pairingError)
        assertEquals("", state.verificationInputCode)
        assertFalse(state.isPairingInProgress)
    }

    @Test
    fun triggerManualSync_delegatesToSyncRepository() = runTest(testDispatcher) {
        coEvery { syncRepository.syncAll() } returns Result.success(Unit)

        viewModel.triggerManualSync()

        coVerify(exactly = 1) {
            syncRepository.syncAll()
        }
    }

    @Test
    fun syncWithDevice_delegatesToSyncRepository() = runTest(testDispatcher) {
        coEvery { syncRepository.syncWithPairedDevice("desktop-uuid-1") } returns Result.success(mockk(relaxed = true))

        viewModel.syncWithDevice("desktop-uuid-1")

        coVerify(exactly = 1) {
            syncRepository.syncWithPairedDevice("desktop-uuid-1")
        }
    }

    @Test
    fun unpairDevice_delegatesToSyncRepository() = runTest(testDispatcher) {
        viewModel.unpairDevice("desktop-uuid-1")

        coVerify(exactly = 1) {
            syncRepository.unpairDevice("desktop-uuid-1")
        }
    }
}
