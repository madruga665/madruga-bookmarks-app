package com.madruga665.bookmarks.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

data class LinkMetadata(
    val title: String?,
    val faviconUrl: String?,
    val thumbnailUrl: String?,
    val sourcePlatform: String?,
    val description: String? = null
)

object LinkMetadataExtractor {

    private const val BOT_USER_AGENT =
        "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
    private const val TWITTER_BOT_USER_AGENT =
        "Twitterbot/1.0"
    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun extractMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url

        val domain = try {
            val uri = URI(cleanUrl)
            uri.host?.removePrefix("www.")?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        when {
            domain.contains("twitter.com") || domain.contains("x.com") -> {
                extractTwitterMetadata(cleanUrl, domain)
            }
            domain.contains("threads.net") || domain.contains("threads") -> {
                extractThreadsMetadata(cleanUrl, domain)
            }
            domain.contains("instagram.com") -> {
                extractInstagramMetadata(cleanUrl)
            }
            domain.contains("linkedin.com") -> {
                extractLinkedInMetadata(cleanUrl)
            }
            domain.contains("facebook.com") || domain.contains("fb.watch") -> {
                extractFacebookMetadata(cleanUrl)
            }
            domain.contains("youtube.com") || domain.contains("youtu.be") -> {
                extractYouTubeMetadata(cleanUrl)
            }
            domain.contains("github.com") -> {
                extractGitHubMetadata(cleanUrl)
            }
            else -> {
                extractGenericMetadata(cleanUrl, domain)
            }
        }
    }

