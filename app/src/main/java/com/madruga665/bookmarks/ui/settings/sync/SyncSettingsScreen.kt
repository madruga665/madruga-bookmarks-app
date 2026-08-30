package com.madruga665.bookmarks.ui.settings.sync

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.data.repository.SyncStatus
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.components.NeobrutalistTextField
import com.madruga665.bookmarks.ui.components.SyncStatusBadge
import com.madruga665.bookmarks.ui.settings.sync.components.DiscoveredDeviceCard
import com.madruga665.bookmarks.ui.settings.sync.components.PairVerificationDialog
import com.madruga665.bookmarks.ui.settings.sync.components.PairedDeviceCard
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistGridBackground
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

@Composable
fun SyncSettingsScreen(
    uiState: SyncSettingsUiState,
    onBackClick: () -> Unit,
    onInitiatePairing: (DiscoveredPeer) -> Unit,
    onVerificationCodeChange: (String) -> Unit,
    onConfirmPairing: () -> Unit,
    onDismissPairingDialog: () -> Unit,
    onSyncNow: () -> Unit,
    onSyncDevice: (String) -> Unit,
    onUnpairDevice: (String) -> Unit,
    onRefreshDiscovery: () -> Unit,
    modifier: Modifier = Modifier,
    onManualHostChange: (String) -> Unit = {},
    onManualPortChange: (String) -> Unit = {},
    onPairManualHost: () -> Unit = {}
) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeobrutalismTheme.colors.background)
            .neobrutalistGridBackground(NeobrutalismTheme.colors.gridLine)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            SyncSettingsTopBar(
                onBackClick = onBackClick,
                onRefreshDiscovery = onRefreshDiscovery,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Status & Sync Overview Hero Card
            SyncOverviewHeroCard(
                syncStatus = uiState.syncStatus,
                pairedDeviceCount = uiState.pairedDevices.size,
                isSyncing = uiState.isManualSyncing || uiState.syncStatus == SyncStatus.SYNCING,
                localIpAddress = uiState.localIpAddress,
                onSyncNow = onSyncNow
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PAIRED DEVICES Section
            SectionHeader(text = stringResource(R.string.sync_paired_devices_heading))

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.pairedDevices.isEmpty()) {
                EmptyPairedDevicesCard()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    uiState.pairedDevices.forEach { device ->
                        val isDeviceSyncing = uiState.activeSyncingDeviceId == device.deviceId ||
                            (uiState.isManualSyncing && uiState.syncStatus == SyncStatus.SYNCING)

                        PairedDeviceCard(
                            device = device,
                            onSyncClick = { onSyncDevice(device.deviceId) },
                            onUnpairClick = { onUnpairDevice(device.deviceId) },
                            isSyncing = isDeviceSyncing
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DISCOVERED DEVICES Section
            SectionHeader(text = stringResource(R.string.sync_discovered_devices_heading))

            Spacer(modifier = Modifier.height(8.dp))

            // Filter out peers that are already paired
            val pairedIds = uiState.pairedDevices.map { it.deviceId }.toSet()
            val unpairedPeers = uiState.discoveredPeers.filter { it.deviceId !in pairedIds }

            if (unpairedPeers.isEmpty()) {
                EmptyDiscoveredDevicesCard(
                    onScanClick = onRefreshDiscovery
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    unpairedPeers.forEach { peer ->
                        val isThisPairing = uiState.isPairingInProgress &&
                            uiState.selectedPeerForPairing?.deviceId == peer.deviceId

                        DiscoveredDeviceCard(
                            peer = peer,
                            onPairClick = onInitiatePairing,
                            isPairing = isThisPairing
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DIRECT IP CONNECT Section
            SectionHeader(text = "DIRECT IP CONNECTION")

            Spacer(modifier = Modifier.height(8.dp))

            ManualConnectCard(
                host = uiState.manualHostInput,
                port = uiState.manualPortInput,
                onHostChange = onManualHostChange,
                onPortChange = onManualPortChange,
                onConnectClick = onPairManualHost,
                isPairing = uiState.isPairingInProgress
            )

            Spacer(modifier = Modifier.height(32.dp))

        }

        // Pair Verification Dialog
        PairVerificationDialog(
            isVisible = uiState.selectedPeerForPairing != null,
            targetDeviceName = uiState.selectedPeerForPairing?.deviceName ?: "",
            verificationCode = uiState.verificationInputCode,
            onCodeChange = onVerificationCodeChange,
            onConfirmPair = onConfirmPairing,
            onDismiss = onDismissPairingDialog,
            isPairing = uiState.isPairingInProgress,
            errorMessage = uiState.pairingError
        )
    }
}

@Composable
private fun SyncSettingsTopBar(
    onBackClick: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_sync_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeobrutalistButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                containerColor = NeobrutalismTheme.colors.surface,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.5.dp,
                shadowOffset = 4.dp,
                modifier = Modifier.testTag("tag_sync_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = stringResource(R.string.sync_title),
                style = NeobrutalismTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = NeobrutalismTheme.colors.onSurface
            )
        }

        NeobrutalistButton(
            onClick = onRefreshDiscovery,
            shape = RoundedCornerShape(12.dp),
            containerColor = NeobrutalismTheme.colors.surface,
            borderColor = NeobrutalismTheme.colors.border,
            borderWidth = 2.5.dp,
            shadowOffset = 4.dp,
            modifier = Modifier.testTag("tag_sync_refresh_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.sync_action_scan),
                tint = NeobrutalismTheme.colors.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SyncOverviewHeroCard(
    syncStatus: SyncStatus,
    pairedDeviceCount: Int,
    isSyncing: Boolean,
    localIpAddress: String = "127.0.0.1",
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "heroSyncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heroRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_sync_overview_card")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.5.dp,
                shadowOffset = 4.dp,
                shape = shape
            )
            .background(NeobrutalismTheme.colors.accentYellow, shape)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.sync_title),
                        style = NeobrutalismTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = NeobrutalismTheme.colors.border
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.sync_description),
                        style = NeobrutalismTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = NeobrutalismTheme.colors.border.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📱 This Device: $localIpAddress:43889",
                        style = NeobrutalismTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = NeobrutalismTheme.colors.border
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                SyncStatusBadge(
                    status = syncStatus,
                    onClick = onSyncNow
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button inside Hero
            NeobrutalistButton(
                onClick = onSyncNow,
                enabled = !isSyncing && pairedDeviceCount > 0,
                containerColor = NeobrutalismTheme.colors.surface,
                shape = RoundedCornerShape(10.dp),
                borderWidth = 2.dp,
                shadowOffset = 2.5.dp,
                modifier = Modifier
                    .fillMaxWidth()

                    .testTag("tag_hero_sync_now_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        tint = NeobrutalismTheme.colors.onSurface,
                        modifier = Modifier
                            .size(18.dp)
                            .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                    )

                    Text(
                        text = if (isSyncing) {
                            stringResource(R.string.sync_status_syncing)
                        } else {
                            stringResource(R.string.sync_action_sync_now)
                        },
                        style = NeobrutalismTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = NeobrutalismTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPairedDevicesCard(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_empty_paired_devices")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                shape = shape
            )
            .background(NeobrutalismTheme.colors.surface, shape)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NeobrutalismTheme.colors.accentPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(2.dp, NeobrutalismTheme.colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Devices,
                    contentDescription = null,
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.sync_empty_paired_hint),
                style = NeobrutalismTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = NeobrutalismTheme.colors.subtext,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyDiscoveredDevicesCard(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_empty_discovered_devices")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                shape = shape
            )
            .background(NeobrutalismTheme.colors.surface, shape)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NeobrutalismTheme.colors.accentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(2.dp, NeobrutalismTheme.colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WifiTethering,
                    contentDescription = null,
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.sync_no_devices_found),
                style = NeobrutalismTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = NeobrutalismTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.sync_empty_discovered_hint),
                style = NeobrutalismTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = NeobrutalismTheme.colors.subtext,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            NeobrutalistButton(
                text = stringResource(R.string.sync_action_scan),
                onClick = onScanClick,
                containerColor = NeobrutalismTheme.colors.accentYellow,
                shape = RoundedCornerShape(8.dp),
                borderWidth = 2.dp,
                shadowOffset = 2.dp,
                modifier = Modifier.testTag("tag_scan_now_btn")
            )
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = NeobrutalismTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            fontSize = 12.sp
        ),
        color = NeobrutalismTheme.colors.subtext,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
private fun ManualConnectCard(
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectClick: () -> Unit,
    isPairing: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_manual_connect_card")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                shape = shape
            )
            .background(NeobrutalismTheme.colors.surface, shape)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Connect by IP / Hostname",
                style = NeobrutalismTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = NeobrutalismTheme.colors.onSurface
            )

            Text(
                text = "Use 10.0.2.2 (Android Emulator) or local LAN IP (e.g. 192.168.1.50) to connect directly.",
                style = NeobrutalismTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = NeobrutalismTheme.colors.subtext
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(2f)) {
                    NeobrutalistTextField(
                        value = host,
                        onValueChange = onHostChange,
                        placeholderText = "10.0.2.2 or IP",
                        modifier = Modifier.fillMaxWidth().testTag("tag_manual_host_input")
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    NeobrutalistTextField(
                        value = port,
                        onValueChange = onPortChange,
                        placeholderText = "43889",
                        modifier = Modifier.fillMaxWidth().testTag("tag_manual_port_input")
                    )
                }

            }

            NeobrutalistButton(
                text = if (isPairing) "Connecting..." else "⚡ Connect & Pair",
                onClick = onConnectClick,
                enabled = host.isNotBlank() && !isPairing,
                containerColor = NeobrutalismTheme.colors.accentYellow,
                shape = RoundedCornerShape(10.dp),
                borderWidth = 2.dp,
                shadowOffset = 2.5.dp,
                modifier = Modifier.fillMaxWidth().testTag("tag_manual_connect_btn")
            )
        }
    }
}

