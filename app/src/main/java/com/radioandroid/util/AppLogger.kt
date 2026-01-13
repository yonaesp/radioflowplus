package com.radioandroid.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * RadioFlow+ Debug Logger
 * 
 * Persistent file-based logging system that survives app crashes.
 * Logs are written to internal storage and can be shared for debugging.
 * 
 * Features:
 * - Captures ALL crashes with full stack trace
 * - Writes synchronously during crashes to prevent data loss
 * - Keeps history across app sessions
 * - Exportable log text for easy sharing
 * 
 * Log file location: /data/data/com.radioandroid/files/radioflow_debug.log
 */
@Suppress("DelicateCoroutinesApi")
object AppLogger {
    
    private const val MAX_ENTRIES = 500  // More entries for better debugging
    private const val LOG_FILE_NAME = "radioflow_debug.log"
    private const val MAX_FILE_SIZE = 500 * 1024L  // 500KB max file size
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private var logFile: File? = null
    private var isInitialized = false
    private var appVersion: String = "unknown"
    private var sessionId: String = ""
    
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        
        try {
            // Get app version for logs
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                appVersion = pInfo.versionName ?: "?"
            } catch (e: Exception) {
                appVersion = "?"
            }
            
            // Generate session ID (timestamp-based)
            sessionId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            
            logFile = File(context.filesDir, LOG_FILE_NAME)
            
            // Rotate log file if too large
            if (logFile?.exists() == true && logFile!!.length() > MAX_FILE_SIZE) {
                val oldFile = File(context.filesDir, "radioflow_debug_old.log")
                oldFile.delete()
                logFile?.renameTo(oldFile)
                logFile = File(context.filesDir, LOG_FILE_NAME)
            }
            
            // Load existing logs for UI display
            if (logFile?.exists() == true) {
                val lines = logFile!!.readLines().takeLast(MAX_ENTRIES)
                val loadedLogs = lines.mapNotNull { line ->
                    try {
                        if (line.startsWith("[")) {
                            val timeEnd = line.indexOf("]")
                            val levelEnd = line.indexOf("/", timeEnd)
                            val tagEnd = line.indexOf(":", levelEnd)
                            
                            if (timeEnd > 0 && levelEnd > 0 && tagEnd > 0) {
                                LogEntry(
                                    timestamp = line.substring(1, timeEnd),
                                    level = line.substring(timeEnd + 2, levelEnd),
                                    tag = line.substring(levelEnd + 1, tagEnd),
                                    message = line.substring(tagEnd + 2)
                                )
                            } else null
                        } else null
                    } catch (e: Exception) {
                        null 
                    }
                }.reversed()
                _logs.value = loadedLogs
            }
            
            // Setup global uncaught exception handler - CRITICAL for crash logging
            val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                // Write crash synchronously - DO NOT use coroutines here!
                val crashEntry = buildString {
                    appendLine()
                    appendLine("╔════════════════════════════════════════════════════════════════════")
                    appendLine("║ 💥 FATAL CRASH - App will close")
                    appendLine("║ Time: ${fullDateFormat.format(Date())}")
                    appendLine("║ Session: $sessionId")
                    appendLine("║ Thread: ${thread.name}")
                    appendLine("║ Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
                    appendLine("║ App Version: $appVersion")
                    appendLine("╠════════════════════════════════════════════════════════════════════")
                    appendLine("║ Exception: ${throwable.javaClass.simpleName}")
                    appendLine("║ Message: ${throwable.message}")
                    appendLine("╠════════════════════════════════════════════════════════════════════")
                    appendLine("║ STACK TRACE:")
                    throwable.stackTraceToString().lines().forEach { line ->
                        appendLine("║   $line")
                    }
                    // Also log cause if exists
                    throwable.cause?.let { cause ->
                        appendLine("╠════════════════════════════════════════════════════════════════════")
                        appendLine("║ CAUSED BY: ${cause.javaClass.simpleName}")
                        appendLine("║ Message: ${cause.message}")
                        cause.stackTraceToString().lines().take(10).forEach { line ->
                            appendLine("║   $line")
                        }
                    }
                    appendLine("╚════════════════════════════════════════════════════════════════════")
                    appendLine()
                }
                
                // SYNCHRONOUS write - critical for crash persistence
                try {
                    logFile?.appendText(crashEntry)
                } catch (e: Exception) {
                    android.util.Log.e("AppLogger", "Failed to write crash: ${e.message}")
                }
                
                // Small delay to ensure file write completes
                try { Thread.sleep(300) } catch (e: Exception) {}
                
                // Call original handler
                oldHandler?.uncaughtException(thread, throwable)
            }
            