    // ==========================================
    // 1. Twitter / X Extraction
    // ==========================================
    private suspend fun extractTwitterMetadata(url: String, domain: String): LinkMetadata = withContext(Dispatchers.IO) {
        val statusMatch = Regex("""(?:twitter\.com|x\.com)/([^/]+)/status/(\d+)""").find(url)
        val defaultFavicon = "https://abs.twimg.com/favicons/twitter.3.ico"
        val platformBadge = if (domain.contains("x.com")) "@X" else "@Twitter"

        if (statusMatch != null) {
            val username = statusMatch.groupValues[1]
            val statusId = statusMatch.groupValues[2]

            // Strategy A: FxTwitter JSON API (Fast, full text, media thumbnails, author avatar)
            try {
                val apiUrl = "https://api.fxtwitter.com/$username/status/$statusId"
                val jsonResponse = httpGet(apiUrl, timeoutMs = 4000)
                if (!jsonResponse.isNullOrBlank()) {
                    val root = JSONObject(jsonResponse)
                    if (root.optInt("code", 0) == 200 && root.has("tweet")) {
                        val tweet = root.getJSONObject("tweet")
                        val text = tweet.optString("text", "")
                        val author = tweet.optJSONObject("author")
                        val authorName = author?.optString("name", username) ?: username
                        val screenName = author?.optString("screen_name", username) ?: username
                        val avatarUrl = author?.optString("avatar_url", defaultFavicon) ?: defaultFavicon

                        val media = tweet.optJSONObject("media")
                        val photos = media?.optJSONArray("photos")
                        val videos = media?.optJSONArray("videos")
                        val mosaic = media?.optJSONObject("mosaic")

                        val thumbnailUrl = when {
                            photos != null && photos.length() > 0 ->
                                photos.getJSONObject(0).optString("url")
                            videos != null && videos.length() > 0 ->
                                videos.getJSONObject(0).optString("thumbnail_url")
                            mosaic != null ->
                                mosaic.optJSONObject("formats")?.optString("jpeg")
                            else -> null
                        }

                        val title = "$authorName (@$screenName) on X"

                        return@withContext LinkMetadata(
                            title = title,
                            faviconUrl = avatarUrl,
                            thumbnailUrl = thumbnailUrl,
                            sourcePlatform = platformBadge,
                            description = text.takeIf { it.isNotBlank() }
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback to oEmbed if FxTwitter is unreachable
            }

            // Strategy B: Official Twitter oEmbed API
            try {
                val oEmbedUrl = "https://publish.twitter.com/oembed?url=" + URLEncoder.encode(url, "UTF-8") + "&omit_script=true"
                val oEmbedResponse = httpGet(oEmbedUrl, timeoutMs = 3000)
                if (!oEmbedResponse.isNullOrBlank()) {
                    val oEmbed = JSONObject(oEmbedResponse)
                    val authorName = oEmbed.optString("author_name", username)
                    val rawHtml = oEmbed.optString("html", "")
                    val doc = Jsoup.parse(rawHtml)
                    val tweetText = doc.select("p").text()

                    return@withContext LinkMetadata(
                        title = "$authorName on X",
                        faviconUrl = defaultFavicon,
                        thumbnailUrl = null,
                        sourcePlatform = platformBadge,
                        description = tweetText.takeIf { it.isNotBlank() }
                    )
                }
            } catch (e: Exception) {
                // Fallback to bot scraping
            }
        }

        // Strategy C: Bot User-Agent OpenGraph Scraping
        val scraped = extractGenericMetadata(url, domain, userAgent = TWITTER_BOT_USER_AGENT)
        if (!scraped.title.isNullOrBlank() && !scraped.title.contains("JavaScript", ignoreCase = true)) {
            scraped.copy(sourcePlatform = platformBadge)
        } else {
            val user = statusMatch?.groupValues?.getOrNull(1) ?: "X"
            LinkMetadata(
                title = "$user on X",
                faviconUrl = defaultFavicon,
                thumbnailUrl = null,
                sourcePlatform = platformBadge,
                description = null
            )
        }
    }

    // ==========================================
    // 2. Threads Extraction
    // ==========================================
    private suspend fun extractThreadsMetadata(url: String, domain: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.threads.net/favicon.ico"
        val fallbackThumbnail = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"

        val uri = try { URI(url) } catch (e: Exception) { null }
        val pathParts = uri?.path?.split("/")?.filter { it.isNotBlank() } ?: emptyList()

        val rawUsername = pathParts.firstOrNull { it.startsWith("@") }
            ?: pathParts.firstOrNull()?.takeIf { it != "t" && it != "post" && !it.startsWith("?") }
        val username = rawUsername?.removePrefix("@")

        val postIndex = pathParts.indexOfFirst { it == "post" || it == "t" }
        val postId = if (postIndex != -1 && postIndex + 1 < pathParts.size) {
            pathParts[postIndex + 1]
        } else null

        // Strategy A: Social bot OpenGraph scraping
        val scraped = extractGenericMetadata(url, domain, userAgent = BOT_USER_AGENT)
        val isLoginWall = scraped.title?.let { t ->
            t.contains("log in", ignoreCase = true) ||
            t.contains("sign up", ignoreCase = true) ||
            t.contains("entrar", ignoreCase = true) ||
            t.contains("cadastrar", ignoreCase = true) ||
            t.contains("login", ignoreCase = true)
        } == true

        val isGenericTitle = scraped.title == null ||
            scraped.title.isBlank() ||
            scraped.title.equals("@Threads", ignoreCase = true) ||
            scraped.title.equals("Threads", ignoreCase = true)

        val resolvedFavicon = scraped.faviconUrl?.takeIf {
            it.isNotBlank() && !it.contains("google.com/s2")
        } ?: defaultFavicon

        if (!isLoginWall && !isGenericTitle && !scraped.title.isNullOrBlank()) {
            LinkMetadata(
                title = scraped.title,
                faviconUrl = resolvedFavicon,
                thumbnailUrl = scraped.thumbnailUrl ?: fallbackThumbnail,
                sourcePlatform = "@Threads",
                description = scraped.description
            )
        } else {
            // Strategy B (Fallback)
            val fallbackTitle = when {
                !username.isNullOrBlank() -> "@$username on Threads"
                postId != null || pathParts.contains("t") -> "Threads Post"
                else -> "Threads"
            }
            LinkMetadata(
                title = fallbackTitle,
                faviconUrl = defaultFavicon,
                thumbnailUrl = fallbackThumbnail,
                sourcePlatform = "@Threads",
                description = null
            )
        }
    }

    // ==========================================
    // 3. Instagram Extraction
    // ==========================================
    private suspend fun extractInstagramMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.google.com/s2/favicons?domain=instagram.com&sz=128"
        val fallbackThumbnail = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"

        val uri = try { URI(url) } catch (e: Exception) { null }
        val pathParts = uri?.path?.split("/")?.filter { it.isNotBlank() } ?: emptyList()
        val shortcode = pathParts.getOrNull(1)?.takeIf { pathParts.firstOrNull() in listOf("p", "reel", "tv") }
        val rawUsername = if (shortcode == null) pathParts.firstOrNull() else null

        // Strategy A: Public Embed Endpoint for Posts/Reels
        if (shortcode != null) {
            try {
                val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
                val doc = Jsoup.connect(embedUrl)
                    .userAgent(BOT_USER_AGENT)
                    .timeout(5000)
                    .get()

                val caption = doc.select(".Caption, .CaptionText").text().takeIf { it.isNotBlank() }
                val author = doc.select(".UsernameText, .Avatar").text().takeIf { it.isNotBlank() }
                val imgSrc = doc.select("img.EmbeddedMediaImage, .EmbeddedMediaImage img").attr("abs:src").takeIf { it.isNotBlank() }

                val title = if (!author.isNullOrBlank()) {
                    "$author on Instagram"
                } else {
                    "Instagram Post"
                }

                return@withContext LinkMetadata(
                    title = title,
                    faviconUrl = defaultFavicon,
                    thumbnailUrl = imgSrc ?: fallbackThumbnail,
                    sourcePlatform = "@Instagram",
                    description = caption
                )
            } catch (e: Exception) {
                // Fallback to bot scraper
            }
        }

        // Strategy B: Bot User-Agent OpenGraph Scraping
        val scraped = extractGenericMetadata(url, "instagram.com", userAgent = BOT_USER_AGENT)
        val isLoginWall = scraped.title?.let { t ->
            t.contains("log in", ignoreCase = true) ||
            t.contains("sign up", ignoreCase = true) ||
            t.contains("entrar", ignoreCase = true) ||
            t.contains("cadastrar", ignoreCase = true) ||
            t.contains("login", ignoreCase = true)
        } == true

        if (!isLoginWall && !scraped.title.isNullOrBlank()) {
            LinkMetadata(
                title = scraped.title,
                faviconUrl = scraped.faviconUrl ?: defaultFavicon,
                thumbnailUrl = scraped.thumbnailUrl ?: fallbackThumbnail,
                sourcePlatform = "@Instagram",
                description = scraped.description
            )
        } else {
            // Strategy C: Structural Fallback
            val title = when {
                shortcode != null -> "Instagram Post"
                !rawUsername.isNullOrBlank() -> "$rawUsername on Instagram"
                else -> "Instagram"
            }
            LinkMetadata(
                title = title,
                faviconUrl = defaultFavicon,
                thumbnailUrl = fallbackThumbnail,
                sourcePlatform = "@Instagram",
                description = null
            )
        }
    }

    // ==========================================
    // 3. LinkedIn Extraction
    // ==========================================
    private suspend fun extractLinkedInMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.google.com/s2/favicons?domain=linkedin.com&sz=128"

        // Strategy A: Bot User-Agent (LinkedIn serves OpenGraph tags to social bots)
        val scraped = extractGenericMetadata(
            url = url,
            domain = "linkedin.com",
            userAgent = "LinkedInBot/1.0 (compatible; Mozilla/5.0; Apache-HttpClient +http://www.linkedin.com)"
        )

        val isLoginWall = scraped.title?.let { t ->
            t.contains("log in", ignoreCase = true) ||
            t.contains("sign up", ignoreCase = true) ||
            t.contains("entrar", ignoreCase = true) ||
            t.contains("cadastrar", ignoreCase = true) ||
            t.contains("iniciar", ignoreCase = true) ||
            t.contains("authwall", ignoreCase = true) ||
            t.contains("security check", ignoreCase = true)
        } == true

        if (!isLoginWall && !scraped.title.isNullOrBlank()) {
            LinkMetadata(
                title = scraped.title,
                faviconUrl = scraped.faviconUrl ?: defaultFavicon,
                thumbnailUrl = scraped.thumbnailUrl,
                sourcePlatform = "@LinkedIn",
                description = scraped.description
            )
        } else {
            // Strategy B: Smart URL Parsing Fallback
            val path = try { URI(url).path } catch (e: Exception) { "" }
            val pathParts = path.split("/").filter { it.isNotBlank() }
            val title = when {
                pathParts.contains("in") -> {
                    val user = pathParts.getOrNull(pathParts.indexOf("in") + 1) ?: "Profile"
                    "$user | LinkedIn"
                }
                pathParts.contains("company") -> {
                    val comp = pathParts.getOrNull(pathParts.indexOf("company") + 1) ?: "Company"
                    "$comp | LinkedIn"
                }
                pathParts.contains("posts") || pathParts.contains("activity") -> "LinkedIn Post"
                pathParts.contains("pulse") -> {
                    val slug = pathParts.getOrNull(pathParts.indexOf("pulse") + 1)?.replace("-", " ") ?: "Article"
                    "$slug | LinkedIn Article"
                }
                else -> "LinkedIn"
            }

            LinkMetadata(
                title = title,
                faviconUrl = defaultFavicon,
                thumbnailUrl = null,
                sourcePlatform = "@LinkedIn",
                description = null
            )
        }
    }

    // ==========================================
    // 4. Facebook Extraction
    // ==========================================
    private suspend fun extractFacebookMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.google.com/s2/favicons?domain=facebook.com&sz=128"

        // Strategy A: Bot User-Agent
        val scraped = extractGenericMetadata(url, "facebook.com", userAgent = BOT_USER_AGENT)
        val isLoginWall = scraped.title?.let { t ->
            t.contains("log in", ignoreCase = true) ||
            t.contains("sign up", ignoreCase = true) ||
            t.contains("entrar", ignoreCase = true) ||
            t.contains("cadastrar", ignoreCase = true) ||
            t.contains("login", ignoreCase = true)
        } == true

        if (!isLoginWall && !scraped.title.isNullOrBlank()) {
            LinkMetadata(
                title = scraped.title,
                faviconUrl = scraped.faviconUrl ?: defaultFavicon,
                thumbnailUrl = scraped.thumbnailUrl,
                sourcePlatform = "@Facebook",
                description = scraped.description
            )
        } else {
            val path = try { URI(url).path } catch (e: Exception) { "" }
            val user = path.split("/").firstOrNull { it.isNotBlank() } ?: "Facebook"
            LinkMetadata(
                title = "$user on Facebook",
                faviconUrl = defaultFavicon,
                thumbnailUrl = null,
                sourcePlatform = "@Facebook",
                description = null
            )
        }
    }

