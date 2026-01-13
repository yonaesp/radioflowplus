package com.radioandroid.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.radioandroid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages overlay notifications that appear on top of other apps
 */
class OverlayNotificationManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var overlayView: View? = null
    private var isShowing = false
    
    /**
     * Show an overlay message
     * @param message The message to display
     * @param icon Optional icon resource ID
     * @param duration Duration in milliseconds (0 = don't auto-dismiss)
     */
    @SuppressLint("InflateParams")
    suspend fun showOverlay(
        message: String,
        icon: Int? = null,
        duration: Long = 5000
    ) = withContext(Dispatchers.Main) {
        try {
            // Check if we have permission
            if (!canDrawOverlays()) {
                AppLogger.w("OverlayNotification", "No permission to draw overlays")
                return@withContext
            }
            
            // Dismiss any existing overlay first
            dismissOverlay()
            
            // Inflate the layout
            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_notification, null)
            
            // Set message
            overlayView?.findViewById<TextView>(R.id.overlay_message)?.text = message
            
            // Set icon emoji if provided (default is ℹ️)
            icon?.let { emojiIcon ->
                overlayView?.findViewById<TextView>(R.id.overlay_icon)?.text = context.getString(emojiIcon)
            }
            
            
            // Create layout params
            val params =  WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100 // Offset from top
            }
            
            // Add to window manager
            windowManager.addView(overlayView, params)
            isShowing = true
            
            // Animate in
            overlayView?.startAnimation(
                AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            )
            
            // Auto-dismiss after duration if specified
            if (duration > 0) {
                scope.launch {
                    delay(duration)
                    dismissOverlay()
                }
            }
            
            AppLogger.d("OverlayNotification", "Showing overlay: $message")
            
        } catch (e: Exception) {
            AppLogger.e("OverlayNotification", "Error showing overlay", e)
        }
    }
    
    /**
     * Dismiss the current overlay
     */
    suspend fun dismissOverlay() = withContext(Dispatchers.Main) {
        try {
            overlayView?.let { view ->
                // Animate out
                view.startAnimation(
                    AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
                )
                
                delay(200) // Wait for animation
                
                windowManager.removeView(view)
                overlayView = null
                isShowing = false
                
                AppLogger.d("OverlayNotification", "Overlay dismissed")
            }
        } catch (e: Exception) {
            AppLogger.e("OverlayNotification", "Error dismissing overlay", e)
        }
    }
    
    /**
     * Check if app can draw overlays
     */
    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // No permission needed on older versions
        }
    }
    
    /**
     * Request overlay permission from user
     * Call this from an Activity
     */
    fun requestOverlayPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canDrawOverlays()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    /**
     * Whether an overlay is currently showing
     */
    fun isOverlayShowing(): Boolean = isShowing
    
    /**
     * Show station change overlay with automatic Toast fallback
     * Uses overlay if permission granted, otherwise falls back to Toast
     * @param stationName Name of the station
     * @param iconResId String resource ID for the icon emoji (next, previous, resume)
     */
    fun showStationChangeOverlay(stationName: String, iconResId: Int) {
        scope.launch {
            // Try overlay first if permission available
            if (canDrawOverlays()) {
                showOverlay(
                    message = stationName,
                    icon = iconResId,
                    duration = 2000 // 2 seconds - quick and non-intrusive
                )
            } else {
                // Fallback to Toast for devices without overlay permission
                withContext(Dispatchers.Main) {
                    val iconEmoji = context.getString(iconResId)
                    android.widget.Toast.makeText(
                        context,
                        "$iconEmoji $stationName",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.launch {
            dismissOverlay()
        }
    }
    
    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 5469
    }
}
