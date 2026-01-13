package com.radioandroid.ui

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.radioandroid.data.FavoritesRepository
import com.radioandroid.data.RadioStation
import com.radioandroid.data.RadioStations
import com.radioandroid.util.NavigationUtils
import com.radioandroid.timer.SleepTimerManager
import com.radioandroid.cast.CastManager
import com.radioandroid.premium.PremiumManager
import com.radioandroid.ui.components.AlarmSheet
import com.radioandroid.ui.components.HelpSheet
import com.radioandroid.ui.components.LegalSheet
import com.radioandroid.ui.components.LogViewerSheet
import com.radioandroid.ui.components.PlayerBar
import com.radioandroid.ui.components.PremiumInfoSheet
import com.radioandroid.ui.components.SettingsDrawer
import com.radioandroid.ui.components.SleepTimerSheet
import com.radioandroid.ui.components.StationCard
import com.radioandroid.ui.components.StationListItem
import com.radioandroid.ui.navigation.SheetState
import com.google.android.gms.cast.framework.CastButtonFactory

private fun getCountryFlag(country: String): String {
    return when (country.lowercase()) {
        "españa", "spain" -> "🇪🇸"
        "méxico", "mexico" -> "🇲🇽"
        "united kingdom", "uk", "great britain" -> "🇬🇧"
        else -> "🌍"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(
    mediaController: MediaController?,
    modifier: Modifier = Modifier,
    onThemeChanged: (com.radioandroid.data.ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Preferences State
    var isGenreGroupedUI by remember { mutableStateOf(com.radioandroid.data.AppPreferences.isGenreGroupedUI) }
    
    // Observe preferences changes (simple polling or re-read on resume would be better, but this works for now if Settings updates singleton)
    LaunchedEffect(Unit) {
        // In a real app we'd observe a Flow from AppPreferences
    }
    
    // Detect User Country (Simulate logic or use Locale)
    // For testing/default we might want to check system locale
    // val userCountryCode = java.util.Locale.getDefault().country // "ES", "MX", "GB"
    // specific logic: map codes to our display names
    val currentLocale = java.util.Locale.getDefault()
    val homeCountryName = remember {
        // Simple mapping for demo purposes. In prod use real mapping or iso codes in data model.
        when (currentLocale.country.uppercase()) {
            "MX" -> "México"
            "GB", "UK" -> "United Kingdom"
            "ES" -> "España"
            else -> "España" // Default fall-back
        }
    }
    
    // State management for MediaController
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var currentStation by remember { mutableStateOf<RadioStation?>(null) }
    // Now Playing Metadata State


    
    // Network issue detection
    var isNetworkIssue by remember { mutableStateOf(false) }
    var bufferingStartTime by remember { mutableStateOf(0L) }
    
    // Favorites state - using lifecycle-aware collection (2026 best practice)
    val favoriteIds by FavoritesRepository.favoriteIds.collectAsStateWithLifecycle()
    
    // Timer state
    val timerActive by SleepTimerManager.isActive.collectAsStateWithLifecycle()
    
    // Time-Shift delay state
    val accumulatedDelay by com.radioandroid.player.PlayerState.accumulatedDelay.collectAsStateWithLifecycle()
    
    // Cast state
    val castManager = remember { CastManager.getInstance(context) }
    val isCastAvailable by castManager.castDeviceAvailable.collectAsStateWithLifecycle()
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    val isPremium by PremiumManager.isPremium.collectAsStateWithLifecycle()
    
    // Sheet state - using sealed class for type-safe navigation (2026 best practice)
    var sheetState by remember { mutableStateOf<SheetState>(SheetState.Hidden) }
    // List/Grid view toggle
    var isListView by remember { mutableStateOf(false) }
    
    // Search State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    // Collapsed State for International Sections
    val expandedCountries = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    // State for Collapsible Categories (Genres)
    val expandedGenres = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    
    // SMART BACK NAVIGATION using sealed class
    androidx.activity.compose.BackHandler(
        enabled = isSearchActive || sheetState != SheetState.Hidden
    ) {
        when {
            // If a sheet is open, navigate to its parent
            sheetState != SheetState.Hidden -> {
                sheetState = sheetState.parentSheet()
            }
            // Search handling
            isSearchActive -> {
                if (searchQuery.isNotEmpty()) searchQuery = "" else isSearchActive = false
            }
        }
    }
    
    // Bottom Sheets - Rendered based on sealed class state (2026 best practice)
    // Child sheets that appear when settings drawer is open
    val showingSettingsChild = sheetState.isSettingsChild()
    val isDrawerVisible = sheetState == SheetState.Settings || showingSettingsChild

    // Calculate drawer parameters (State Hoisting for Parallax)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = with(androidx.compose.ui.platform.LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val drawerWidth = screenWidth * 0.9f
    
    // Animation State (Offset in pixels: 0 = Fully Open, drawerWidth = Fully Closed)
    val drawerOffset = remember { androidx.compose.animation.core.Animatable(drawerWidth) }
    
    // Sync animation with visibility state
    // Skip animation if drawer is already at target position (e.g., closed via gesture)
    androidx.compose.runtime.LaunchedEffect(isDrawerVisible) {
        val targetOffset = if (isDrawerVisible) 0f else drawerWidth
        // Only animate if not already at target (prevents double-animation on gesture close)
        if (kotlin.math.abs(drawerOffset.value - targetOffset) > 1f) {
            // Fast spring animation for snappy feel (like Spotify)
            drawerOffset.animateTo(
                targetOffset, 
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.85f,
                    stiffness = 500f
                )
            )
        } else {
            // Snap to exact position without animation
            drawerOffset.snapTo(targetOffset)
        }
    }
    
    // Stations sorted with favorites first
    // Group Stations (with Search Filtering) - using derivedStateOf for performance
    val favoriteStations by remember {
        derivedStateOf {
            RadioStations.stations.filter { 
                it.id in favoriteIds && 
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.genre.contains(searchQuery, ignoreCase = true))
            }
        }
    }
    
    val localStations by remember {
        derivedStateOf {
            RadioStations.stations.filter { 
                 it.country == homeCountryName && it.id !in favoriteIds &&
                 (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.genre.contains(searchQuery, ignoreCase = true))
            }
            // Removed .sortedBy { it.name } to preserve popularity order from RadioStations.kt
        }
    }
    
    val internationalStationsGrouped by remember {
        derivedStateOf {
            RadioStations.stations.filter { 
                it.country != homeCountryName && it.id !in favoriteIds &&
                (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.genre.contains(searchQuery, ignoreCase = true) || it.country.contains(searchQuery, ignoreCase = true))
            }.groupBy { it.country }.toSortedMap()
        }
    }
    
    // Group By Genre (New Feature)
    val stationsByGenre by remember(isGenreGroupedUI) {
        derivedStateOf {
             RadioStations.stations
                .filter { it.id !in favoriteIds && 
                    (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.genre.contains(searchQuery, ignoreCase = true))
                }
                .groupBy { 
                    it.genre
                }
                .toSortedMap()
        }
    }



    val toggleFavorite: (RadioStation) -> Unit = { station ->
        when (val result = FavoritesRepository.toggleFavorite(station.id)) {
            is FavoritesRepository.ToggleResult.Added -> {
                Toast.makeText(context, "${station.name} añadida a favoritos", Toast.LENGTH_SHORT).show()
            }
            is FavoritesRepository.ToggleResult.Removed -> {
                Toast.makeText(context, "${station.name} quitada de favoritos", Toast.LENGTH_SHORT).show()
            }
            is FavoritesRepository.ToggleResult.LimitReached -> {
                sheetState = SheetState.Premium
                Toast.makeText(
                    context, 
                    "Máximo ${result.limit} favoritos en versión gratuita", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val playStation: (RadioStation) -> Unit = { station ->
        mediaController?.let { controller ->
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setSubtitle(station.genre)
                        .setArtist(station.genre)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                        .build()
                )
                .build()
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
            currentStation = station
        }
    }
    
    // Listen to MediaController state changes
    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING
                // Detect network issue: buffering for >3 seconds indicates poor connection
                if (playbackState == Player.STATE_BUFFERING) {
                    if (bufferingStartTime == 0L) {
                        bufferingStartTime = System.currentTimeMillis()
                    }
                    if (System.currentTimeMillis() - bufferingStartTime > 3000) {
                        isNetworkIssue = true
                    }
                } else {
                    bufferingStartTime = 0L
                    isNetworkIssue = false
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Find the station by mediaId
                mediaItem?.mediaId?.let { mediaId ->
                    currentStation = RadioStations.stations.find { 
                        it.id.toString() == mediaId 
                    }
                } ?: run {
                    currentStation = null
                }
            }
            

        }
        
        mediaController?.addListener(listener)
        
        // Initialize state from current controller state
        mediaController?.let { controller ->
            isPlaying = controller.isPlaying
            isLoading = controller.playbackState == Player.STATE_BUFFERING
            controller.currentMediaItem?.mediaId?.let { mediaId ->
                currentStation = RadioStations.stations.find { it.id.toString() == mediaId }
            } ?: run {
                currentStation = null
            }

        }
        
        onDispose {
            mediaController?.removeListener(listener)
        }
    }
    
    
    // Render child sheets (these still use bottom sheets)
    when (sheetState) {
        is SheetState.DebugLogs -> {
            LogViewerSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onNavigateBack = { sheetState = sheetState.parentSheet() }
            )
        }
        is SheetState.SleepTimer -> {
            SleepTimerSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onTimerSet = { /* Timer started */ },
                onStopPlayback = { mediaController?.stop() },
                onNavigateBack = { sheetState = sheetState.parentSheet() }
            )
        }
        is SheetState.Alarm -> {
            AlarmSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onPremiumClick = { sheetState = SheetState.Premium },
                onNavigateBack = { sheetState = sheetState.parentSheet() }
            )
        }
        is SheetState.Premium -> {
            PremiumInfoSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onSubscribe = {
                    Toast.makeText(context, "Próximamente: Suscripción Premium", Toast.LENGTH_SHORT).show()
                    sheetState = SheetState.Hidden
                }
            )
        }
        is SheetState.Help -> {
            HelpSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onDebugLogsClick = { sheetState = SheetState.DebugLogs },
                onLegalClick = { sheetState = SheetState.Legal },
                onNavigateBack = { sheetState = sheetState.parentSheet() }
            )
        }
        is SheetState.Legal -> {
            LegalSheet(
                onDismiss = { sheetState = SheetState.Hidden },
                onNavigateBack = { sheetState = sheetState.parentSheet() }
            )
        }
        // Settings and Hidden handled separately with drawer
        is SheetState.Settings, is SheetState.Hidden -> { /* Handled by drawer below */ }
    }

    // Main UI structure with drawer overlay
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                // Apply Parallax translation (slides Main UI to the left by 25% of width)
                // Apply Parallax translation (slides Main UI to the left by 25% of width)
                // Use the real-time drawerOffset for 1:1 synchronization with finger drag (Spotify style)
                .graphicsLayer {
                    // drawerOffset.value goes from drawerWidth (Closed) -> 0 (Open)
                    // We want progress 0 (Closed) -> 1 (Open)
                    val progress = 1f - (drawerOffset.value / drawerWidth).coerceIn(0f, 1f)
                    
                    // Simple slide: content moves left to reveal drawer, but stays flat (no scaling)
                    translationX = -size.width * 0.25f * progress
                    
                },
            topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        // Auto-focus when search opens
                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                        androidx.compose.material3.TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar emisora...") },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📻 RadioFlow+",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                // Timer indicator
                                if (timerActive) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Timer activo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (timerActive) "💤 ${SleepTimerManager.formatRemainingTime()}" else if (homeCountryName == "España") "Emisoras españolas" else "Radios de $homeCountryName",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (timerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = { 
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = ""
                            } else {
                                isSearchActive = false 
                            }
                        }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Buscar")
                        }
                        
                        // View Toggle (List/Grid)
                        IconButton(onClick = { isListView = !isListView }) {
                            Icon(
                                imageVector = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                contentDescription = if (isListView) "Ver como cuadrícula" else "Ver como lista",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                // Reliable open logic: Force reset to closed then open
                                scope.launch {
                                    // Ensure clean state for animation
                                    if (!isDrawerVisible) { 
                                        drawerOffset.snapTo(drawerWidth)
                                    }
                                    sheetState = SheetState.Settings 
                                }
                            },
                             modifier = Modifier.size(56.dp) // Increased touch target
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp) // Slightly larger icon
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            PlayerBar(
                currentStation = currentStation,
                isPlaying = isPlaying,
                isLoading = isLoading,
                isNetworkIssue = isNetworkIssue,
                accumulatedDelay = accumulatedDelay,

                onGoToLive = {
                     mediaController?.seekToDefaultPosition()
                     mediaController?.play()
                },
                onPlayPauseClick = {
                    mediaController?.let { controller ->
                        if (controller.isPlaying) {
                            controller.pause()
                        } else {
                            controller.play()
                        }
                    }
                },
                onStopClick = {
                    mediaController?.stop()
                    currentStation = null
                },
                onPreviousClick = {
                    com.radioandroid.util.AppLogger.action("Skip Previous tapped")
                    
                    val current = currentStation ?: run {
                        // If nothing playing, play last station
                        val last = NavigationUtils.getAllOrderedStations().lastOrNull()
                        if (last != null) playStation(last)
                        return@PlayerBar
                    }
                    
                    val prev = NavigationUtils.getPreviousStation(current)
                    
                    val useCountryFilter = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
                    val useGenreFilter = com.radioandroid.data.AppPreferences.isGenreNavigationEnabled
                    
                    if (useCountryFilter || useGenreFilter) {
                         val symbol = if (useCountryFilter) "🌍" else "🎵"
                         Toast.makeText(context, "⏮️ $symbol ${prev.name}", Toast.LENGTH_SHORT).show()
                    }
                    
                    playStation(prev)
                },
                onNextClick = {
                    com.radioandroid.util.AppLogger.action("Skip Next tapped")
                    
                    val current = currentStation ?: run {
                        // If nothing playing, play first station
                        val first = NavigationUtils.getAllOrderedStations().firstOrNull()
                        if (first != null) playStation(first)
                        return@PlayerBar
                    }
                    
                    val next = NavigationUtils.getNextStation(current)
                    
                    val useCountryFilter = com.radioandroid.data.AppPreferences.isSingleCountryNavigationEnabled
                    val useGenreFilter = com.radioandroid.data.AppPreferences.isGenreNavigationEnabled
                    
                    if (useCountryFilter || useGenreFilter) {
                         val symbol = if (useCountryFilter) "🌍" else "🎵"
                         Toast.makeText(context, "⏭️ $symbol ${next.name}", Toast.LENGTH_SHORT).show()
                    }
                    
                    playStation(next)
                },
                // Cast parameters
                isCastAvailable = isCastAvailable && (isPremium || com.radioandroid.BuildConfig.DEBUG),
                isCasting = isCasting,
                onCastClick = {
                    if (isPremium || com.radioandroid.BuildConfig.DEBUG) {
                        // Open Cast dialog
                        val castContext = castManager.getCastContext()
                        if (castContext != null) {
                            // Use CastButtonFactory to show device chooser
                            try {
                                val intent = android.content.Intent()
                                intent.action = "com.google.android.gms.cast.framework.ACTION_CAST_BUTTON_CLICKED"
                                context.sendBroadcast(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Abre la configuración de Cast", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        sheetState = SheetState.Premium
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (isListView) {
                // LIST VIEW
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Favorites Section
                    if (favoriteStations.isNotEmpty()) {
                        item {
                            Text(
                                text = "❤️ Favoritos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(favoriteStations, key = { "fav_${it.id}" }) { station ->
                           StationListItem(
                                station = station,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                isCurrentStation = currentStation?.id == station.id,
                                isFavorite = true,
                                isLocked = false,
                                // isLocked = station.country != homeCountryName, // DISABLED: Favorites are never locked

                                onFavoriteClick = { toggleFavorite(station) },
                                onClick = { 
                                    if (station.country != homeCountryName && false) { // Don't lock favorites? or keep lock?
                                         // Let's keep logic consistent: if foreign -> locked unless premium
                                         sheetState = SheetState.Premium
                                    } else {
                                        playStation(station) 
                                    }
                                }
                           )
                        }
                    }
                    
                    
                    // 2. CONDITIONAL Grouping: Genre vs Country
                    if (isGenreGroupedUI) {
                        // --- GENRE GROUPING MODE ---
                        if (stationsByGenre.isNotEmpty()) {
                            stationsByGenre.forEach { (genre, stations) ->
                                val isExpanded = expandedGenres[genre] == true
                                
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedGenres[genre] = !isExpanded }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "🎵 $genre",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                if (isExpanded) {
                                    items(stations, key = { it.id }) { station ->
                                        StationListItem(
                                            station = station,
                                            isPlaying = isPlaying,
                                            isLoading = isLoading,
                                            isCurrentStation = currentStation?.id == station.id,
                                            isFavorite = false,
                                            isLocked = false,
                                            onFavoriteClick = { toggleFavorite(station) },
                                            onClick = { playStation(station) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- STANDARD COUNTRY GROUPING MODE ---
                        
                        // 2. Local Country Section
                        if (localStations.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Bandera $homeCountryName".replace("Bandera España", "🇪🇸 España").replace("Bandera México", "🇲🇽 México").replace("Bandera United Kingdom", "🇬🇧 United Kingdom"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(localStations, key = { it.id }) { station ->
                                 StationListItem(
                                    station = station,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    isCurrentStation = currentStation?.id == station.id,
                                    isFavorite = false,
                                    isLocked = false,
                                    onFavoriteClick = { toggleFavorite(station) },
                                    onClick = { playStation(station) }
                                 )
                            }
                        }
    
                        // 3. International Sections (Grouped by Country)
                        if (internationalStationsGrouped.isNotEmpty()) {
                            internationalStationsGrouped.forEach { (country, stations) ->
                               val isExpanded = expandedCountries[country] == true
                               
                               item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedCountries[country] = !isExpanded }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${getCountryFlag(country)} $country",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                if (isExpanded) {
                                    items(stations, key = { it.id }) { station ->
                               StationListItem(
                                    station = station,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    isCurrentStation = currentStation?.id == station.id,
                                    isFavorite = false,
                                    isLocked = false,
                                    onFavoriteClick = { toggleFavorite(station) },
                                    onClick = { playStation(station) }
                                 )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // GRID VIEW
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                     // 1. Favorites
                     if (favoriteStations.isNotEmpty()) {
                         item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "❤️ Favoritos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                         }
                         items(favoriteStations, key = { "fav_grid_${it.id}" }) { station ->
                             StationCard(
                                station = station,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                isCurrentStation = currentStation?.id == station.id,
                                isFavorite = true,
                                isLocked = false,
                                // isLocked = station.country != homeCountryName, // DISABLED: Favorites are never locked

                                onFavoriteClick = { toggleFavorite(station) },
                                onClick = { 
                                   if (station.country != homeCountryName) {
                                        sheetState = SheetState.Premium
                                   } else {
                                        playStation(station) 
                                   }
                                }
                             )
                         }
                     }
                     
                     if (isGenreGroupedUI) {
                         // --- GENRE GROUPING MODE (GRID) ---
                         if (stationsByGenre.isNotEmpty()) {
                             stationsByGenre.forEach { (genre, stations) ->
                                 val isExpanded = expandedGenres[genre] == true
                                 
                                 item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                     Row(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .clickable { expandedGenres[genre] = !isExpanded }
                                             .padding(vertical = 8.dp),
                                         verticalAlignment = Alignment.CenterVertically,
                                         horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                         Text(
                                             text = "🎵 $genre",
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.secondary
                                         )
                                         Icon(
                                             imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                             contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                     }
                                 }
                                 
                                 if (isExpanded) {
                                     items(stations, key = { it.id }) { station ->
                                         StationCard(
                                            station = station,
                                            isPlaying = isPlaying,
                                            isLoading = isLoading,
                                            isCurrentStation = currentStation?.id == station.id,
                                            isFavorite = false,
                                            isLocked = false,
                                            onFavoriteClick = { toggleFavorite(station) },
                                            onClick = { playStation(station) }
                                         )
                                     }
                                 }
                             }
                         }
                     } else {
                         // --- STANDARD COUNTRY GROUPING MODE ---
                         
                         // 2. Local
                         if (localStations.isNotEmpty()) {
                             item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                 Text(
                                    text = "${getCountryFlag(homeCountryName)} $homeCountryName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                             }
                             items(localStations, key = { it.id }) { station ->
                                 StationCard(
                                    station = station,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    isCurrentStation = currentStation?.id == station.id,
                                    isFavorite = false,
                                    isLocked = false,
                                    onFavoriteClick = { toggleFavorite(station) },
                                    onClick = { playStation(station) }
                                 )
                             }
                         }
                         
                         // 3. International Sections (Grouped by Country)
                         if (internationalStationsGrouped.isNotEmpty()) {
                             internationalStationsGrouped.forEach { (country, stations) ->
                                 val isExpanded = expandedCountries[country] == true
                                 
                                 item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                     Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedCountries[country] = !isExpanded }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${getCountryFlag(country)} $country",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                 }
                                 
                                 if (isExpanded) {
                                     items(stations, key = { it.id }) { station ->
                                         StationCard(
                                            station = station,
                                            isPlaying = isPlaying,
                                            isLoading = isLoading,
                                            isCurrentStation = currentStation?.id == station.id,
                                            isFavorite = false,
                                            isLocked = false,
                                            onFavoriteClick = { toggleFavorite(station) },
                                            onClick = { playStation(station) }
                                         )
                                     }
                                 }
                             }
                         }
                     }
                }
            }
        } // End of inner Box (Scaffold content)
    } // End of Scaffold
        // Edge Drag Detector - Transparent strip on the right edge
        // Allows swiping left to open settings drawer without conflicting with list scrolling
        if (!isDrawerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(24.dp)
                    .draggable(
                        orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                        state = androidx.compose.foundation.gestures.rememberDraggableState { delta ->
                            // Dragging left (negative) reduces offset (moves towards 0/Open)
                            // Dragging right (positive) increases offset (moves towards drawerWidth/Closed)
                            // We only care about decreasing offset here (opening)
                            val newOffset = (drawerOffset.value + delta).coerceIn(0f, drawerWidth)
                            scope.launch { drawerOffset.snapTo(newOffset) }
                        },
                        onDragStopped = { velocity ->
                            // Velocity < -1000 means fast swipe to left (Open)
                            // Offset < drawerWidth * 0.7 means dragged more than 30% of the way (Open)
                            val shouldOpen = velocity < -1000 || drawerOffset.value < drawerWidth * 0.7f
                            
                            if (shouldOpen) {
                                // Trigger state change, LaunchedEffect will finish the animation to 0f
                                sheetState = SheetState.Settings
                            } else {
                                // Snap back to suspended/closed state
                                scope.launch {
                                    drawerOffset.animateTo(
                                        drawerWidth, 
                                        animationSpec = androidx.compose.animation.core.tween(300)
                                    )
                                }
                            }
                        }
                    )
            )
        }
        
        // Settings Drawer - overlays the entire UI with Spotify-style slide from right
        SettingsDrawer(
            isVisible = isDrawerVisible,
            drawerOffset = drawerOffset,
            drawerWidth = drawerWidth,
            onDismiss = { sheetState = SheetState.Hidden },
            onSleepTimerClick = { sheetState = SheetState.SleepTimer },
            onAlarmClick = { sheetState = SheetState.Alarm },
            onPremiumClick = { sheetState = SheetState.Premium },
            onDebugLogsClick = { sheetState = SheetState.DebugLogs },
            onHelpClick = { sheetState = SheetState.Help },
            // Legal moved to Help sheet
            onLegalClick = { }, // Deprecated in Drawer, logic moved to HelpSheet instantiation
            onThemeChanged = onThemeChanged,
            onAutoResumeChanged = {}, // Empty lambda, handled internally by SettingsDrawer
            onMetadataChanged = { }, // Metadata changes handled by preference observation in Service/UI
            onGenreGroupChanged = { isGenreGroupedUI = it } // Update local state when drawer toggle changes
        )
    } // End of main Box
}
