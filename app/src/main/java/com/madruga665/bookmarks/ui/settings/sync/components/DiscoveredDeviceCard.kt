package com.madruga665.bookmarks.ui.settings.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.remote.sync.DiscoveredPeer
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

@Composable
fun DiscoveredDeviceCard(
    peer: DiscoveredPeer,
    onPairClick: (DiscoveredPeer) -> Unit,
    modifier: Modifier = Modifier,
    isPairing: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_discovered_device_${peer.deviceId}")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Device Icon Container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeobrutalismTheme.colors.accentBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(2.dp, NeobrutalismTheme.colors.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DesktopWindows,
                        contentDescription = peer.deviceName,
                        tint = NeobrutalismTheme.colors.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = peer.deviceName.ifBlank { "KDE Companion" },
                        style = NeobrutalismTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = NeobrutalismTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = stringResource(R.string.sync_ip_port_fmt, peer.hostAddress, peer.httpPort),
                        style = NeobrutalismTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = NeobrutalismTheme.colors.subtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pair Button
            NeobrutalistButton(
                onClick = { onPairClick(peer) },
                enabled = !isPairing,
                containerColor = NeobrutalismTheme.colors.accentYellow,
                shape = RoundedCornerShape(8.dp),
                borderWidth = 2.dp,
                shadowOffset = 2.dp,
                modifier = Modifier.testTag("tag_pair_device_btn_${peer.deviceId}")
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NeobrutalismTheme.colors.border,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sync_action_pair),
                        style = NeobrutalismTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = NeobrutalismTheme.colors.border
                    )
                }
            }
        }
    }
}
