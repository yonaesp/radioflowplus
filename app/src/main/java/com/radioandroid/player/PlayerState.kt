package com.radioandroid.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerState {
    private val _accumulatedDelay = MutableStateFlow(0L)
    val accumulatedDelay: StateFlow<Long> = _accumulatedDelay.asStateFlow()
    
    fun setDelay(delay: Long) {
        _accumulatedDelay.value = delay
    }
}
