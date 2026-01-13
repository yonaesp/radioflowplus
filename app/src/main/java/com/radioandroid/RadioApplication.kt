package com.radioandroid

import android.app.Application
import com.radioandroid.cast.CastManager
import com.radioandroid.data.AppPreferences
import com.radioandroid.data.FavoritesRepository
import com.radioandroid.data.ThemePreferences
import com.radioandroid.premium.PremiumManager
import com.radioandroid.timer.RadioAlarmManager
import com.radioandroid.timer.SleepTimerManager
import com.radioandroid.util.AppLogger

/**
 * Main Application class.
 * Initializes all singletons and managers at app startup.
 * This prevents crashes when components (like Services or Activities) 
 * access these managers before MainActivity is created.
 */
class RadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Logger
        AppLogger.init(this)
        
        // Initialize Data Repositories & Preferences
        AppPreferences.init(this)
        ThemePreferences.init(this)
        FavoritesRepository.init(this)
        
        // Initialize Managers
        PremiumManager.init(this)
        SleepTimerManager.init(this)
        RadioAlarmManager.init(this)
        
        // Initialize Cast Manager (safe - handles missing Play Services)
        try {
            CastManager.getInstance(this).initialize()
        } catch (e: Exception) {
            AppLogger.w("RadioApplication", "Cast not available: ${e.message}")
        }
        
        AppLogger.d("RadioApplication", "All managers initialized successfully")
    }
}
