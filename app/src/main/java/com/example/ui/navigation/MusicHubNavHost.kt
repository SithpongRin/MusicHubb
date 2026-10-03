package com.example.ui.navigation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.MusicHubApp
import com.example.R
import com.example.data.download.UrlValidator
import com.example.domain.models.AppLanguage
import com.example.domain.models.Song
import com.example.domain.models.ThemeMode
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.download.DownloadScreen
import com.example.ui.download.DownloadViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.playlist.PlaylistDetailScreen
import com.example.ui.playlist.PlaylistDetailViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MusicHubTheme
import com.example.ui.update.UpdateViewModel
import kotlinx.coroutines.launch
import java.util.Locale

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

@Composable
fun MusicHubMain(
    initialSharedUrl: String? = null
) {
    val container = MusicHubApp.instance.container
    val context = LocalContext.current

    val language by container.settingsRepository.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.KHMER)
    val themeMode by container.settingsRepository.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

    // Locale configuration
    val currentLocale = when (language) {
        AppLanguage.KHMER -> Locale("km")
        AppLanguage.ENGLISH -> Locale("en")
    }

    val configuration = LocalConfiguration.current
    configuration.setLocale(currentLocale)

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration
    ) {
        MusicHubTheme(darkTheme = isDark) {
            val navController = rememberNavController()

            // ViewModels
            val homeViewModel = remember {
                HomeViewModel(container.mediaRepository, container.playbackManager)
            }
            val downloadViewModel = remember {
                DownloadViewModel(container.mediaExtractor, container.downloadRepository, container.mediaRepository)
            }
            val libraryViewModel = remember {
                LibraryViewModel(container.mediaRepository, container.downloadRepository, container.playbackManager)
            }
            val searchViewModel = remember {
                SearchViewModel(container.mediaRepository, container.onlineSearchRepository, container.downloadRepository, container.playbackManager)
            }
            val settingsViewModel = remember {
                SettingsViewModel(context, container.settingsRepository, container.downloadRepository, container.updateRepository)
            }
            val updateViewModel = remember {
                UpdateViewModel(container.updateRepository)
            }

            var showDownloadModal by remember { mutableStateOf(false) }
            var showNowPlayingSheet by remember { mutableStateOf(false) }
            var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

            // Handle prefilled shared URL
            LaunchedEffect(initialSharedUrl) {
                if (!initialSharedUrl.isNullOrBlank()) {
                    val url = UrlValidator.extractUrlFromText(initialSharedUrl)
                    if (url != null) {
                        downloadViewModel.onUrlInputChanged(url)
                        downloadViewModel.fetchMetadata(url)
                        showDownloadModal = true
                    }
                }
            }

            // Playback state
            val currentSong by container.playbackManager.currentSong.collectAsStateWithLifecycle()
            val isPlaying by container.playbackManager.isPlaying.collectAsStateWithLifecycle()
            val positionMs by container.playbackManager.currentPositionMs.collectAsStateWithLifecycle()
            val durationMs by container.playbackManager.durationMs.collectAsStateWithLifecycle()
            val repeatMode by container.playbackManager.repeatMode.collectAsStateWithLifecycle()
            val shuffleMode by container.playbackManager.shuffleMode.collectAsStateWithLifecycle()
            val speed by container.playbackManager.playbackSpeed.collectAsStateWithLifecycle()
            val sleepTimer by container.playbackManager.sleepTimerSeconds.collectAsStateWithLifecycle()
            val isVideoPresentation by container.playbackManager.isVideoPresentation.collectAsStateWithLifecycle()

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val isBottomBarVisible = currentDestination?.route in listOf(
                Screen.Home.route,
                Screen.Search.route,
                Screen.Library.route,
                Screen.Settings.route
            )

            Scaffold(
                bottomBar = {
                    if (isBottomBarVisible) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Mini Player above navigation bar when song is available
                            if (currentSong != null) {
                                MiniPlayer(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    positionMs = positionMs,
                                    durationMs = durationMs,
                                    onTogglePlayPause = { container.playbackManager.togglePlayPause() },
                                    onNext = { container.playbackManager.next() },
                                    onToggleFavorite = {
                                        currentSong?.let { s ->
                                            homeViewModel.toggleFavorite(s)
                                        }
                                    },
                                    onClick = { showNowPlayingSheet = true }
                                )
                            }

                            // Bottom Navigation Bar with Overlapping Center Add Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                            ) {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .align(Alignment.BottomCenter),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp
                                ) {
                                    // 1. Home
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (currentDestination?.route == Screen.Home.route) Icons.Default.Home else Icons.Outlined.Home,
                                                contentDescription = "Home"
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_home)) },
                                        selected = currentDestination?.route == Screen.Home.route,
                                        onClick = {
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )

                                    // 2. Search
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (currentDestination?.route == Screen.Search.route) Icons.Default.Search else Icons.Outlined.Search,
                                                contentDescription = "Search"
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_search)) },
                                        selected = currentDestination?.route == Screen.Search.route,
                                        onClick = {
                                            navController.navigate(Screen.Search.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )

                                    // 3. Center Spacer for 56dp Add FAB
                                    Spacer(modifier = Modifier.weight(1f))

                                    // 4. Library
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (currentDestination?.route == Screen.Library.route) Icons.Default.LibraryMusic else Icons.Outlined.LibraryMusic,
                                                contentDescription = "Library"
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_library)) },
                                        selected = currentDestination?.route == Screen.Library.route,
                                        onClick = {
                                            navController.navigate(Screen.Library.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )

                                    // 5. Settings
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (currentDestination?.route == Screen.Settings.route) Icons.Default.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = { Text(stringResource(R.string.nav_settings)) },
                                        selected = currentDestination?.route == Screen.Settings.route,
                                        onClick = {
                                            navController.navigate(Screen.Settings.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }

                                // Center 56dp Circular Overlapping Add Button
                                FloatingActionButton(
                                    onClick = { showDownloadModal = true },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-14).dp)
                                        .size(56.dp)
                                        .testTag("global_add_download_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Download",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToDownload = { showDownloadModal = true },
                            onNavigateToLibrary = { navController.navigate(Screen.Library.route) }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(viewModel = searchViewModel)
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onNavigateToPlaylist = { playlistId ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            updateViewModel = updateViewModel
                        )
                    }

                    composable(
                        route = Screen.PlaylistDetail.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                        val playlistViewModel = remember(playlistId) {
                            PlaylistDetailViewModel(
                                playlistId = playlistId,
                                mediaRepository = container.mediaRepository,
                                playbackManager = container.playbackManager
                            )
                        }
                        PlaylistDetailScreen(
                            viewModel = playlistViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            // Full-screen Download Modal
            if (showDownloadModal) {
                DownloadScreen(
                    viewModel = downloadViewModel,
                    onClose = { showDownloadModal = false }
                )
            }

            // Now Playing Sheet
            if (showNowPlayingSheet && currentSong != null) {
                NowPlayingSheet(
                    playbackManager = container.playbackManager,
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    speed = speed,
                    sleepTimerSeconds = sleepTimer,
                    isVideoPresentation = isVideoPresentation,
                    onDismiss = { showNowPlayingSheet = false },
                    onToggleFavorite = { s -> homeViewModel.toggleFavorite(s) },
                    onAddToPlaylist = { s -> songToAddToPlaylist = s }
                )
            }

            // Add To Playlist Dialog
            val coroutineScope = rememberCoroutineScope()
            songToAddToPlaylist?.let { song ->
                val playlists by container.mediaRepository.getPlaylists().collectAsStateWithLifecycle(initialValue = emptyList())
                AddToPlaylistDialog(
                    song = song,
                    playlists = playlists,
                    onAddToPlaylist = { playlistId, songId ->
                        coroutineScope.launch {
                            container.mediaRepository.addSongToPlaylist(playlistId, songId)
                        }
                    },
                    onCreateAndAdd = { name, songId ->
                        coroutineScope.launch {
                            val newId = container.mediaRepository.createPlaylist(name)
                            container.mediaRepository.addSongToPlaylist(newId, songId)
                        }
                    },
                    onDismiss = { songToAddToPlaylist = null }
                )
            }
        }
    }
}
