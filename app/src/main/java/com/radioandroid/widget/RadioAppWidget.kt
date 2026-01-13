package com.radioandroid.widget

import android.content.Context
import android.content.Intent
import com.radioandroid.R
import com.radioandroid.service.RadioMediaService
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build

import androidx.glance.action.actionStartActivity
import com.radioandroid.MainActivity

object RadioAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetUI()
        }
    }

    @androidx.compose.runtime.Composable
    fun WidgetUI() {
        // State
        val isPlaying = currentState(key = WidgetStateManager.KEY_IS_PLAYING) ?: false
        val isLoading = currentState(key = WidgetStateManager.KEY_IS_LOADING) ?: false
        val stationName = currentState(key = WidgetStateManager.KEY_STATION_NAME) ?: "RadioFlow+"
        val logoRes = currentState(key = WidgetStateManager.KEY_STATION_LOGO) ?: R.drawable.ic_launcher_foreground
        val widgetColorInt = currentState(key = WidgetStateManager.KEY_WIDGET_COLOR) ?: 0xFF1C1B1F.toInt()

        // Dynamic Colors logic
        // If color is very dark or black (default), use standard dark theme
        // Otherwise use the extracted color
        val backgroundColor = Color(widgetColorInt)
        
        // Calculate content color based on luminance
        // Simple formula: if background is dark, use white; else black
        val isDarkBackground = androidx.core.graphics.ColorUtils.calculateLuminance(widgetColorInt) < 0.5
        val contentColor = if (isDarkBackground) Color.White else Color.Black
        val secondaryContentColor = contentColor.copy(alpha = 0.8f)

        // Main Container
        // Use a Box to allow background image if we wanted, but solid color is cleaner
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Logo with Frame
            // If logoRes is generic (launcher), show it smaller or different
            val isGenericLogo = logoRes == R.drawable.ic_launcher_foreground || logoRes == com.radioandroid.R.mipmap.ic_launcher
            
            Image(
                provider = ImageProvider(logoRes),
                contentDescription = null,
                contentScale = androidx.glance.layout.ContentScale.Fit,
                modifier = GlanceModifier
                    .size(if (isGenericLogo) 40.dp else 48.dp) // Reduced to fit in 64dp height
                    .cornerRadius(12.dp)
                    .background(Color.Transparent) // Removed white background
                    .padding(0.dp)
            )

            // Info (Title)
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (stationName == "Radio Flow") "RadioFlow+" else stationName,
                    style = TextStyle(
                        color = ColorProvider(contentColor),
                        fontSize = 15.sp, // Kept 15sp as requested
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2 // Allow 2 lines as requested
                )
                
                // Status / Subtitle
                Row(
                    modifier = GlanceModifier.padding(top = 2.dp), // Add slight spacing
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small status indicator dot
                    if (isPlaying || isLoading) {
                         Text(
                            text = "• ", 
                            style = TextStyle(
                                color = ColorProvider(if (isLoading) Color.Yellow else Color.Green), 
                                fontSize = 12.sp
                            )
                        )
                    }
                    
                    Text(
                        text = when {
                            isLoading -> "Cargando..."
                            isPlaying -> "En directo"
                            stationName == "Radio Flow" -> "Toca para abrir"
                            else -> "Pausado"
                        },
                        style = TextStyle(
                            color = ColorProvider(secondaryContentColor),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            // Controls (Play/Pause + Next)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Play/Pause Button
                 val playIcon = if (isLoading) R.drawable.ic_loading 
                                else if (isPlaying) R.drawable.ic_pause 
                                else R.drawable.ic_play
                 
                 // Circle background for button to ensure visibility
                 // Calculate button bg color (inverse of main bg)
                 val buttonBgColor = if (isDarkBackground) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
                 
                 Image(
                     provider = ImageProvider(playIcon),
                     contentDescription = "Alternar Reproducción",
                     modifier = GlanceModifier
                         .size(48.dp)
                         .background(buttonBgColor)
                         .cornerRadius(24.dp)
                         .padding(12.dp) // Padding inside the circle
                         .clickable(onClick = actionRunCallback<TogglePlaybackAction>())
                 )
                 
                 // Next Button
                 Image(
                     provider = ImageProvider(R.drawable.ic_next),
                     contentDescription = "Siguiente",
                     modifier = GlanceModifier
                         .size(40.dp)
                         .padding(start = 12.dp)
                         .cornerRadius(20.dp)
                         .background(buttonBgColor.copy(alpha = 0.05f))
                         .clickable(onClick = actionRunCallback<NextStationAction>())
                 )
            }
        }
    }
}

class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, RadioMediaService::class.java).apply {
            action = "ACTION_TOGGLE_PLAYBACK"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

class NextStationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, RadioMediaService::class.java).apply {
            action = "ACTION_NEXT_STATION"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
