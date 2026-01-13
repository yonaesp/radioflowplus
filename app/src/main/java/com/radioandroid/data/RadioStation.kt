package com.radioandroid.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * Data class representing a radio station.
 * 
 * @Immutable annotation helps Compose compiler optimize recompositions.
 * Uses local drawable resources for offline icon support.
 */
@Immutable
data class RadioStation(
    val id: Int,
    val name: String,
    val streamUrl: String,
    @DrawableRes val logoResId: Int,
    val genre: String = "",
    val country: String = "España"
)
