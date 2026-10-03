# MusicHub Automated Testing Strategy and Verification

This document specifies the testing methodology, automated test suites, and command execution for MusicHub.

## 1. Test Architecture

MusicHub emphasizes local JVM testing using JUnit 4, Kotlin Coroutines Test, and Robolectric 4.16+.

Instrumented device tests (requiring physical emulators or adb) are avoided in CI/CD environments to guarantee fast, deterministic test execution.

## 2. Test Suites

### A. URL Validation (`UrlValidatorTest.kt`)
- Validates supported HTTP and HTTPS protocol schemes.
- Rejects malformed, empty, blank, or unsupported schemes (e.g. ftp://).
- Detects playlist indicators (`list=`, `/playlist`, `/sets/`).
- Extracts valid URLs from surrounding text (e.g. shared from WhatsApp, Telegram, or Messenger).
- Validates host matching against supported domains.

### B. Update and Versioning Logic (`UpdateLogicTest.kt`)
- Numerical `versionCode` comparisons: verifies higher remote versions trigger updates while equal or lower versions do not.
- Minimum supported version: verifies that if `installedVersion < minSupportedVersionCode`, the update is mandatory.
- Forced update: verifies that `forceUpdate = true` prevents the user from skipping the update.
- SHA-256 Checksum: creates temporary files, computes authentic SHA-256 hashes, verifies 64-character hex output, and validates that modified files fail checksum verification.

### C. Download Logic and Calculations (`DownloadLogicTest.kt`)
- Audio size calculations: verifies that 320 kbps > 192 kbps > 128 kbps for identical track durations.
- Video size calculations: verifies scaling from 360p to 720p to 1080p.
- Human-readable file size formatting: tests conversion into KB, MB, and GB boundaries.

### D. Database and Repository Integration (`DatabaseAndRepositoryTest.kt`)
- Uses Robolectric with an in-memory SQLite Room Database (`AppDatabase`).
- Verifies song insertion, retrieval, and field integrity.
- Tests duplicate detection in DAO (`findExisting`).
- Tests favorite toggle and reactive Flow emission.
- Tests playlist creation, song assignment, query by playlist, and cascade deletion.
- Tests sorting order: verifies ascending and descending results by song name.

### E. Context and Localization (`ExampleRobolectricTest.kt`)
- Verifies that application resources resolve properly.
- Validates that `R.string.app_name` resolves to "MusicHub".

## 3. Running Tests Locally

Run all unit and Robolectric tests via Gradle:

```bash
gradle :app:testDebugUnitTest
```

Or using the Gradle wrapper:

```bash
./gradlew testDebugUnitTest
```

### Viewing Test Results
After execution, HTML test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

## 4. CI/CD Integration

The GitHub Actions workflow (`.github/workflows/build.yml`) executes:
```bash
./gradlew testDebugUnitTest --no-daemon --stacktrace
```
on every pull request and push to the `main` or `master` branches before generating any APK artifacts.
