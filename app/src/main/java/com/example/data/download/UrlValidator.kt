package com.example.data.download

import java.net.URI

object UrlValidator {

    private val SUPPORTED_HOSTS = listOf(
        "youtube.com",
        "youtu.be",
        "m.youtube.com",
        "soundcloud.com",
        "m.soundcloud.com",
        "tiktok.com",
        "vm.tiktok.com",
        "facebook.com",
        "fb.watch",
        "instagram.com",
        "vimeo.com",
        "dailymotion.com",
        "twitter.com",
        "x.com"
    )

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        return try {
            val uri = URI(trimmed)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun isPlaylistUrl(url: String): Boolean {
        if (!isValidUrl(url)) return false
        val lower = url.lowercase()
        return lower.contains("list=") || lower.contains("/playlist") || lower.contains("/sets/")
    }

    fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val words = text.split("\\s+".toRegex())
        for (word in words) {
            if (isValidUrl(word)) {
                return word.trim()
            }
        }
        return null
    }

    fun isSupportedDomain(url: String): Boolean {
        if (!isValidUrl(url)) return false
        return try {
            val host = URI(url.trim()).host?.lowercase() ?: return false
            SUPPORTED_HOSTS.any { host == it || host.endsWith(".$it") } ||
                    url.endsWith(".mp3") || url.endsWith(".mp4") || url.endsWith(".m4a") || url.endsWith(".webm")
        } catch (_: Exception) {
            false
        }
    }
}
