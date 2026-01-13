package com.radioandroid.ui.components

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radioandroid.ui.theme.logoBackgroundColor
import com.radioandroid.data.RadioStations
import com.radioandroid.premium.PremiumManager
import com.radioandroid.timer.RadioAlarmManager
import java.util.Calendar
import androidx.compose.material3.TimePicker
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSheet(
    onDismiss: () -> Unit,
    onPremiumClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Handle Back press inside the sheet with animation (2026 smooth transition)
    androidx.activity.compose.BackHandler {
        scope.launch { sheetState.hide() }.invokeOnCompletion { 
            onNavigateBack() 
        }
    }
    val scrollState = rememberScrollState()
    val isPremium by PremiumManager.isPremium.collectAsStateWithLifecycle()
    val isAlarmSet by RadioAlarmManager.isAlarmSet.collectAsStateWithLifecycle()
    
    // Track permission state with a trigger for manual refresh
    var permissionCheckTrigger by remember { mutableStateOf(0) }
    
    // Check permission - recheck when trigger changes
    val hasAlarmPermission = remember(permissionCheckTrigger) {
        RadioAlarmManager.canScheduleExactAlarms(context)
    }
    
    // Check Full Screen Intent permission for Android 14+ (needed for lock screen visibility)
    val hasFullScreenIntentPermission = remember(permissionCheckTrigger) {
        if (Build.VERSION.SDK_INT >= 34) { // Android 14
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            true // Not required or auto-granted on older versions
        }
    }
    
    // Get Android version for display
    val androidVersion = Build.VERSION.SDK_INT
    
    // Default to next minute for quicker testing
    val now = remember { 
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }
    }
    
    val timePickerState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )
    
    // Default to first station if no favorites
    var selectedStationId by remember { mutableStateOf(RadioStations.stations.firstOrNull()?.id ?: 1) }
    
    // Repeat days state
    val daysOfWeek = remember {
        listOf(
            Calendar.MONDAY to "L",
            Calendar.TUESDAY to "M",
            Calendar.WEDNESDAY to "X",
            Calendar.THURSDAY to "J",
            Calendar.FRIDAY to "V",
            Calendar.SATURDAY to "S",
            Calendar.SUNDAY to "D"
        )
    }
    
    // Initialize with loaded repeat days if available (would need exposure in ViewModel/Manager)
    // For now starts empty (one-time)
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    
    // Country and Station Filtering
    var selectedCountry by remember { mutableStateOf("España") } // Default to primary market
    
    // Process countries list
    val availableCountries = remember {
        listOf("Todos") + RadioStations.stations.map { it.country }.distinct().sorted()
    }
    
    val filteredStations = remember(selectedCountry) {
        if (selectedCountry == "Todos") RadioStations.stations
        else RadioStations.stations.filter { it.country == selectedCountry }
    }
    
    // Check alarm volume
    var isAlarmVolumeLow by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val checkAlarmVolume = {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val current = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            isAlarmVolumeLow = current < (max * 0.6)
        } catch (e: Exception) {
            isAlarmVolumeLow = false
        }
    }
    
    // Check volume on resume (in case user changed it in settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAlarmVolume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkAlarmVolume() // Initial check
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onNavigateBack, // Back press returns to settings
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ Radio Alarma",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
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
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isPremium) {
                // Non-premium - show upsell
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Función Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Despierta cada mañana con tu emisora de radio favorita. El volumen sube gradualmente para no asustarte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        onDismiss()
                        onPremiumClick()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver Premium")
                }
            } else if (!hasAlarmPermission) {
                // Premium but no alarm permission - show permission request
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Permiso Necesario",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Para programar alarmas exactas, necesitas activar el permiso en Configuración del sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Info box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "¿Por qué este permiso?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Permite que la alarma suene a la hora exacta, incluso con la app cerrada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // Open system settings for alarm permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir Configuración", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Configuración")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { permissionCheckTrigger++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ya lo activé, verificar")
                }
            } else if (!hasFullScreenIntentPermission) {
                // Premium, has alarm permission, but NO full screen intent (Android 14+)
               Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Permiso de Pantalla Completa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "En Android 14+, para que la alarma se muestre sobre la pantalla de bloqueo, necesitas activar este permiso especial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // Open system settings for full screen intent
                        if (Build.VERSION.SDK_INT >= 34) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir Configuración", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Configuración")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { permissionCheckTrigger++ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ya lo activé, verificar")
                }
            } else if (isAlarmSet) {
                // Alarm is set - show current alarm
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = RadioAlarmManager.formatAlarmTime(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = RadioAlarmManager.getAlarmStation()?.name ?: "Emisora",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Show repeat days
                Text(
                    text = RadioAlarmManager.formatRepeatDays(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Fade-in indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fade-in: 5% → 100% en 30s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = {
                        RadioAlarmManager.cancelAlarm(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AlarmOff, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelar Alarma")
                }
            } else {
                // Premium user with permission - show alarm setup
                
                // Time input (more compact than TimePicker)
                // Native Time Picker (Analog Clock)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Day Selection (Recurrence)
                Text(
                    text = "Repetir:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { (day, label) ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    val newSelection = selectedDays.toMutableSet()
                                    if (isSelected) newSelection.remove(day)
                                    else newSelection.add(day)
                                    selectedDays = newSelection
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Station selector
                Text(
                    text = "Emisora para despertar:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Country Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCountries.forEach { country ->
                        val isSelected = selectedCountry == country
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCountry = country }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = country,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Station List (Vertical Column)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredStations.forEach { station ->
                        val isSelected = selectedStationId == station.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedStationId = station.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Station Logo/Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(logoBackgroundColor(), RoundedCornerShape(8.dp)), // Adaptive background based on theme
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = station.logoResId),
                                    contentDescription = null,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${station.genre} • ${station.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fade-in info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Fade-in automático",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "El volumen sube gradualmente (5% → 100%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                

                
                if (isAlarmVolumeLow) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                            .clickable {
                                // Attempt to boost volume on click
                                try {
                                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                                    val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, (max * 0.8).toInt(), 0)
                                    checkAlarmVolume() // Re-check
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Volumen bajo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "El volumen de la alarma es bajo. Toca para subirlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        RadioAlarmManager.scheduleAlarm(
                            context = context,
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            stationId = selectedStationId,
                            repeatDays = selectedDays.toList()
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Alarm, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Programar Alarma")
                }
            }
        }
    }
}
