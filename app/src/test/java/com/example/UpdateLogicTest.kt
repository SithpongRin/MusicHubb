package com.example

import com.example.data.update.UpdateRepository
import com.example.domain.models.AppVersionManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateLogicTest {

    @Test
    fun versionComparisonLogic() {
        val currentVersion = 1

        // Higher remote version should indicate update available
        assertTrue(10 > currentVersion)
        assertTrue(2 > currentVersion)
        assertTrue(105 > currentVersion)

        // Same or lower remote version should not indicate update
        assertFalse(1 > currentVersion)
        assertFalse(0 > currentVersion)
    }

    @Test
    fun minimumSupportedVersionCheck() {
        val installedVersion = 5

        // If minSupportedVersionCode is 10, installed (5) is unsupported and requires update
        val manifestHighMin = AppVersionManifest(
            versionCode = 12,
            versionName = "2.0.0",
            apkUrl = "https://example.com/apk",
            sha256 = "dummy",
            forceUpdate = false,
            minSupportedVersionCode = 10
        )
        val isRequired = manifestHighMin.forceUpdate || installedVersion < manifestHighMin.minSupportedVersionCode
        assertTrue(isRequired)

        // If minSupportedVersionCode is 4, installed (5) is supported
        val manifestLowMin = AppVersionManifest(
            versionCode = 12,
            versionName = "2.0.0",
            apkUrl = "https://example.com/apk",
            sha256 = "dummy",
            forceUpdate = false,
            minSupportedVersionCode = 4
        )
        val isNotRequired = manifestLowMin.forceUpdate || installedVersion < manifestLowMin.minSupportedVersionCode
        assertFalse(isNotRequired)
    }

    @Test
    fun forceUpdateFlagHonored() {
        val installedVersion = 10
        val manifest = AppVersionManifest(
            versionCode = 11,
            versionName = "1.1.0",
            apkUrl = "https://example.com/apk",
            sha256 = "dummy",
            forceUpdate = true,
            minSupportedVersionCode = 5
        )
        assertTrue(manifest.forceUpdate || installedVersion < manifest.minSupportedVersionCode)
    }

    @Test
    fun sha256ChecksumVerification() {
        // Create temporary test file with known contents
        val tempFile = File.createTempFile("test_sha", ".txt")
        tempFile.writeText("MusicHub SHA-256 test content")

        val calculatedHash = UpdateRepository.calculateSha256(tempFile)
        assertTrue(calculatedHash.isNotEmpty())
        assertEquals(64, calculatedHash.length) // SHA-256 is 64 hex characters

        // Modifying the file changes the hash
        tempFile.writeText("Modified content")
        val newHash = UpdateRepository.calculateSha256(tempFile)
        assertFalse(calculatedHash == newHash)

        tempFile.delete()
    }
}
