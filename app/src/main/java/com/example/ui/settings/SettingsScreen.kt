package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.models.AppLanguage
import com.example.domain.models.ThemeMode
import com.example.ui.components.UpdateDialog
import com.example.ui.update.UpdateViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateDownloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.updateMessage) {
        uiState.updateMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUpdateMessage()
        }
    }

    LaunchedEffect(uiState.engineMessage) {
        uiState.engineMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissEngineMessage()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Appearance Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    // Theme
                    val themeLabel = when (uiState.themeMode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    }
                    SettingsRow(
                        title = stringResource(R.string.pref_theme),
                        subtitle = themeLabel,
                        icon = Icons.Default.Palette,
                        onClick = { showThemeDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Language
                    val langLabel = when (uiState.language) {
                        AppLanguage.KHMER -> stringResource(R.string.lang_khmer)
                        AppLanguage.ENGLISH -> stringResource(R.string.lang_english)
                    }
                    SettingsRow(
                        title = stringResource(R.string.pref_language),
                        subtitle = langLabel,
                        icon = Icons.Default.Translate,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Downloads Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_download)) {
                    // Wi-Fi only toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.pref_wifi_only),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = uiState.wifiOnly,
                            onCheckedChange = { viewModel.setWifiOnly(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Default Format
                    SettingsRow(
                        title = stringResource(R.string.pref_default_format),
                        subtitle = uiState.defaultFormat.uppercase(),
                        icon = Icons.Default.Audiotrack,
                        onClick = {
                            val next = if (uiState.defaultFormat == "mp3") "mp4" else "mp3"
                            viewModel.setDefaultFormat(next)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Default Quality
                    SettingsRow(
                        title = stringResource(R.string.pref_default_quality),
                        subtitle = uiState.defaultQuality,
                        icon = Icons.Default.Audiotrack,
                        onClick = {
                            val next = if (uiState.defaultQuality == "320kbps") "192kbps" else "320kbps"
                            viewModel.setDefaultQuality(next)
                        }
                    )
                }
            }

            // Playback Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_playback)) {
                    // Audio focus
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.pref_audio_focus),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = uiState.audioFocus,
                            onCheckedChange = { viewModel.setAudioFocus(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Gapless playback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.pref_gapless),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = uiState.gapless,
                            onCheckedChange = { viewModel.setGapless(it) }
                        )
                    }
                }
            }

            // Storage Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_storage)) {
                    SettingsRow(
                        title = stringResource(R.string.storage_audio_usage, uiState.audioStorageFormatted),
                        subtitle = stringResource(R.string.storage_app_specific),
                        icon = Icons.Default.Storage
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = stringResource(R.string.storage_video_usage, uiState.videoStorageFormatted),
                        subtitle = stringResource(R.string.storage_app_specific),
                        icon = Icons.Default.Storage
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.storage_cache_usage, uiState.cacheStorageFormatted),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        FilledTonalButton(onClick = { viewModel.clearCache() }) {
                            Text(stringResource(R.string.action_clear_cache))
                        }
                    }
                }
            }

            // Updates Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_updates)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_installed_version, uiState.installedVersion),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !uiState.isCheckingUpdate,
                            modifier = Modifier.testTag("settings_check_updates_button")
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.action_check_updates))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Update extraction engine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.action_update_engine),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FilledTonalButton(
                            onClick = { viewModel.updateEngine() },
                            enabled = !uiState.isEngineUpdating
                        ) {
                            if (uiState.isEngineUpdating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Update")
                            }
                        }
                    }
                }
            }

            // About Section
            item {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    SettingsRow(
                        title = stringResource(R.string.about_version, uiState.installedVersion),
                        subtitle = "MusicHub for Android",
                        icon = Icons.Default.Info
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = stringResource(R.string.about_disclaimer_title),
                        subtitle = "Legal and copyright information",
                        icon = Icons.Default.Info,
                        onClick = { showDisclaimerDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        title = stringResource(R.string.about_licenses),
                        subtitle = "Open-source software used in MusicHub",
                        icon = Icons.Default.Info,
                        onClick = { showLicensesDialog = true }
                    )
                }
            }
        }
    }

    // Theme Chooser Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.pref_theme)) },
            text = {
                Column {
                    listOf(
                        ThemeMode.SYSTEM to R.string.theme_system,
                        ThemeMode.LIGHT to R.string.theme_light,
                        ThemeMode.DARK to R.string.theme_dark
                    ).forEach { (mode, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Language Chooser Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.pref_language)) },
            text = {
                Column {
                    listOf(
                        AppLanguage.KHMER to R.string.lang_khmer,
                        AppLanguage.ENGLISH to R.string.lang_english
                    ).forEach { (lang, labelRes) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.language == lang,
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(labelRes))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Platform Terms and Copyright Disclaimer Dialog
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(stringResource(R.string.about_disclaimer_title)) },
            text = {
                Text(
                    text = stringResource(R.string.about_disclaimer_content),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text(stringResource(R.string.about_licenses)) },
            text = {
                Column {
                    val licenses = listOf(
                        "Jetpack Compose - Apache 2.0",
                        "Media3 ExoPlayer - Apache 2.0",
                        "Room Database - Apache 2.0",
                        "Coil - Apache 2.0",
                        "OkHttp - Apache 2.0",
                        "yt-dlp engine - Unlicense / Public Domain",
                        "FFmpeg - LGPL / GPL"
                    )
                    licenses.forEach { lic ->
                        Text(
                            text = "- $lic",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Update Dialog if manifest available
    uiState.updateManifest?.let { manifest ->
        UpdateDialog(
            manifest = manifest,
            downloadState = updateDownloadState,
            isKhmer = uiState.language == AppLanguage.KHMER,
            onStartDownload = { updateViewModel.downloadAndVerify(manifest) },
            onCancelDownload = { updateViewModel.cancelDownload() },
            onInstall = { file -> updateViewModel.install(file) },
            onOpenPermissionSettings = {
                updateViewModel.getPermissionIntent()?.let { intent ->
                    context.startActivity(intent)
                }
            },
            canInstallPackages = updateViewModel.canInstall(),
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
