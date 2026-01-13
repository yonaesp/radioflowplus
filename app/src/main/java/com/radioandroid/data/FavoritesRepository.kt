package com.radioandroid.data

import android.content.Context
import android.content.SharedPreferences
import com.radioandroid.premium.PremiumManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing favorite radio stations
 * Persists favorites using SharedPreferences
 */
object FavoritesRepository {
    
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_station_ids"
    
    private lateinit var prefs: SharedPreferences
    
    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFavorites()
    }
    
    private fun loadFavorites() {
        val savedIds = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoriteIds.value = savedIds.mapNotNull { it.toIntOrNull() }.toSet()
    }
    
    private fun saveFavorites() {
        val idsAsStrings = _favoriteIds.value.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_FAVORITES, idsAsStrings).apply()
    }
    
    fun isFavorite(stationId: Int): Boolean {
        return stationId in _favoriteIds.value
    }
    
    fun toggleFavorite(stationId: Int): ToggleResult {
        val currentFavorites = _favoriteIds.value.toMutableSet()
        
        return if (stationId in currentFavorites) {
            // Remove from favorites
            currentFavorites.remove(stationId)
            _favoriteIds.value = currentFavorites
            saveFavorites()
            ToggleResult.Removed
        } else {
            // Check limit before adding
            if (!PremiumManager.canAddMoreFavorites(currentFavorites.size)) {
                ToggleResult.LimitReached(PremiumManager.getFavoritesLimit())
            } else {
                currentFavorites.add(stationId)
                _favoriteIds.value = currentFavorites
                saveFavorites()
                ToggleResult.Added
            }
        }
    }
    
    fun getFavoriteStations(): List<RadioStation> {
        val ids = _favoriteIds.value
        return RadioStations.stations.filter { it.id in ids }
    }
    
    fun getNonFavoriteStations(): List<RadioStation> {
        val ids = _favoriteIds.value
        return RadioStations.stations.filter { it.id !in ids }
    }
    
    /**
     * Get all stations sorted with favorites first
     */
    fun getStationsSortedByFavorites(): List<RadioStation> {
        val ids = _favoriteIds.value
        return RadioStations.stations.sortedByDescending { it.id in ids }
    }
    
    fun getFavoritesCount(): Int = _favoriteIds.value.size
    
    sealed class ToggleResult {
        object Added : ToggleResult()
        object Removed : ToggleResult()
        data class LimitReached(val limit: Int) : ToggleResult()
    }
}