    // ==========================================
    // 5. YouTube Extraction
    // ==========================================
    private suspend fun extractYouTubeMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.google.com/s2/favicons?domain=youtube.com&sz=128"

        // Strategy A: YouTube oEmbed API
        try {
            val oEmbedUrl = "https://www.youtube.com/oembed?url=" + URLEncoder.encode(url, "UTF-8") + "&format=json"
            val jsonStr = httpGet(oEmbedUrl, timeoutMs = 4000)
            if (!jsonStr.isNullOrBlank()) {
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "YouTube Video")
                val author = json.optString("author_name", "")
                val thumb = json.optString("thumbnail_url", "")

                val displayTitle = if (author.isNotBlank()) "$title - $author" else title
                return@withContext LinkMetadata(
                    title = displayTitle,
                    faviconUrl = defaultFavicon,
                    thumbnailUrl = thumb.takeIf { it.isNotBlank() },
                    sourcePlatform = "@YouTube",
                    description = null
                )
            }
        } catch (e: Exception) {
            // Fallback to generic extractor
        }

        extractGenericMetadata(url, "youtube.com")
    }

    // ==========================================
    // 6. GitHub Extraction
    // ==========================================
    private suspend fun extractGitHubMetadata(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = "https://www.google.com/s2/favicons?domain=github.com&sz=128"
        val path = try { URI(url).path } catch (e: Exception) { "" }
        val parts = path.split("/").filter { it.isNotBlank() }

        val scraped = extractGenericMetadata(url, "github.com")

        if (parts.size >= 2) {
            val owner = parts[0]
            val repo = parts[1]
            val fallbackOgImage = "https://opengraph.githubassets.com/1/$owner/$repo"

            LinkMetadata(
                title = scraped.title ?: "$owner/$repo: GitHub Repository",
                faviconUrl = scraped.faviconUrl ?: defaultFavicon,
                thumbnailUrl = scraped.thumbnailUrl ?: fallbackOgImage,
                sourcePlatform = "@GitHub",
                description = scraped.description
            )
        } else {
            scraped
        }
    }

    // ==========================================
    // 7. Generic Web Extraction (OpenGraph & Meta)
    // ==========================================
    private suspend fun extractGenericMetadata(
        url: String,
        domain: String,
        userAgent: String = BOT_USER_AGENT
    ): LinkMetadata = withContext(Dispatchers.IO) {
        val defaultFavicon = if (domain.isNotBlank()) {
            "https://www.google.com/s2/favicons?domain=$domain&sz=128"
        } else null

        val defaultPlatform = if (domain.isNotBlank()) {
            formatDomainToPlatformName(domain)
        } else null

        try {
            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .ignoreHttpErrors(true)
                .timeout(6000)
                .get()

            val ogTitle = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
            val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
            val pageTitle = doc.title().takeIf { it.isNotBlank() }
            val title = ogTitle ?: twitterTitle ?: pageTitle ?: defaultPlatform

            val ogDescription = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
            val twitterDescription = doc.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
            val metaDescription = doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
            val description = ogDescription ?: twitterDescription ?: metaDescription

            val ogImage = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
            val twitterImage = doc.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }
            val thumbnailUrl = ogImage ?: twitterImage

            val ogSiteName = doc.select("meta[property=og:site_name]").attr("content").takeIf { it.isNotBlank() }
            val sourcePlatform = if (!ogSiteName.isNullOrBlank()) "@$ogSiteName" else defaultPlatform

            val iconHref = doc.select("link[rel~=(?i)^(shortcut )?icon]").attr("abs:href").takeIf { it.isNotBlank() }
            val faviconUrl = iconHref ?: defaultFavicon

            LinkMetadata(
                title = title,
                faviconUrl = faviconUrl,
                thumbnailUrl = thumbnailUrl,
                sourcePlatform = sourcePlatform,
                description = description
            )
        } catch (e: Exception) {
            LinkMetadata(
                title = defaultPlatform,
                faviconUrl = defaultFavicon,
                thumbnailUrl = null,
                sourcePlatform = defaultPlatform,
                description = null
            )
        }
    }

    private fun httpGet(urlString: String, timeoutMs: Int = 4000): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", BROWSER_USER_AGENT)
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun formatDomainToPlatformName(domain: String): String {
        val clean = domain.removePrefix("www.").lowercase()
        return when {
            clean.contains("instagram") -> "@Instagram"
            clean.contains("threads.net") || clean.contains("threads") -> "@Threads"
            clean.contains("linkedin") -> "@LinkedIn"
            clean.contains("twitter") || clean.contains("x.com") -> "@X"
            clean.contains("youtube") || clean.contains("youtu.be") -> "@YouTube"
            clean.contains("github") -> "@GitHub"
            clean.contains("facebook") || clean.contains("fb.watch") -> "@Facebook"
            clean.contains("reddit") -> "@Reddit"
            clean.contains("medium") -> "@Medium"
            clean.contains("tiktok") -> "@TikTok"
            else -> {
                val parts = clean.split(".")
                val name = if (parts.size >= 2) parts[parts.size - 2] else clean
                "@" + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }
}
