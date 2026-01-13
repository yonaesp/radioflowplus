package com.radioandroid.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object AppPreferences {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_AUTO_RESUME = "auto_resume"
    private const val KEY_LAST_STATION_ID = "last_station_id"
    private const val KEY_SINGLE_COUNTRY_NAV = "single_country_navigation"
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var isAutoResumeEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESUME, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RESUME, value).apply()
        
    var lastStationId: Int
        get() = prefs.getInt(KEY_LAST_STATION_ID, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_STATION_ID, value).apply()
        
    private const val KEY_LAST_PLAYBACK_TIME = "last_playback_time"
    private const val KEY_WAS_PLAYING = "was_playing"
    
    var lastPlaybackTime: Long
        get() = prefs.getLong(KEY_LAST_PLAYBACK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PLAYBACK_TIME, value).apply()
        
    var wasPlaying: Boolean
        get() = prefs.getBoolean(KEY_WAS_PLAYING, false)
        set(value) = prefs.edit().putBoolean(KEY_WAS_PLAYING, value).apply()
    
    var isSingleCountryNavigationEnabled: Boolean
        get() = prefs.getBoolean(KEY_SINGLE_COUNTRY_NAV, false)
        set(value) = prefs.edit().putBoolean(KEY_SINGLE_COUNTRY_NAV, value).apply()
        
    private const val KEY_GENRE_NAV = "genre_navigation"
    
    var isGenreNavigationEnabled: Boolean
        get() = prefs.getBoolean(KEY_GENRE_NAV, false)
        set(value) = prefs.edit().putBoolean(KEY_GENRE_NAV, value).apply()
        
    private const val KEY_GENRE_GROUPED_UI = "genre_grouped_ui"
    
    var isGenreGroupedUI: Boolean
        get() = prefs.getBoolean(KEY_GENRE_GROUPED_UI, false)
        set(value) = prefs.edit().putBoolean(KEY_GENRE_GROUPED_UI, value).apply()
        
    // Time-Shift (Premium)
    private const val KEY_TIME_SHIFT = "time_shift"
    
    var isTimeShiftEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIME_SHIFT, false)
        set(value) = prefs.edit().putBoolean(KEY_TIME_SHIFT, value).apply()
        
    

    
    // Favorites Only Navigation (Premium)
    private const val KEY_FAVORITES_ONLY_NAV = "favorites_only_navigation"
    
    var isFavoritesOnlyNavigationEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAVORITES_ONLY_NAV, false)
        set(value) = prefs.edit().putBoolean(KEY_FAVORITES_ONLY_NAV, value).apply()

    // Station Change Overlay
    private const val KEY_STATION_CHANGE_OVERLAY = "station_change_overlay"
    
    var isStationChangeOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_STATION_CHANGE_OVERLAY, true) // Enabled by default
        set(value) = prefs.edit().putBoolean(KEY_STATION_CHANGE_OVERLAY, value).apply()

        

}
