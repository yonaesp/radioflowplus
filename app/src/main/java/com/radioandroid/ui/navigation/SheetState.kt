package com.radioandroid.ui.navigation

import androidx.compose.runtime.Immutable

/**
 * Sealed class representing all possible sheet states in the app.
 * This modern approach replaces multiple boolean flags with a single state holder.
 * 
 * Benefits:
 * - Type-safe: compiler ensures all states are handled
 * - No invalid states: only one sheet can be active at a time
 * - Easy to extend: add new sheets by adding new data classes
 * - Better maintainability: single source of truth for sheet navigation
 */
@Immutable
sealed class SheetState {
    /**
     * No sheet is currently visible
     */
    data object Hidden : SheetState()
    
    /**
     * Settings sheet is visible (main menu)
     */
    data object Settings : SheetState()
    
    /**
     * Sleep Timer sheet is visible.
     * When closed, returns to Settings.
     */
    data object SleepTimer : SheetState()
    
    /**
     * Alarm configuration sheet is visible.
     * When closed, returns to Settings.
     */
    data object Alarm : SheetState()
    
    /**
     * Premium info/subscription sheet is visible.
     */
    data object Premium : SheetState()
    
    /**
     * Debug logs viewer sheet is visible.
     * When closed, returns to Settings.
     */
    data object DebugLogs : SheetState()
    
    /**
     * Help/How-to-use sheet is visible.
     * When closed, returns to Settings.
     */
    data object Help : SheetState()
    
    /**
     * Legal & Information sheet is visible.
     */
    data object Legal : SheetState()
    
    /**
     * Returns the parent sheet to navigate back to when this sheet is closed.
     * Returns Hidden if the sheet has no parent (is a root-level sheet).
     */
    fun parentSheet(): SheetState = when (this) {
        is Hidden -> Hidden
        is Settings -> Hidden
        is SleepTimer -> Settings
        is Alarm -> Settings
        is Premium -> Settings
        is DebugLogs -> Settings
        is Help -> Settings
        is Legal -> Settings
    }
    
    /**
     * Returns true if this sheet is a child of Settings (requires back navigation to Settings)
     */
    fun isSettingsChild(): Boolean = when (this) {
        is SleepTimer, is Alarm, is DebugLogs, is Help, is Legal -> true
        else -> false
    }
}
