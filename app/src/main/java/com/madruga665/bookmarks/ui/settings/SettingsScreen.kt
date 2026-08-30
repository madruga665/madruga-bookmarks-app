package com.madruga665.bookmarks.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madruga665.bookmarks.R
import com.madruga665.bookmarks.data.repository.AppLanguage
import com.madruga665.bookmarks.data.repository.AppThemeMode
import com.madruga665.bookmarks.ui.components.NeobrutalistButton
import com.madruga665.bookmarks.ui.settings.components.ActionItemCard
import com.madruga665.bookmarks.ui.settings.components.HapticPreferenceCard
import com.madruga665.bookmarks.ui.settings.components.LanguageSelectionDialog
import com.madruga665.bookmarks.ui.settings.components.PreferenceItemCard
import com.madruga665.bookmarks.ui.settings.components.ThemeSelectionDialog
import com.madruga665.bookmarks.ui.settings.components.UsageHeroCard
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import com.madruga665.bookmarks.ui.theme.neobrutalistGridBackground
import com.madruga665.bookmarks.ui.theme.neobrutalistShadow

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onThemeSelect: (AppThemeMode) -> Unit,
    onLanguageSelect: (AppLanguage) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSyncSettings: () -> Unit = {},
    onExportBackupClick: () -> Unit = {},
    onRestoreBackupClick: () -> Unit = {},
    onImportBookmarksClick: () -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
            // Top App Bar
            SettingsTopBar(
                onBackClick = onBackClick,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeobrutalismTheme.colors.onSurface
                    )
                }
            } else {
                // Top Yellow Hero Usage Card
                UsageHeroCard(
                    usageStatistics = uiState.usageStatistics
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PREFERENCES Section
                SectionHeader(text = stringResource(R.string.section_preferences))

                Spacer(modifier = Modifier.height(8.dp))

                val themeLabel = when (uiState.currentTheme) {
                    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    AppThemeMode.CATPPUCCIN_MOCHA -> stringResource(R.string.theme_dark)
                    AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                }

                PreferenceItemCard(
                    title = stringResource(R.string.pref_theme_title),
                    subtitle = stringResource(R.string.pref_theme_desc),
                    valueText = themeLabel,
                    icon = Icons.Outlined.Palette,
                    iconBackground = NeobrutalismTheme.colors.accentYellow,
                    iconTint = NeobrutalismTheme.colors.border,
                    onClick = { showThemeDialog = true },
                    testTag = "tag_preference_theme"
                )

                Spacer(modifier = Modifier.height(10.dp))

                val langLabel = when (uiState.currentLanguage) {
                    AppLanguage.EN -> stringResource(R.string.lang_en)
                    AppLanguage.PT_BR -> stringResource(R.string.lang_pt_br)
                    AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
                }

                PreferenceItemCard(
                    title = stringResource(R.string.pref_language_title),
                    subtitle = stringResource(R.string.pref_language_desc),
                    valueText = langLabel,
                    icon = Icons.Outlined.Language,
                    iconBackground = NeobrutalismTheme.colors.accentBlue,
                    iconTint = Color.White,
                    onClick = { showLanguageDialog = true },
                    testTag = "tag_preference_language"
                )

                Spacer(modifier = Modifier.height(10.dp))

                HapticPreferenceCard(
                    isEnabled = uiState.isHapticFeedbackEnabled,
                    onToggle = onToggleHapticFeedback,
                    testTag = "tag_preference_haptic"
                )

                Spacer(modifier = Modifier.height(10.dp))

                PreferenceItemCard(
                    title = stringResource(R.string.sync_pref_title),
                    subtitle = stringResource(R.string.sync_pref_desc),
                    valueText = "",
                    icon = Icons.Outlined.DesktopWindows,
                    iconBackground = NeobrutalismTheme.colors.accentPurple,
                    iconTint = Color.White,
                    onClick = onNavigateToSyncSettings,
                    testTag = "tag_preference_sync"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // YOUR DATA Section
                SectionHeader(text = stringResource(R.string.section_your_data))

                Spacer(modifier = Modifier.height(8.dp))

                ActionItemCard(
                    title = stringResource(R.string.action_export_backup),
                    subtitle = stringResource(R.string.action_export_backup_desc),
                    icon = Icons.Outlined.Share,
                    iconBackground = NeobrutalismTheme.colors.accentYellow,
                    iconTint = NeobrutalismTheme.colors.border,
                    onClick = onExportBackupClick,
                    testTag = "tag_action_export_backup"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ActionItemCard(
                    title = stringResource(R.string.action_restore_backup),
                    subtitle = stringResource(R.string.action_restore_backup_desc),
                    icon = Icons.Outlined.CloudDownload,
                    iconBackground = NeobrutalismTheme.colors.accentPurple,
                    iconTint = Color.White,
                    onClick = onRestoreBackupClick,
                    testTag = "tag_action_restore_backup"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // IMPORT FROM OTHER APPS Section
                SectionHeader(text = stringResource(R.string.section_import))

                Spacer(modifier = Modifier.height(8.dp))

                ActionItemCard(
                    title = stringResource(R.string.action_import_bookmarks),
                    subtitle = stringResource(R.string.action_import_bookmarks_desc),
                    icon = Icons.Outlined.MoveToInbox,
                    iconBackground = NeobrutalismTheme.colors.accentOrange,
                    iconTint = Color.White,
                    onClick = onImportBookmarksClick,
                    testTag = "tag_action_import_bookmarks"
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ABOUT Section
                SectionHeader(text = stringResource(R.string.section_about))

                Spacer(modifier = Modifier.height(8.dp))

                AboutCard(appVersion = uiState.appVersion)

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = uiState.currentTheme,
                onThemeSelect = onThemeSelect,
                onDismiss = { showThemeDialog = false }
            )
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = uiState.currentLanguage,
                onLanguageSelect = onLanguageSelect,
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_settings_top_bar"),
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
            modifier = Modifier.testTag("tag_settings_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = NeobrutalismTheme.colors.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = stringResource(R.string.settings_title),
            style = NeobrutalismTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = NeobrutalismTheme.colors.onSurface
        )
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
private fun AboutCard(
    appVersion: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tag_settings_about_card")
            .neobrutalistShadow(
                shadowColor = NeobrutalismTheme.colors.shadow,
                borderColor = NeobrutalismTheme.colors.border,
                borderWidth = 2.5.dp,
                shadowOffset = 4.dp,
                shape = shape
            )
            .background(NeobrutalismTheme.colors.surface, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = NeobrutalismTheme.colors.onSurface,
                    modifier = Modifier.size(22.dp)
                )

                Text(
                    text = stringResource(R.string.app_name),
                    style = NeobrutalismTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = NeobrutalismTheme.colors.onSurface
                )
            }

            Text(
                text = "${stringResource(R.string.about_version)} $appVersion",
                style = NeobrutalismTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = NeobrutalismTheme.colors.subtext
            )
        }
    }
}
