package com.madruga665.bookmarks.ui.settings.sync.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.local.PairedDeviceEntity
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val UnpairRedColor = Color(0xFFFF4B4B)

@Composable
fun PairedDeviceCard(
    device: PairedDeviceEntity,
    onSyncClick: (PairedDeviceEntity) -> Unit,
    onUnpairClick: (PairedDeviceEntity) -> Unit,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val currentLocale = remember(configuration) {
        val locales = configuration.locales
        if (!locales.isEmpty) locales[0] ?: Locale.getDefault() else Locale.getDefault()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pairedDeviceSyncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pairedDeviceRotation"
    )

    val syncButtonText = if (isSyncing) {
        stringResource(R.string.sync_status_syncing)
    } else {
        stringResource(R.string.sync_action_sync_now)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_paired_device_${device.deviceId}")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 3.5.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Section: Icon, Name, Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(NeobrutalismTheme.colors.accentPurple.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .border(2.dp, NeobrutalismTheme.colors.border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DesktopWindows,
                        contentDescription = device.deviceName,
                        tint = NeobrutalismTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.deviceName.ifBlank { "KDE Plasmoid Desktop" },
                        style = NeobrutalismTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = NeobrutalismTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = stringResource(R.string.sync_ip_port_fmt, device.hostAddress, device.httpPort),
                        style = NeobrutalismTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = NeobrutalismTheme.colors.subtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Last Synced Info
            val lastSyncedText = if (device.lastSyncTimestamp > 0L) {
                val formattedTime = formatTimestamp(device.lastSyncTimestamp, currentLocale)
                stringResource(R.string.sync_last_synced_fmt, formattedTime)
            } else {
                stringResource(R.string.sync_last_synced_never)
            }

            Text(
                text = lastSyncedText,
                style = NeobrutalismTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = NeobrutalismTheme.colors.subtext
            )

            Spacer(modifier = Modifier.height(14.dp))

            val syncButtonText = if (isSyncing) {
                stringResource(R.string.sync_status_syncing)
            } else {
                stringResource(R.string.sync_action_sync_now)
            }

            // Actions Row: Unpair & Sync Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unpair Button
                NeobrutalistButton(
                    onClick = { onUnpairClick(device) },
                    containerColor = NeobrutalismTheme.colors.surface,
                    shape = RoundedCornerShape(8.dp),
                    borderWidth = 2.dp,
                    shadowOffset = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tag_unpair_btn_${device.deviceId}")
                ) {
                    Text(
                        text = stringResource(R.string.sync_action_unpair),
                        style = NeobrutalismTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = UnpairRedColor
                    )
                }

                // Sync Now Button
                NeobrutalistButton(
                    onClick = { onSyncClick(device) },
                    enabled = !isSyncing,
                    containerColor = NeobrutalismTheme.colors.accentYellow,
                    shape = RoundedCornerShape(8.dp),
                    borderWidth = 2.dp,
                    shadowOffset = 2.dp,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("tag_sync_now_btn_${device.deviceId}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = NeobrutalismTheme.colors.border,
                            modifier = Modifier
                                .size(16.dp)
                                .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                        )
                        Text(
                            text = syncButtonText,
                            style = NeobrutalismTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = NeobrutalismTheme.colors.border
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long, locale: Locale): String {
    if (timestamp <= 0L) return ""
    return try {
        val sdf = SimpleDateFormat("d MMM yyyy, h:mm a", locale)
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
