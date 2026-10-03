# MusicHub Release and Publishing Guide

This document describes the complete release procedure, key generation, secret configuration, version management, and update deployment for MusicHub.

## 1. Generating a Release Keystore

Do not commit the keystore file to Git. Generate it locally using Java keytool:

### Linux / macOS / Bash:
```bash
keytool -genkeypair -v \
  -keystore musichub-release.jks \
  -alias musichub_key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STRONG_STORE_PASSWORD \
  -keypass YOUR_STRONG_KEY_PASSWORD \
  -dname "CN=MusicHub, OU=Mobile, O=MusicHub Org, C=KH"
```

### Windows PowerShell:
```powershell
keytool -genkeypair -v `
  -keystore musichub-release.jks `
  -alias musichub_key `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storepass "YOUR_STRONG_STORE_PASSWORD" `
  -keypass "YOUR_STRONG_KEY_PASSWORD" `
  -dname "CN=MusicHub, OU=Mobile, O=MusicHub Org, C=KH"
```

## 2. Safely Backing Up the Keystore

The keystore certificate is permanently required to sign app updates. If lost, existing users will not be able to install updates over their current installations.
- Store `musichub-release.jks` in an encrypted password manager or secure offline physical drive.
- Record the passwords, key alias, and expiration date securely.
- Never add `*.jks` or `*.keystore` files to the Git repository.

## 3. Encoding Keystore as Base64

Encode the keystore file as a Base64 string for GitHub Actions Secrets:

### Linux / macOS / Bash:
```bash
base64 -w 0 musichub-release.jks > keystore_base64.txt
```

### Windows PowerShell:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("musichub-release.jks")) | Set-Content "keystore_base64.txt"
```

## 4. Configuring GitHub Repository Secrets

Open your repository on GitHub:
1. Navigate to Settings -> Secrets and variables -> Actions.
2. Click "New repository secret".
3. Add the following four secrets:

- `MUSICHUB_KEYSTORE_BASE64`: Paste the entire content of `keystore_base64.txt`.
- `MUSICHUB_KEYSTORE_PASSWORD`: The keystore store password (`YOUR_STRONG_STORE_PASSWORD`).
- `MUSICHUB_KEY_ALIAS`: The key alias (e.g. `musichub_key` or `upload`).
- `MUSICHUB_KEY_PASSWORD`: The key password (`YOUR_STRONG_KEY_PASSWORD`).

## 5. Updating versionCode and versionName

In `app/build.gradle.kts`:
- Increment `versionCode` monotonically for each production release (e.g. 1 -> 2 -> 3).
- Update `versionName` to reflect semantic versioning (e.g. "1.0.0" -> "1.0.1" -> "1.1.0").

```kotlin
defaultConfig {
    applicationId = "com.aistudio.musichub.vwnkqp"
    minSdk = 26
    targetSdk = 35
    versionCode = 2
    versionName = "1.0.1"
}
```

## 6. Updating Changelogs

Update the changelog in both Khmer and English:
1. In `version.json`:
```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "apkUrl": "https://github.com/OWNER/REPOSITORY/releases/download/v1.0.1/MusicHub-release.apk",
  "sha256": "ACTUAL_SHA256_HASH_HEX",
  "changelog": {
    "en": [
      "Optimized media stream extraction",
      "Improved playlist drag reorder stability"
    ],
    "km": [
      "បង្កើនប្រសិទ្ធភាពនៃការទាញយកមេឌៀ",
      "កែលម្អការរៀបចំលំដាប់បទក្នុង Playlist"
    ]
  },
  "forceUpdate": false,
  "minSupportedVersionCode": 1
}
```

## 7. Creating and Pushing a Version Tag

Tag the verified Git commit:
```bash
git add .
git commit -m "Prepare release v1.0.1"
git push origin main

git tag v1.0.1
git push origin v1.0.1
```

The GitHub Actions release job will automatically:
1. Run tests and lint checks.
2. Build the signed release APK using the decoded keystore secret.
3. Compute the authentic SHA-256 hash.
4. Generate `version.json`.
5. Create a GitHub Release and attach the APK, `version.json`, and `SHA256SUMS.txt`.

## 8. Downloading Debug APKs from Actions Artifacts

For development testing without release signing:
1. Open the GitHub repository.
2. Click on the "Actions" tab.
3. Select the latest workflow run on the `main` branch.
4. Scroll to the "Artifacts" section.
5. Click `MusicHub-debug` to download the zip containing the debug APK.
6. Extract and install the APK on an Android device running Android 8.0+ (API 26+).

## 9. Downloading Signed Release APKs

1. Open the GitHub repository.
2. Navigate to "Releases" in the right sidebar.
3. Select the latest tag (e.g. `v1.0.1`).
4. Under "Assets", click `MusicHub-release.apk`.

## 10. Calculating and Verifying SHA-256 Checksum

Verify that the downloaded APK was not modified in transit:

### Linux / macOS:
```bash
sha256sum MusicHub-release.apk
```

### Windows PowerShell:
```powershell
Get-FileHash -Algorithm SHA256 MusicHub-release.apk
```

Compare the output hash against the hash listed in `SHA256SUMS.txt` or `version.json`.

## 11. Verifying version.json Manifest

The `version.json` file controls in-app update checks:
- Verify that `versionCode` is greater than the current app version.
- Verify that `apkUrl` points to the exact GitHub Release asset URL.
- Verify that `sha256` matches the real calculated checksum of `MusicHub-release.apk`.
- Verify that both `km` and `en` changelogs are present.

## 12. Testing Upgrading an Existing Installation

1. Install the previous signed release APK on a physical device or emulator.
2. Download the newly built signed release APK.
3. Run:
```bash
adb install -r MusicHub-release.apk
```
4. Verify that:
   - Installation succeeds with `Success` status.
   - User data, playlists, and downloaded media remain intact.
   - Database migrations execute without data loss.

## 13. Troubleshooting Signing and Publishing Failures

- **Missing Secrets**: Ensure all four secret names in GitHub match exactly: `MUSICHUB_KEYSTORE_BASE64`, `MUSICHUB_KEYSTORE_PASSWORD`, `MUSICHUB_KEY_ALIAS`, `MUSICHUB_KEY_PASSWORD`.
- **Bad Base64**: If decoding fails, re-encode without line breaks using `base64 -w 0`.
- **Password Mismatch**: Confirm the keystore password and key password are correct.
- **Workflow Permissions**: In GitHub repository Settings -> Actions -> General -> Workflow permissions, ensure "Read and write permissions" is selected so the workflow can publish GitHub Releases.

## 14. Key Preservation Checklist

- Backup keystore to at least two independent offline storage media.
- Keep a secure offline note of the passwords and alias.
- Never delete the keystore file.
- If migrating to a new build machine or CI runner, re-import the same keystore.
