package com.madruga665.bookmarks.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.repository.SyncStatus
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.components.SyncStatusBadge
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme

@Composable
fun HomeScreenTopBar(
    syncStatus: SyncStatus = SyncStatus.IDLE,
    onNavigateToSettings: () -> Unit,
    onNavigateToSyncSettings: () -> Unit = {},
    onNavigateToManageCollections: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Settings Action Button & Sync Status Badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistButton(
                onClick = onNavigateToSettings,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("tag_top_bar_settings")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            SyncStatusBadge(
                status = syncStatus,
                onClick = onNavigateToSyncSettings,
                showLabel = true
            )
        }

        // Right: Manage Collections & Search Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeobrutalistButton(
                onClick = onNavigateToManageCollections,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("tag_top_bar_manage_collections")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderSpecial,
                    contentDescription = stringResource(R.string.home_topbar_manage_collections),
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            NeobrutalistButton(
                onClick = onNavigateToSearch,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("tag_top_bar_search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.home_topbar_search),
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
