package com.radioandroid.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.radioandroid.service.RadioMediaService
import com.radioandroid.data.RadioStations

class RadioWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RadioAppWidget
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        
        // Try to connect to MediaService to get current state
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, RadioMediaService::class.java)
            )
            
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    
                    // Get current playback state
                    val isPlaying = controller.isPlaying
                    val currentMediaItem = controller.currentMediaItem
                    
                    if (currentMediaItem != null) {
                        // Find station from mediaId
                        val stationId = currentMediaItem.mediaId.toIntOrNull()
                        val station = RadioStations.stations.find { it.id == stationId }
                        
                        if (station != null) {
                            // Update widget with real station data
                            WidgetStateManager.updateWidget(
                                context,
                                isPlaying = isPlaying,
                                isLoading = false,
                                stationName = station.name,
                                logoRes = station.logoResId,
                                widgetColor = 0xFF1C1B1F.toInt()
                            )
                        } else {
                            // Unknown station, show placeholder
                            setPlaceholder(context)
                        }
                    } else {
                        // No media loaded, show placeholder
                        setPlaceholder(context)
                    }
                    
                    // Release controller
                    MediaController.releaseFuture(controllerFuture)
                } catch (e: Exception) {
                    // Service not available, show placeholder
                    setPlaceholder(context)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            // Service connection failed, show placeholder
            setPlaceholder(context)
        }
    }
    
    private fun setPlaceholder(context: Context) {
        WidgetStateManager.updateWidget(
            context, 
            isPlaying = false, 
            isLoading = false,
            stationName = "Radio Flow",
            logoRes = com.radioandroid.R.drawable.ic_launcher_foreground,
            widgetColor = 0xFF1C1B1F.toInt()
        )
    }
}
