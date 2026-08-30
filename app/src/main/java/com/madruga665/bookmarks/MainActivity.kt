package com.madruga665.bookmarks

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.repository.AppLanguage
import com.madruga665.bookmarks.data.repository.AppThemeMode
import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.data.repository.SettingsRepository
import com.madruga665.bookmarks.data.repository.SyncRepository
import com.madruga665.bookmarks.ui.home.HomeViewModel
import com.madruga665.bookmarks.ui.navigation.BookmarksNavGraph
import com.madruga665.bookmarks.ui.savemodal.SaveBookmarkViewModel
import com.madruga665.bookmarks.ui.theme.NeobrutalismTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val saveBookmarkViewModel: SaveBookmarkViewModel by viewModels()

    @Inject
    lateinit var collectionRepository: CollectionRepository

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var peerDiscoveryManager: PeerDiscoveryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
            val appLanguage by settingsRepository.language.collectAsState(initial = AppLanguage.SYSTEM)

            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.CATPPUCCIN_MOCHA -> true
                AppThemeMode.SYSTEM -> isSystemDark
            }

            LaunchedEffect(appLanguage) {
                val localeList = when (appLanguage) {
                    AppLanguage.PT_BR -> LocaleListCompat.forLanguageTags("pt-BR")
                    AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
                    AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }

            val context = LocalContext.current
            val baseConfig = LocalConfiguration.current

            val (configuration, localizedContext) = remember(appLanguage, baseConfig, context) {
                val activeLocale = when (appLanguage) {
                    AppLanguage.PT_BR -> Locale("pt", "BR")
                    AppLanguage.EN -> Locale("en")
                    AppLanguage.SYSTEM -> {
                        val sysLocales = ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration)
                        if (!sysLocales.isEmpty) sysLocales[0] ?: Locale.getDefault() else Locale.getDefault()
                    }
                }
                val config = Configuration(baseConfig).apply {
                    setLocale(activeLocale)
                    setLayoutDirection(activeLocale)
                }
                val locContext = context.createConfigurationContext(config)
                Pair(config, locContext)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext
            ) {
                NeobrutalismTheme(darkTheme = darkTheme) {
                    BookmarksNavGraph(
                        homeViewModel = homeViewModel,
                        saveBookmarkViewModel = saveBookmarkViewModel,
                        collectionRepository = collectionRepository,
                        bookmarkRepository = bookmarkRepository,
                        settingsRepository = settingsRepository,
                        syncRepository = syncRepository,
                        peerDiscoveryManager = peerDiscoveryManager
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val extractedUrl = extractUrlFromText(sharedText)
            if (!extractedUrl.isNullOrBlank()) {
                saveBookmarkViewModel.openSaveModal(extractedUrl)
            }
        }
    }

    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val urlRegex = Regex("""(https?://[^\s]+)""", RegexOption.IGNORE_CASE)
        val match = urlRegex.find(text)
        return match?.value ?: if (text.trim().startsWith("http://") || text.trim().startsWith("https://")) text.trim() else null
    }
}
