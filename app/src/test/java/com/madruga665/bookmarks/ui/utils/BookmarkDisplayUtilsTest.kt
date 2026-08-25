package com.madruga665.bookmarks.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkDisplayUtilsTest {

    @Test
    fun getDisplayTitle_withValidTitle_returnsTitle() {
        val title = "Kotlin Coroutines Guide"
        val result = BookmarkDisplayUtils.getDisplayTitle(title, "https://kotlinlang.org/docs/coroutines-overview.html")
        assertEquals("Kotlin Coroutines Guide", result)
    }

    @Test
    fun getDisplayTitle_withNullOrBlankTitle_parsesFromUrl() {
        val resultGithub = BookmarkDisplayUtils.getDisplayTitle(null, "https://github.com/madruga665/bookmarks")
        assertEquals("madruga665/bookmarks: GitHub Repository", resultGithub)

        val resultYoutube = BookmarkDisplayUtils.getDisplayTitle("", "https://www.youtube.com/watch?v=123")
        assertEquals("YouTube Video", resultYoutube)
    }

    @Test
    fun getThumbnailUrl_withThumbnail_returnsThumbnail() {
        val thumb = "https://example.com/image.jpg"
        val result = BookmarkDisplayUtils.getThumbnailUrl(thumb, "https://example.com")
        assertEquals("https://example.com/image.jpg", result)
    }

    @Test
    fun getThumbnailUrl_withNullOrBlank_generatesFallback() {
        val result = BookmarkDisplayUtils.getThumbnailUrl(null, "https://instagram.com/p/123")
        assertTrue(result.isNotBlank())
    }

    @Test
    fun getSourceLabel_withExplicitPlatform_returnsFormattedPlatform() {
        assertEquals("Instagram", BookmarkDisplayUtils.getSourceLabel("instagram", "https://instagram.com"))
        assertEquals("GitHub", BookmarkDisplayUtils.getSourceLabel("github", "https://github.com"))
        assertEquals("X / Twitter", BookmarkDisplayUtils.getSourceLabel("twitter", "https://twitter.com"))
        assertEquals("X / Twitter", BookmarkDisplayUtils.getSourceLabel("x", "https://x.com"))
        assertEquals("Threads", BookmarkDisplayUtils.getSourceLabel("threads", "https://threads.net"))
        assertEquals("Threads", BookmarkDisplayUtils.getSourceLabel("@Threads", "https://threads.net"))
    }

    @Test
    fun getSourceLabel_withNullPlatform_parsesFromDomain() {
        assertEquals("Instagram", BookmarkDisplayUtils.getSourceLabel(null, "https://www.instagram.com/devemdobro"))
        assertEquals("GitHub", BookmarkDisplayUtils.getSourceLabel(null, "https://github.com/torvalds"))
        assertEquals("YouTube", BookmarkDisplayUtils.getSourceLabel(null, "https://www.youtube.com/watch?v=abc"))
        assertEquals("Medium", BookmarkDisplayUtils.getSourceLabel(null, "https://medium.com/@user/story"))
        assertEquals("Threads", BookmarkDisplayUtils.getSourceLabel(null, "https://www.threads.net/@zuck"))
    }

    @Test
    fun getFaviconUrl_withFavicon_returnsFavicon() {
        assertEquals("https://custom.favicon.ico", BookmarkDisplayUtils.getFaviconUrl("https://custom.favicon.ico", "https://google.com"))
    }

    @Test
    fun getFaviconUrl_withNullFavicon_generatesFromDomain() {
        val result = BookmarkDisplayUtils.getFaviconUrl(null, "https://github.com/repo")
        assertNotNull(result)
        assertTrue(result!!.contains("google.com/s2/favicons?domain=github.com"))

        val resultThreads = BookmarkDisplayUtils.getFaviconUrl(null, "https://www.threads.net/@zuck")
        assertEquals("https://www.threads.net/favicon.ico", resultThreads)
    }

    @Test
    fun getCollectionAccentColor_delegatesToCollectionPalette() {
        assertEquals(CollectionPalette.getColor("yellow"), BookmarkDisplayUtils.getCollectionAccentColor("yellow"))
        assertEquals(CollectionPalette.getColor("PURPLE"), BookmarkDisplayUtils.getCollectionAccentColor("PURPLE"))
        assertEquals(CollectionPalette.getColor("#FF4B8B"), BookmarkDisplayUtils.getCollectionAccentColor("#FF4B8B"))
        assertEquals(CollectionPalette.getColor("dark_slate"), BookmarkDisplayUtils.getCollectionAccentColor("dark_slate"))
        assertEquals(CollectionPalette.defaultColor.color, BookmarkDisplayUtils.getCollectionAccentColor(null))
        assertEquals(CollectionPalette.defaultColor.color, BookmarkDisplayUtils.getCollectionAccentColor(""))
        assertEquals(CollectionPalette.defaultColor.color, BookmarkDisplayUtils.getCollectionAccentColor("unknown_non_existent"))
    }
}
