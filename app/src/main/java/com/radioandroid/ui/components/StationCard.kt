package com.radioandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radioandroid.ui.theme.logoBackgroundColor
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.radioandroid.data.RadioStation

/**
 * Optimized StationCard with favorite support
 */
@Composable
fun StationCard(
    station: RadioStation,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    isCurrentStation: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pre-compute colors to avoid MaterialTheme lookups during scroll
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val favoriteColor = Color(0xFFE91E63) // Pink/Red for favorites
    
    Card(
        modifier = modifier
            .border(
                width = if (isCurrentStation) 2.dp else 0.dp,
                color = if (isCurrentStation) primaryColor else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        )
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                // Station logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(logoBackgroundColor()), // Adaptive background based on theme
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = station.logoResId),
                        contentDescription = station.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Animated playback waves below logo
                    if (isCurrentStation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        ) {
                            PlaybackWaves(
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                barCount = 5,
                                barWidth = 3.dp,
                                maxHeight = 18.dp,
                                spacing = 2.5.dp
                            )
                        }
                    }
                    
                    // Locked indicator
                    if (isLocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Premium Only",
                                tint = Color(0xFFFFD700), // Gold
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                // Station name
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrentStation) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrentStation) primaryColor else onSurfaceColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                // Genre tag
                if (station.genre.isNotEmpty()) {
                    Text(
                        text = station.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Favorite button (top-right corner)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (isFavorite) favoriteColor else onSurfaceVariantColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
