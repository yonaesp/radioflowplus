package com.radioandroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Returns the appropriate background color for radio station logos
 * based on the current theme.
 * 
 * - Light theme: Soft white (0xFFF5F5F5)
 * - Dark theme: Soft dark (0xFF2A2A2A)
 */
@Composable
fun logoBackgroundColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        // Tema claro - fondo blanco suave
        Color(0xFFF5F5F5)
    } else {
        // Tema oscuro - fondo gris oscuro suave
        Color(0xFF2A2A2A)
    }
}
