package com.radioandroid.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import kotlinx.coroutines.launch

import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GridView


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.radioandroid.premium.PremiumManager
import com.radioandroid.timer.SleepTimerManager
import com.radioandroid.data.AppPreferences
import com.radioandroid.data.ThemeMode
import com.radioandroid.data.ThemePreferences

/**
 * Modern right-to-left sliding drawer for settings (Spotify-style)
 * Covers 90% of the screen with 10% visible main content on the right
 * Uses 2026 Jetpack Compose best practices with smooth animations
 */
@Composable
fun SettingsDrawer(
    isVisible: Boolean,
    drawerOffset: Animatable<Float, AnimationVector1D>,
    drawerWidth: Float,
    onDismiss: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onDebugLogsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLegalClick: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit = {},
    onAutoResumeChanged: (Boolean) -> Unit = {},
    onMetadataChanged: (Boolean) -> Unit = {},
    onGenreGroupChanged: (Boolean) -> Unit = {}
) {
    // Handle back press to dismiss drawer
    androidx.activity.compose.BackHandler(enabled = isVisible) {
        onDismiss()
    }
    
    val isPremium by PremiumManager.isPremium.collectAsStateWithLifecycle()
    val timerActive by SleepTimerManager.isActive.collectAsStateWithLifecycle()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(ThemePreferences.getThemeMode()) }
    var isAutoResumeEnabled by remember { mutableStateOf(AppPreferences.isAutoResumeEnabled) }
    var isSingleCountryNavEnabled by remember { mutableStateOf(AppPreferences.isSingleCountryNavigationEnabled) }
    var isTimeShiftEnabled by remember { mutableStateOf(AppPreferences.isTimeShiftEnabled) }
    
    // Screen width needed for child layout
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scope = rememberCoroutineScope()

    // Only show content if drawer is not fully closed (to avoid composition cost)
    // Only show content if drawer is not fully closed
    if (isVisible || drawerOffset.value < drawerWidth) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Unified Gesture Handling on Root
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Calculate visual left edge of the drawer
                            // Drawer is aligned to CenterEnd (Right), so it starts at (Screen - Width)
                            // drawerOffset.value adds translation to the right
                            // We need screenWidth in pixels
                            val screenWidthPx = configuration.screenWidthDp * density.density
                            val drawerLeftEdge = (screenWidthPx - drawerWidth) + drawerOffset.value
                            
                            // Check if tap is on the scrim (left of the drawer)
                            if (offset.x < drawerLeftEdge) {
                                // Animate close, THEN dismiss
                                scope.launch {
                                    drawerOffset.animateTo(drawerWidth, animationSpec = tween(250))
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Logic: Swipe fast enough OR drag past 30% to dismiss
                            val shouldClose = drawerOffset.value > drawerWidth * 0.2f
                            scope.launch {
                                if (shouldClose) {
                                    drawerOffset.animateTo(
                                        drawerWidth, 
                                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)
                                    )
                                    onDismiss()
                                } else {
                                    drawerOffset.animateTo(
                                        0f, 
                                        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (drawerOffset.value + dragAmount).coerceIn(0f, drawerWidth)
                            scope.launch { drawerOffset.snapTo(newOffset) }
                        }
                    )
                }
        ) {
            // Scrim overlay - Visual only (gestures handled by parent)
            val scrimAlpha = 0.6f * (1f - (drawerOffset.value / drawerWidth).coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha } // Use GPU alpha
                    .background(Color.Black)
            )
            
            // Drawer content - Visual (moved by graphicsLayer)
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(configuration.screenWidthDp.dp * 0.9f)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        translationX = drawerOffset.value
                    },
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Handle Status Bar Insets (Safe Area)
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ Configuración",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // ============================================================
                    // FREE SETTINGS - Most relevant first
                    // ============================================================
                    
                    // 1. Sleep Timer (FREE - Most used feature)
                    SettingsItem(
                        icon = Icons.Default.Bedtime,
                        title = "Sleep Timer",
                        subtitle = if (timerActive) "Activo: ${SleepTimerManager.formatRemainingTime()}" else "Apagar radio automáticamente",
                        onClick = onSleepTimerClick,
                        showBadge = timerActive,
                        badgeColor = MaterialTheme.colorScheme.primary
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 2. Theme (FREE - Visual preference)
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Tema",
                        subtitle = when (currentThemeMode) {
                            ThemeMode.SYSTEM -> "Automático (sistema)"
                            ThemeMode.LIGHT -> "Claro"
                            ThemeMode.DARK -> "Oscuro"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 3. Auto-Resume (FREE - For drivers)
                    SettingsSwitchItem(
                        icon = Icons.Default.DirectionsCar,
                        title = "Reanudar al Conectar",
                        subtitle = "Reproducir automáticamente en el coche (Android Auto/Bluetooth)",
                        checked = isAutoResumeEnabled,
                        onCheckedChange = { checked ->
                            isAutoResumeEnabled = checked
                            AppPreferences.isAutoResumeEnabled = checked
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 4. Navigation Options (FREE)
                    SettingsSwitchItem(
                        icon = Icons.Default.Public,
                        title = "Navegar por País",
                        subtitle = "Siguiente/anterior solo dentro del país actual",
                        checked = isSingleCountryNavEnabled,
                        onCheckedChange = { checked ->
                            isSingleCountryNavEnabled = checked
                            AppPreferences.isSingleCountryNavigationEnabled = checked
                        }
                    )
                    
                    var isGenreNavEnabled by remember { mutableStateOf(AppPreferences.isGenreNavigationEnabled) }
                    SettingsSwitchItem(
                        icon = Icons.Default.MusicNote,
                        title = "Navegar por Categoría",
                        subtitle = "Siguiente/anterior solo del mismo estilo",
                        checked = isGenreNavEnabled,
                        onCheckedChange = { checked ->
                            isGenreNavEnabled = checked
                            AppPreferences.isGenreNavigationEnabled = checked
                        }
                    )
                    
                    var isGenreGroupingEnabled by remember { mutableStateOf(AppPreferences.isGenreGroupedUI) }
                    SettingsSwitchItem(
                        icon = Icons.Default.GridView,
                        title = "Agrupar por Categoría",
                        subtitle = "Mostrar emisoras divididas por categoría (Música, Noticias, etc.)",
                        checked = isGenreGroupingEnabled,
                        onCheckedChange = { checked ->
                            isGenreGroupingEnabled = checked
                            AppPreferences.isGenreGroupedUI = checked
                            onGenreGroupChanged(checked)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // 5. Help (FREE)
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Ayuda",
                        subtitle = "Cómo usar las funciones y ver logs",
                        onClick = onHelpClick
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // ============================================================
                    // PREMIUM SETTINGS - At the bottom
                    // ============================================================
                    
                    // PRO: Radio Alarma
                    SettingsItem(
                        icon = Icons.Default.Alarm,
                        title = "Radio Alarma",
                        subtitle = "Despertar con tu emisora favorita",
                        onClick = onAlarmClick,
                        isPremiumOnly = true,
                        isPremiumUser = isPremium
                    )
                    
                    // PRO: Time-Shift
                    SettingsSwitchItem(
                        icon = Icons.Default.Pause,
                        title = "Time-Shift",
                        subtitle = "Pausar y continuar donde lo dejaste (máx. 2 min)",
                        checked = isTimeShiftEnabled,
                        enabled = isPremium || com.radioandroid.BuildConfig.DEBUG,
                        onCheckedChange = { checked ->
                            if (isPremium || com.radioandroid.BuildConfig.DEBUG) {
                                isTimeShiftEnabled = checked
                                AppPreferences.isTimeShiftEnabled = checked
                            } else {
                                onPremiumClick()
                            }
                        },
                        isPremiumOnly = true
                    )
                    
                    // Premium info section - always visible
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumBanner(onClick = onPremiumClick, isPremiumUser = isPremium)
                    
                    // Extra bottom padding for navigation bar
                    Spacer(modifier = Modifier.height(48.dp))
                }
                
                // Theme selection dialog
                if (showThemeDialog) {
                    ThemeSelectionDialog(
                        currentMode = currentThemeMode,
                        onModeSelected = { newMode ->
                            currentThemeMode = newMode
                            onThemeChanged(newMode)
                        },
                        onDismiss = { showThemeDialog = false }
                    )
                }
            }
        }
    }
}

// Private Components Helpers

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPremiumOnly: Boolean = false,
    isPremiumUser: Boolean = false,
    showBadge: Boolean = false,
    badgeColor: Color = Color.Green
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (showBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "●",
                        color = badgeColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isPremiumOnly) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRO",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color(0xFFFFD700).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    isPremiumOnly: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isPremiumOnly) {
                    Text(
                        text = "PRO",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                Color(0xFFFFD700).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit, isPremiumUser: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFFD700).copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isPremiumUser) "Eres Premium ⭐" else "Hazte Premium",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )
            Text(
                text = if (isPremiumUser) "Gracias por tu apoyo" else "Desbloquea todas las funciones PRO",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFFFD700)
        )
    }
}
