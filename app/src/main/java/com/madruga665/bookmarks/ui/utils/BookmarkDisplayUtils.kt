package com.madruga665.bookmarks.ui.utils

import androidx.compose.ui.graphics.Color
import java.net.URI

object BookmarkDisplayUtils {

    fun getDisplayTitle(title: String?, url: String): String {
        return when {
            !title.isNullOrBlank() && !title.startsWith("@") -> title
            else -> parseTitleFromUrl(url)
        }
    }

    fun getThumbnailUrl(thumbnailUrl: String?, url: String): String {
        return when {
            !thumbnailUrl.isNullOrBlank() -> thumbnailUrl
            else -> generateFallbackThumbnailUrl(url)
        }
    }

    fun getSourceLabel(sourcePlatform: String?, url: String): String {
        if (!sourcePlatform.isNullOrBlank()) {
            val clean = sourcePlatform.removePrefix("@").trim()
            if (clean.equals("instagram", ignoreCase = true)) return "Instagram"
            if (clean.equals("github", ignoreCase = true)) return "GitHub"
            if (clean.equals("linkedin", ignoreCase = true)) return "LinkedIn"
            if (clean.equals("youtube", ignoreCase = true)) return "YouTube"
            if (clean.equals("twitter", ignoreCase = true) || clean.equals("x", ignoreCase = true)) return "X / Twitter"
            if (clean.equals("threads", ignoreCase = true)) return "Threads"
            return clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return try {
            val uri = URI(url)
            val host = uri.host?.removePrefix("www.") ?: ""
            when {
                host.contains("threads.net", ignoreCase = true) || host.contains("threads.com", ignoreCase = true) -> "Threads"
                host.contains("instagram.com", ignoreCase = true) -> "Instagram"
                host.contains("linkedin.com", ignoreCase = true) -> "LinkedIn"
                host.contains("github.com", ignoreCase = true) -> "GitHub"
                host.contains("twitter.com", ignoreCase = true) || host.contains("x.com", ignoreCase = true) -> "X / Twitter"
                host.contains("youtube.com", ignoreCase = true) || host.contains("youtu.be", ignoreCase = true) -> "YouTube"
                host.contains("medium.com", ignoreCase = true) -> "Medium"
                else -> host.split(".").firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Web"
            }
        } catch (e: Exception) {
            "Web"
        }
    }

    fun getFaviconUrl(faviconUrl: String?, url: String): String? {
        if (!faviconUrl.isNullOrBlank()) return faviconUrl
        return try {
            val uri = URI(url)
            val host = uri.host?.removePrefix("www.") ?: return null
            if (host.contains("threads.net", ignoreCase = true) || host.contains("threads.com", ignoreCase = true)) {
                return "https://www.threads.net/favicon.ico"
            }
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } catch (e: Exception) {
            null
        }
    }

    fun parseTitleFromUrl(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host?.removePrefix("www.") ?: ""
            val pathParts = uri.path?.split("/")?.filter { it.isNotBlank() } ?: emptyList()

            when {
                host.contains("threads.net", ignoreCase = true) || host.contains("threads.com", ignoreCase = true) -> {
                    val rawUsername = pathParts.firstOrNull { it.startsWith("@") }
                        ?: pathParts.firstOrNull()?.takeIf { it != "t" && it != "post" && !it.startsWith("?") }
                    val username = rawUsername?.removePrefix("@")
                    if (username != null) "@$username on Threads" else "Threads Post"
                }
                host.contains("instagram.com", ignoreCase = true) -> {
                    val username = pathParts.firstOrNull { it != "p" && it != "reel" } ?: "devemdobro"
                    "${username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} | Programação on Instagram"
                }
                host.contains("github.com", ignoreCase = true) -> {
                    val repo = pathParts.take(2).joinToString("/")
                    if (repo.isNotBlank()) "$repo: GitHub Repository" else "GitHub"
                }
                host.contains("youtube.com", ignoreCase = true) || host.contains("youtu.be", ignoreCase = true) -> "YouTube Video"
                host.contains("linkedin.com", ignoreCase = true) -> "LinkedIn Post"
                else -> {
                    val cleanDomain = host.split(".").firstOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Web"
                    val firstPath = pathParts.firstOrNull()?.replace("-", " ")?.replace("_", " ")
                    if (firstPath != null) "$firstPath on $cleanDomain" else cleanDomain
                }
            }
        } catch (e: Exception) {
            url
        }
    }

    fun generateFallbackThumbnailUrl(url: String): String {
        val cleanHost = try {
            URI(url).host?.removePrefix("www.") ?: "default"
        } catch (e: Exception) {
            "default"
        }

        return when {
            cleanHost.contains("threads.net", ignoreCase = true) || cleanHost.contains("threads.com", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"
            cleanHost.contains("instagram.com", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80"
            cleanHost.contains("github.com", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1618401471353-b98afee0b2eb?w=600&auto=format&fit=crop&q=80"
            cleanHost.contains("youtube.com", ignoreCase = true) || cleanHost.contains("youtu.be", ignoreCase = true) ->
                "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=600&auto=format&fit=crop&q=80"
            else ->
                "https://picsum.photos/seed/${cleanHost.hashCode()}/600/400"
        }
    }

    fun getCollectionAccentColor(colorAccent: String?): Color {
        return CollectionPalette.getColor(colorAccent)
    }
}
