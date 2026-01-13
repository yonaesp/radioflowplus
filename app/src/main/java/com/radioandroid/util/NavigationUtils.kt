package com.radioandroid.util

import com.radioandroid.data.AppPreferences
import com.radioandroid.data.FavoritesRepository
import com.radioandroid.data.RadioStation
import com.radioandroid.data.RadioStations
import java.util.Locale

/**
 * Shared utility for verifying the current filtered/ordered station list.
 * Used by both UI (RadioApp) and Service (RadioMediaService) to ensure
 * unified navigation behavior (Next/Prev buttons).
 */
object NavigationUtils {

    /**
     * Returns the full list of stations respecting the current Visual Grouping (Category/Genre vs Country).
     * Order matches what the user sees on screen.
     */
    fun getAllOrderedStations(): List<RadioStation> {
        val favoriteIds = FavoritesRepository.favoriteIds.value
        val favoriteStations = RadioStations.stations.filter { it.id in favoriteIds }

        // Determine Home Country
        val locale = Locale.getDefault()
        val homeCountry = when (locale.country.uppercase()) {
            "MX" -> "México"
            "GB", "UK" -> "United Kingdom"
            else -> "España"
        }

        val isGenreGrouped = AppPreferences.isGenreGroupedUI

        if (isGenreGrouped) {
             // Logic: Favorites -> Genres (A-Z) -> Stations
             val genreStations = RadioStations.stations
                 .filter { it.id !in favoriteIds }
                 .groupBy { it.genre } // RadioStations.stations has pre-consolidated genres
                 .toSortedMap()
                 .values
                 .flatten()
             
             // Favorites first
             return (favoriteStations + genreStations).distinctBy { it.id }
        } else {
             // Logic: Favorites -> Local -> International
             val localStations = RadioStations.stations
                 .filter { it.country == homeCountry && it.id !in favoriteIds }
                 // Removed .sortedBy { it.name } to preserve popularity order from RadioStations.kt
                 
             val internationalStations = RadioStations.stations
                 .filter { it.country != homeCountry && it.id !in favoriteIds }
                 .groupBy { it.country }
                 .toSortedMap()
                 .values
                 .flatten()

             return (favoriteStations + localStations + internationalStations).distinctBy { it.id }
        }
    }
    
    fun getNextStation(currentStation: RadioStation): RadioStation {
        val list = getFilteredNavigationList(currentStation)
        if (list.isEmpty()) return currentStation
        
        val currentIndex = list.indexOfFirst { it.id == currentStation.id }
        if (currentIndex == -1) return list.first()
        
        val nextIndex = (currentIndex + 1) % list.size
        return list[nextIndex]
    }

    fun getPreviousStation(currentStation: RadioStation): RadioStation {
        val list = getFilteredNavigationList(currentStation)
        if (list.isEmpty()) return currentStation
        
        val currentIndex = list.indexOfFirst { it.id == currentStation.id }
        if (currentIndex == -1) return list.last()
        
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        return list[prevIndex]
    }
    
    /**
     * Applies enabled filters (Single Country, Genre, or Favorites Only) strictly on top of the ordered list.
     */
    private fun getFilteredNavigationList(currentStation: RadioStation): List<RadioStation> {
        val allStations = getAllOrderedStations()
        
        val useCountryFilter = AppPreferences.isSingleCountryNavigationEnabled
        val useGenreFilter = AppPreferences.isGenreNavigationEnabled
        val useFavoritesOnlyFilter = AppPreferences.isFavoritesOnlyNavigationEnabled
        
        // Favorites-Only filter takes precedence (Premium)
        if (useFavoritesOnlyFilter) {
            val favoriteIds = FavoritesRepository.favoriteIds.value
            return allStations.filter { it.id in favoriteIds }
        }
        
        if (!useCountryFilter && !useGenreFilter) return allStations
        
        return allStations.filter { station ->
            var matches = true
            if (useCountryFilter && station.country != currentStation.country) matches = false
            if (useGenreFilter && station.genre != currentStation.genre) matches = false // Strict Match
            matches
        }
    }
}
