package com.radioandroid.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.radioandroid.data.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetStateManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    val KEY_IS_PLAYING = booleanPreferencesKey("isPlaying")
    val KEY_IS_LOADING = booleanPreferencesKey("isLoading")
    val KEY_STATION_NAME = stringPreferencesKey("stationName")
    val KEY_STATION_LOGO = androidx.datastore.preferences.core.intPreferencesKey("stationLogo")
    val KEY_WIDGET_COLOR = androidx.datastore.preferences.core.intPreferencesKey("widgetColor")

    fun updateState(context: Context, station: RadioStation?, isPlaying: Boolean, isLoading: Boolean = false, color: Int = 0xFF1C1B1F.toInt()) {
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RadioAppWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[KEY_IS_PLAYING] = isPlaying
                        prefs[KEY_IS_LOADING] = isLoading
                        prefs[KEY_STATION_NAME] = station?.name ?: "Radio Flow"
                        prefs[KEY_STATION_LOGO] = station?.logoResId ?: com.radioandroid.R.drawable.ic_launcher_foreground
                        prefs[KEY_WIDGET_COLOR] = color
                    }
                    RadioAppWidget.update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateWidget(
        context: Context,
        isPlaying: Boolean,
        isLoading: Boolean,
        stationName: String,
        logoRes: Int,
        widgetColor: Int
    ) {
        scope.launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(RadioAppWidget::class.java)
                
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[KEY_IS_PLAYING] = isPlaying
                        prefs[KEY_IS_LOADING] = isLoading
                        prefs[KEY_STATION_NAME] = stationName
                        prefs[KEY_STATION_LOGO] = logoRes
                        prefs[KEY_WIDGET_COLOR] = widgetColor
                    }
                    RadioAppWidget.update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
