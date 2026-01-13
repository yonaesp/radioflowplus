package com.radioandroid.cast

import android.content.Context
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

/**
 * Provides Cast configuration for Google Cast SDK.
 * Uses the Default Media Receiver for audio streaming.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        // Notification configuration for Cast
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName("com.radioandroid.MainActivity")
            .build()

        // Media options with notification
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .build()

        // Launch options - don't relaunch on each item (audio live stream)
        val launchOptions = LaunchOptions.Builder()
            .setRelaunchIfRunning(false)
            .build()

        return CastOptions.Builder()
            // Use Default Media Receiver - ideal for audio streaming
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setLaunchOptions(launchOptions)
            .setCastMediaOptions(mediaOptions)
            // Don't resume session - radio is live, not resumable
            .setResumeSavedSession(false)
            .setEnableReconnectionService(true)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
