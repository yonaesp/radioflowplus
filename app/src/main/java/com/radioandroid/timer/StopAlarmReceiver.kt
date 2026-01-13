package com.radioandroid.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.radioandroid.service.RadioMediaService

/**
 * BroadcastReceiver dedicado para detener la alarma desde la notificación.
 * Esto es más fiable que enviar intents directamente al servicio desde PendingIntent.
 * 
 * Funciona en todos los estados:
 * - Pantalla bloqueada
 * - Pantalla desbloqueada  
 * - App en primer plano
 * - App en segundo plano
 */
class StopAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_STOP_ALARM = "com.radioandroid.STOP_ALARM_BROADCAST"
        private const val TAG = "StopAlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "StopAlarmReceiver triggered")
        
        try {
            // Enviar acción de detener al servicio
            val stopIntent = Intent(context, RadioMediaService::class.java).apply {
                action = RadioMediaService.ACTION_STOP_ALARM
            }
            
            // Iniciar servicio para procesar la detención
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(stopIntent)
            } else {
                context.startService(stopIntent)
            }
            
            android.util.Log.d(TAG, "Stop alarm intent sent to service")
            
            // Enviar broadcast para cerrar AlarmActivity si está abierta
            // IMPORTANTE: setPackage() requerido en Android 14+ para receivers no exportados
            val closeActivityIntent = Intent("com.radioandroid.CLOSE_ALARM_ACTIVITY").apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(closeActivityIntent)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error stopping alarm: ${e.message}", e)
        }
    }
}
