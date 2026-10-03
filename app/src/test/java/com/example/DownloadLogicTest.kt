package com.example

import com.example.data.download.MediaExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLogicTest {

    @Test
    fun calculateEstimatedSizeAudio() {
        val durationMs = 180000L // 3 minutes = 180 seconds

        val size128 = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp3", "128kbps")
        val size192 = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp3", "192kbps")
        val size320 = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp3", "320kbps")

        // 320 kbps should be larger than 192 kbps, which should be larger than 128 kbps
        assertTrue(size320 > size192)
        assertTrue(size192 > size128)
        assertTrue(size128 > 0)
    }

    @Test
    fun calculateEstimatedSizeVideo() {
        val durationMs = 120000L // 2 minutes

        val size360p = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp4", "360p")
        val size720p = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp4", "720p")
        val size1080p = MediaExtractor.calculateEstimatedSizeBytes(durationMs, "mp4", "1080p")

        assertTrue(size1080p > size720p)
        assertTrue(size720p > size360p)
    }

    @Test
    fun formatFileSize() {
        assertEquals("Unknown", MediaExtractor.formatFileSize(0))
        assertEquals("500 KB", MediaExtractor.formatFileSize(500 * 1024L))
        assertEquals("15.0 MB", MediaExtractor.formatFileSize(15 * 1024 * 1024L))
        assertEquals("1.5 GB", MediaExtractor.formatFileSize((1.5 * 1024 * 1024 * 1024L).toLong()))
    }
}
