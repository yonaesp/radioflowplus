package com.radioandroid.premium

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.radioandroid.BuildConfig

/**
 * Manages premium status and feature limits.
 * When BuildConfig.PREMIUM_ENABLED is true, all users have premium access.
 */
object PremiumManager {
    
    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_premium"
    
    // Free tier limits
    const val FREE_FAVORITES_LIMIT = 5
    const val PREMIUM_FAVORITES_LIMIT = Int.MAX_VALUE
    
    // Internal state from billing/prefs (before override)
    private val _purchasedPremium = MutableStateFlow(false)
    
    // Public premium state - always check this
    private val _isPremiumState = MutableStateFlow(true) // Will be updated in init
    val isPremium: StateFlow<Boolean> = _isPremiumState
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * Check if premium is effectively enabled (either purchased or force-enabled via build config)
     */
    fun isEffectivelyPremium(): Boolean {
        return _purchasedPremium.value || BuildConfig.PREMIUM_ENABLED
    }
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _purchasedPremium.value = prefs.getBoolean(KEY_IS_PREMIUM, false)
        // Update the public state
        _isPremiumState.value = isEffectivelyPremium()
    }
    
    fun getFavoritesLimit(): Int {
        return if (isEffectivelyPremium()) PREMIUM_FAVORITES_LIMIT else FREE_FAVORITES_LIMIT
    }
    
    fun canAddMoreFavorites(currentCount: Int): Boolean {
        return currentCount < getFavoritesLimit()
    }
    
    // For testing purposes - will be replaced with Google Play Billing
    fun setPremiumStatus(isPremium: Boolean) {
        _purchasedPremium.value = isPremium
        _isPremiumState.value = isEffectivelyPremium()
        prefs.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply()
    }
    
    /**
     * Premium features list for UI
     */
    fun getPremiumFeatures(): List<String> {
        return listOf(
            "⏱️ Time-Shift (Pausa el Directo)",
            "⏰ Radio Alarma",
            "🚀 Favoritos ilimitados y más"
        )
    }
}
