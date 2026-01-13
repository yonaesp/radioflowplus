package com.radioandroid.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radioandroid.util.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerSheet(
    onDismiss: () -> Unit,
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
    val logs by AppLogger.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onNavigateBack, // Back press returns to settings
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "📋 Debug Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("RadioAndroid Logs", AppLogger.getLogsAsText())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Logs copiados al portapapeles", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(" Copiar Todo", modifier = Modifier.padding(start = 4.dp))
                }
                
                OutlinedButton(
                    onClick = { AppLogger.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(" Limpiar", modifier = Modifier.padding(start = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Log count
            Text(
                text = "${logs.size} entradas",
                color = Color.Gray,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Logs list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "No hay logs aún. Programa una alarma para ver los logs.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    items(logs) { entry ->
                        val levelColor = when (entry.level) {
                            "E" -> Color(0xFFFF5252) // Red
                            "W" -> Color(0xFFFFB74D) // Orange
                            "I" -> Color(0xFF4CAF50) // Green
                            else -> Color(0xFF90CAF9) // Light blue for debug
                        }
                        
                        Text(
                            text = entry.toString(),
                            color = levelColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
