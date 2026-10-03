package com.example

import com.example.data.download.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {

    @Test
    fun validUrlsAreAccepted() {
        assertTrue(UrlValidator.isValidUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isValidUrl("http://soundcloud.com/artist/track"))
        assertTrue(UrlValidator.isValidUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(UrlValidator.isValidUrl("https://example.com/audio.mp3"))
    }

    @Test
    fun invalidUrlsAreRejected() {
        assertFalse(UrlValidator.isValidUrl(null))
        assertFalse(UrlValidator.isValidUrl(""))
        assertFalse(UrlValidator.isValidUrl("   "))
        assertFalse(UrlValidator.isValidUrl("not a url"))
        assertFalse(UrlValidator.isValidUrl("ftp://invalid-scheme.com"))
        assertFalse(UrlValidator.isValidUrl("http://"))
    }

    @Test
    fun playlistUrlsAreDetected() {
        assertTrue(UrlValidator.isPlaylistUrl("https://www.youtube.com/playlist?list=PL123456789"))
        assertTrue(UrlValidator.isPlaylistUrl("https://www.youtube.com/watch?v=abc&list=PL123456789"))
        assertTrue(UrlValidator.isPlaylistUrl("https://soundcloud.com/user/sets/album-name"))
        assertFalse(UrlValidator.isPlaylistUrl("https://www.youtube.com/watch?v=abc"))
    }

    @Test
    fun extractUrlFromSharedText() {
        val sharedText = "Check out this song: https://youtu.be/dQw4w9WgXcQ listen now!"
        val extracted = UrlValidator.extractUrlFromText(sharedText)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", extracted)

        assertNull(UrlValidator.extractUrlFromText("No url here"))
        assertNull(UrlValidator.extractUrlFromText(""))
        assertNull(UrlValidator.extractUrlFromText(null))
    }

    @Test
    fun supportedDomainsCheck() {
        assertTrue(UrlValidator.isSupportedDomain("https://youtube.com/watch?v=123"))
        assertTrue(UrlValidator.isSupportedDomain("https://m.youtube.com/watch?v=123"))
        assertTrue(UrlValidator.isSupportedDomain("https://soundcloud.com/artist/song"))
        assertTrue(UrlValidator.isSupportedDomain("https://vm.tiktok.com/ZM8example/"))
        assertTrue(UrlValidator.isSupportedDomain("https://cdn.example.com/song.mp3"))
        assertFalse(UrlValidator.isSupportedDomain("https://unsupported-site-example.com/page"))
    }
}