            // Log session start
            val sessionHeader = buildString {
                appendLine()
                appendLine("══════════════════════════════════════════════════════════════════════")
                appendLine("  📻 RadioFlow+ Session Started")
                appendLine("  Time: ${fullDateFormat.format(Date())}")
                appendLine("  Session ID: $sessionId")
                appendLine("  App Version: $appVersion")
                appendLine("  Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("  Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("══════════════════════════════════════════════════════════════════════")
            }
            logFile?.appendText(sessionHeader)
            
            d("AppLogger", "✅ Logger initialized. Ready to capture events and crashes.")
            
        } catch (e: Exception) {
            android.util.Log.e("AppLogger", "Failed to init logger: ${e.message}")
        }
    }
    
    data class LogEntry(
        val timestamp: String,
        val tag: String,
        val level: String,
        val message: String
    ) {
        override fun toString(): String = "[$timestamp] $level/$tag: $message"
    }
    
    private fun addEntry(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(
            timestamp = timestamp,
            tag = tag,
            level = level,
            message = message
        )
        
        // Update UI state
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > MAX_ENTRIES) {
            _logs.value = current.take(MAX_ENTRIES)
        } else {
            _logs.value = current
        }
        
        // Write to file in background (safe for normal logging)
        try {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    logFile?.appendText("$entry\n")
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        
        // Also log to system logcat
        when (level) {
            "D" -> android.util.Log.d(tag, message)
            "I" -> android.util.Log.i(tag, message)
            "W" -> android.util.Log.w(tag, message)
            "E" -> android.util.Log.e(tag, message)
        }
    }
    
    /**
     * Log debug message - for tracing app flow
     */
    fun d(tag: String, message: String) = addEntry("D", tag, message)
    
    /**
     * Log info message - for important events
     */
    fun i(tag: String, message: String) = addEntry("I", tag, message)
    
    /**
     * Log warning message - for recoverable issues
     */
    fun w(tag: String, message: String) = addEntry("W", tag, message)
    
    /**
     * Log error message - for failures and exceptions
     */
    fun e(tag: String, message: String) = addEntry("E", tag, message)
    
    /**
     * Log error with exception - captures stack trace
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        val fullMessage = "$message\n  Exception: ${throwable.javaClass.simpleName}\n  " +
            throwable.stackTraceToString().lines().take(5).joinToString("\n  ")
        addEntry("E", tag, fullMessage)
    }
    
    /**
     * Log user action - for understanding what user did before crash
     */
    fun action(action: String) {
        addEntry("I", "USER_ACTION", "👆 $action")
    }
    
    /**
     * Log playback event - specifically for radio stream issues
     */
    fun playback(event: String) {
        addEntry("I", "PLAYBACK", "🎵 $event")
    }
    
    /**
     * Clear all logs
     */
    fun clear() {
        _logs.value = emptyList()
        try {
            logFile?.delete()
            logFile?.createNewFile()
        } catch (e: Exception) {
            // ignore
        }
    }
    
    /**
     * Get all logs as shareable text
     */
    fun getLogsAsText(): String {
        return try {
            logFile?.readText() ?: _logs.value.joinToString("\n") { it.toString() }
        } catch (e: Exception) {
            _logs.value.joinToString("\n") { it.toString() }
        }
    }
    
    /**
     * Get recent logs (last N entries) for quick debugging
     */
    fun getRecentLogs(count: Int = 50): String {
        return _logs.value.take(count).joinToString("\n") { it.toString() }
    }
    
    /**
     * Get log file path for sharing
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
}
