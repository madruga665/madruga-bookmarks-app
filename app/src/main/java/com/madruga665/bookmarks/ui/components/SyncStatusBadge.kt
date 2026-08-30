package com.madruga665.bookmarks.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.repository.SyncStatus
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

private val SyncedColor = Color(0xFF22C55E)
private val ErrorColor = Color(0xFFFF4B4B)

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncBadgeRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    val (statusColor, statusText) = when (status) {
        SyncStatus.SYNCED -> SyncedColor to stringResource(R.string.sync_status_synced)
        SyncStatus.SYNCING -> NeobrutalismTheme.colors.accentYellow to stringResource(R.string.sync_status_syncing)
        SyncStatus.DISCONNECTED -> NeobrutalismTheme.colors.subtext to stringResource(R.string.sync_status_disconnected)
        SyncStatus.ERROR -> ErrorColor to stringResource(R.string.sync_status_error)
        SyncStatus.IDLE -> NeobrutalismTheme.colors.subtext to stringResource(R.string.sync_status_idle)
    }

    Box(
        modifier = modifier
            .testTag("tag_sync_status_badge")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.dp,
                shadowOffset = 2.5.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .background(NeobrutalismTheme.colors.surface, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (status == SyncStatus.SYNCING) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = statusText,
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(rotation)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                        .border(1.dp, NeobrutalismTheme.colors.border, CircleShape)
                )
            }

            if (showLabel) {
                Text(
                    text = statusText,
                    style = NeobrutalismTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = NeobrutalismTheme.colors.onSurface
                )
            }
        }
    }
}
