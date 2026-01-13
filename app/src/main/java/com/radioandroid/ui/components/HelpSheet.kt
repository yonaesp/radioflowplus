package com.radioandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(
    onDismiss: () -> Unit,
    onDebugLogsClick: () -> Unit = {},
    onLegalClick: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Handle Back press inside the sheet with animation
    androidx.activity.compose.BackHandler {
        scope.launch { sheetState.hide() }.invokeOnCompletion { 
            onNavigateBack() 
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onNavigateBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📖 Cómo usar RadioFlow+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Section Header: GRATIS
            SectionHeader(title = "🆓 Funciones Gratuitas", color = Color(0xFF4CAF50))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Voice Control Section (FREE)
            FeatureSection(
                icon = Icons.Default.Mic,
                iconColor = Color(0xFF4CAF50),
                title = "Control por Voz",
                isPro = false,
                description = "Controla la radio con tu voz usando Google Assistant o Gemini. Perfecto para conducir.",
                examples = listOf(
                    "\"Hey Google, pon Los 40\" → Reproduce Los 40",
                    "\"Hey Google, pausa la radio\" → Pausa",
                    "\"Hey Google, siguiente emisora\" → Cambia emisora"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sleep Timer (FREE)
            FeatureSection(
                icon = Icons.Default.Timer,
                iconColor = Color(0xFF9C27B0),
                title = "Sleep Timer",
                isPro = false,
                description = "Apaga la radio automáticamente. Ideal para dormir con tu emisora favorita.",
                examples = listOf(
                    "Elige 15, 30 o 60 minutos",
                    "La radio se apaga sola al terminar",
                    "Configura desde ⚙️ → Sleep Timer"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Theme (FREE)
            FeatureSection(
                icon = Icons.Default.Palette,
                iconColor = Color(0xFF2196F3),
                title = "Personalización de Tema",
                isPro = false,
                description = "Elige entre tema claro, oscuro o automático según el sistema.",
                examples = listOf(
                    "Oscuro: Ideal para usar de noche",
                    "Claro: Mejor visibilidad al sol",
                    "Automático: Cambia según tu móvil"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Auto-Resume (FREE)
            FeatureSection(
                icon = Icons.Default.DirectionsCar,
                iconColor = Color(0xFFFF5722),
                title = "Auto-Reanudación",
                isPro = false,
                description = "La radio se reanuda automáticamente al conectar Bluetooth o Android Auto.",
                examples = listOf(
                    "Arranca el coche → La radio empieza sola",
                    "Conecta auriculares → Reanuda donde lo dejaste",
                    "Perfecto para conductores"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Navigation Options (FREE)
            FeatureSection(
                icon = Icons.Default.Public,
                iconColor = Color(0xFF009688),
                title = "Navegación Inteligente",
                isPro = false,
                description = "Personaliza cómo cambias entre emisoras con los botones ⏮️ ⏭️.",
                examples = listOf(
                    "Por país: Solo emisoras del mismo país",
                    "Por categoría: Solo del mismo estilo (Rock, Pop, etc.)",
                    "Agrupar: Ver emisoras organizadas por tipo"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Smart Network (FREE)
            FeatureSection(
                icon = Icons.Default.NetworkCheck,
                iconColor = Color(0xFFFF9800),
                title = "Gestión de Conexión",
                isPro = false,
                description = "La app detecta problemas de red y se recupera automáticamente.",
                examples = listOf(
                    "📶 \"Señal baja\" → Te avisa del problema",
                    "Reanuda sola cuando vuelve la conexión",
                    "No consume batería esperando"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Auto-Skip (FREE)
            FeatureSection(
                icon = Icons.Default.SkipNext,
                iconColor = Color(0xFF4CAF50),
                title = "Auto-Skip Inteligente",
                isPro = false,
                description = "Si una emisora no funciona, salta automáticamente a la siguiente.",
                examples = listOf(
                    "URL rota → Salta en menos de 1 segundo",
                    "Servidor lento → Espera hasta 8 segundos",
                    "Sin conexión → Espera a que vuelva la red"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Audio Focus (FREE)
            FeatureSection(
                icon = Icons.Default.PlayArrow,
                iconColor = Color(0xFF2196F3),
                title = "Audio Focus Inteligente",
                isPro = false,
                description = "La radio se adapta cuando otras apps reproducen audio.",
                examples = listOf(
                    "GPS/Notificaciones → Baja volumen 80%",
                    "Videos/Llamadas → Pausa completamente",
                    "Vuelve al directo automáticamente"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Favorites (FREE with limit)
            FeatureSection(
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFE91E63),
                title = "Favoritos",
                isPro = false,
                description = "Guarda tus emisoras favoritas para acceder más rápido.",
                examples = listOf(
                    "Pulsa ❤️ en cualquier emisora",
                    "Favoritos aparecen arriba de la lista",
                    "Hasta 5 favoritos gratis"
                )
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Section Header: PRO
            SectionHeader(title = "⭐ Funciones Premium", color = Color(0xFFFFD700))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Radio Alarm (PRO)
            FeatureSection(
                icon = Icons.Default.Alarm,
                iconColor = Color(0xFFF44336),
                title = "Radio Alarma",
                isPro = true,
                description = "Despierta con tu emisora favorita. El volumen sube gradualmente.",
                examples = listOf(
                    "Elige hora y emisora favorita",
                    "Fade-in suave del volumen",
                    "Funciona con pantalla bloqueada",
                    "Botón grande para apagar fácilmente"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Time-Shift (PRO)
            FeatureSection(
                icon = Icons.Default.Pause,
                iconColor = Color(0xFF673AB7),
                title = "Time-Shift",
                isPro = true,
                description = "Pausa la radio en directo y continúa donde lo dejaste (máx. 2 min).",
                examples = listOf(
                    "Pausa mientras atiendes algo",
                    "Continúa exactamente donde paraste",
                    "Perfecto para no perderte nada"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Favorites Only Navigation (PRO)
            FeatureSection(
                icon = Icons.Default.Star,
                iconColor = Color(0xFFFFD700),
                title = "Navegar Solo Favoritos",
                isPro = true,
                description = "Los botones ⏮️ ⏭️ solo cambian entre tus emisoras favoritas.",
                examples = listOf(
                    "Salta directo a tus favoritos",
                    "Ignora las demás emisoras",
                    "Perfecto si tienes pocas emisoras preferidas"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Unlimited Favorites (PRO)
            FeatureSection(
                icon = Icons.Default.Favorite,
                iconColor = Color(0xFFE91E63),
                title = "Favoritos Ilimitados",
                isPro = true,
                description = "Guarda todas las emisoras que quieras como favoritas.",
                examples = listOf(
                    "Sin límite de 5 favoritos",
                    "Organiza todas tus emisoras preferidas",
                    "Acceso rápido a más contenido"
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chromecast (PRO)
            FeatureSection(
                icon = Icons.Default.Cast,
                iconColor = Color(0xFF607D8B),
                title = "Chromecast",
                isPro = true,
                description = "Transmite la radio a Chromecast, Google Home o Smart TVs.",
                examples = listOf(
                    "📺 Envía audio a cualquier dispositivo Cast",
                    "🔊 Escucha en tus altavoces favoritos",
                    "📱 Controla desde el móvil"
                )
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Section Header: Android Auto
            SectionHeader(title = "🚗 Android Auto", color = Color(0xFF607D8B))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FeatureSection(
                icon = Icons.Default.DirectionsCar,
                iconColor = Color(0xFF607D8B),
                title = "Controles desde el Coche",
                isPro = false,
                description = "RadioFlow+ funciona perfectamente con Android Auto.",
                examples = listOf(
                    "Navega emisoras desde la pantalla del coche",
                    "Usa comandos de voz para cambiar",
                    "Logos y nombres visibles mientras conduces",
                    "Los ajustes de navegación aplican igual"
                )
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Legal & Information
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLegalClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Legal e Información",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Privacidad, términos y licencias",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer tip
            Text(
                text = "💡 ¿Quieres desbloquear todas las funciones Premium? Hazte PRO desde Ajustes y apoya el desarrollo de RadioFlow+.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFFFD700).copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Debug logs button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onDebugLogsClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Ver logs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Registro de actividad para diagnóstico",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun FeatureSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    isPro: Boolean,
    description: String,
    examples: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isPro) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PRO",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            Color(0xFFFFD700).copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
