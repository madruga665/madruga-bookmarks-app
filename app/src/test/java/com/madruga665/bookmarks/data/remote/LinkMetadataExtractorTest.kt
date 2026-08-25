package com.madruga665.bookmarks.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkMetadataExtractorTest {

    @Test
    fun extractMetadata_handlesInvalidUrl_returnsFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("not-a-valid-url")
        assertNotNull(result)
        assertNotNull(result.title)
    }

    @Test
    fun extractMetadata_handlesTwitterDomainFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://x.com/devemdobro")
        assertNotNull(result)
        assertEquals("@X", result.sourcePlatform)
        assertTrue(result.title?.contains("devemdobro") == true || result.title?.contains("X") == true)
    }

    @Test
    fun extractMetadata_handlesInstagramFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://www.instagram.com/devemdobro")
        assertNotNull(result)
        assertEquals("@Instagram", result.sourcePlatform)
        assertNotNull(result.thumbnailUrl)
    }

    @Test
    fun extractMetadata_handlesLinkedInFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://www.linkedin.com/in/john-doe")
        assertNotNull(result)
        assertEquals("@LinkedIn", result.sourcePlatform)
        assertTrue(result.title?.contains("LinkedIn") == true || result.title?.contains("john-doe") == true)
    }

    @Test
    fun extractMetadata_handlesGitHubFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://github.com/google/dagger")
        assertNotNull(result)
        assertEquals("@GitHub", result.sourcePlatform)
        assertTrue(result.title?.contains("dagger") == true)
    }

    @Test
    fun extractMetadata_handlesThreadsPostExtraction() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://www.threads.net/@zuck/post/Cx123")
        assertNotNull(result)
        assertEquals("@Threads", result.sourcePlatform)
        assertNotNull(result.faviconUrl)
        assertTrue(result.faviconUrl!!.contains("threads.net/favicon.ico") || result.faviconUrl!!.contains("cdninstagram.com") || result.faviconUrl!!.contains(".svg"))
        assertTrue(result.title?.contains("zuck", ignoreCase = true) == true || result.title?.contains("Threads", ignoreCase = true) == true)
    }

    @Test
    fun extractMetadata_handlesThreadsProfileExtraction() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://threads.net/@devemdobro")
        assertNotNull(result)
        assertEquals("@Threads", result.sourcePlatform)
        assertNotNull(result.faviconUrl)
        assertTrue(result.faviconUrl!!.contains("threads.net/favicon.ico") || result.faviconUrl!!.contains("cdninstagram.com") || result.faviconUrl!!.contains(".svg"))
        assertTrue(result.title?.contains("devemdobro", ignoreCase = true) == true)
    }

    @Test
    fun extractMetadata_handlesThreadsShortlinkFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://www.threads.net/t/Cx123")
        assertNotNull(result)
        assertEquals("@Threads", result.sourcePlatform)
        assertNotNull(result.faviconUrl)
    }

    @Test
    fun extractMetadata_handlesThreadsFallback() = runTest {
        val result = LinkMetadataExtractor.extractMetadata("https://threads.net/invalid/path/offline")
        assertNotNull(result)
        assertEquals("@Threads", result.sourcePlatform)
        assertEquals("https://www.threads.net/favicon.ico", result.faviconUrl)
        assertNotNull(result.title)
    }

    @Test
    fun formatDomainToPlatformName_formatsCorrectly() {
        assertEquals("@X", LinkMetadataExtractor.formatDomainToPlatformName("x.com"))
        assertEquals("@X", LinkMetadataExtractor.formatDomainToPlatformName("twitter.com"))
        assertEquals("@Instagram", LinkMetadataExtractor.formatDomainToPlatformName("instagram.com"))
        assertEquals("@Threads", LinkMetadataExtractor.formatDomainToPlatformName("threads.net"))
        assertEquals("@LinkedIn", LinkMetadataExtractor.formatDomainToPlatformName("linkedin.com"))
        assertEquals("@Facebook", LinkMetadataExtractor.formatDomainToPlatformName("facebook.com"))
        assertEquals("@YouTube", LinkMetadataExtractor.formatDomainToPlatformName("youtube.com"))
        assertEquals("@GitHub", LinkMetadataExtractor.formatDomainToPlatformName("github.com"))
        assertEquals("@Medium", LinkMetadataExtractor.formatDomainToPlatformName("medium.com"))
        assertEquals("@Reddit", LinkMetadataExtractor.formatDomainToPlatformName("reddit.com"))
    }
}
