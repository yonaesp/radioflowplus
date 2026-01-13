package com.radioandroid

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.radioandroid.data.RadioStations
import com.radioandroid.service.RadioMediaService
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Activity that handles voice search queries from Google Assistant/Gemini.
 * 
 * Example voice commands:
 * - "Hey Google, pon Europa FM en RadioFlow"
 * - "OK Google, reproduce Los 40 en RadioFlow" 
 * - "Hey Google, escucha Cadena SER en RadioFlow"
 * - "Pon Los 40 en RadioFlow"
 * 
 * Also works without mentioning the app (if Assistant routes to us):
 * - "Pon Europa FM"
 * - "Reproduce Los 40"
 * 
 * This feature is FREE for all users.
 */
class VoiceSearchActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "VoiceSearch"
        
        // Common voice command prefixes to strip
        private val COMMAND_PREFIXES = listOf(
            "pon ", "poner ", "reproduce ", "reproducir ", 
            "escucha ", "escuchar ", "sintoniza ", "sintonizar ",
            "play ", "abre ", "abrir ", "inicia ", "iniciar ",
            "quiero escuchar ", "quiero oir ", "ponme ",
            "enciende ", "emite ", "emitir "
        )
        
        // App name mentions to strip from the end
        private val APP_MENTIONS = listOf(
            " en radioflow", " en radio flow", " en radioflow+",
            " en la app", " en la aplicación", " en la aplicacion",
            " on radioflow", " on radio flow"
        )
        
        // Aliases for radio stations - maps common names/variations to the official name
        private val STATION_ALIASES = mapOf(
            // Los 40
            "los 40" to "Los 40",
            "los cuarenta" to "Los 40",
            "los 40 principales" to "Los 40",
            "cuarenta principales" to "Los 40",
            "40 principales" to "Los 40",
            
            // Europa FM
            "europa fm" to "Europa FM",
            "europa" to "Europa FM",
            "europafm" to "Europa FM",
            
            // Cadena SER
            "cadena ser" to "Cadena SER",
            "la ser" to "Cadena SER",
            "ser" to "Cadena SER",
            
            // Cadena 100
            "cadena 100" to "Cadena 100",
            "cadena cien" to "Cadena 100",
            "cien" to "Cadena 100",
            
            // Cadena COPE
            "cope" to "Cadena COPE",
            "cadena cope" to "Cadena COPE",
            
            // Onda Cero
            "onda cero" to "Onda Cero",
            "ondacero" to "Onda Cero",
            
            // RNE
            "radio nacional" to "RNE Radio Nacional",
            "rne" to "RNE Radio Nacional",
            "radio nacional de españa" to "RNE Radio Nacional",
            
            // Kiss FM
            "kiss fm" to "Kiss FM",
            "kiss" to "Kiss FM",
            "kissfm" to "Kiss FM",
            
            // Rock FM
            "rock fm" to "Rock FM",
            "rockfm" to "Rock FM",
            
            // Máxima FM
            "maxima fm" to "Máxima FM",
            "máxima fm" to "Máxima FM",
            "maxima" to "Máxima FM",
            "máxima" to "Máxima FM",
            
            // Melodía FM
            "melodia fm" to "Melodía FM",
            "melodía fm" to "Melodía FM",
            "melodia" to "Melodía FM",
            "melodía" to "Melodía FM",
            
            // Radio 3
            "radio 3" to "Radio 3",
            "radio tres" to "Radio 3",
            
            // Dial
            "cadena dial" to "Cadena Dial",
            "dial" to "Cadena Dial",
            
            // Hit FM
            "hit fm" to "Hit FM",
            "hitfm" to "Hit FM",
            
            // esRadio
            "esradio" to "esRadio",
            "es radio" to "esRadio",
            
            // RAC1
            "rac1" to "RAC1",
            "rac 1" to "RAC1",
            
            // Flaix FM
            "flaix fm" to "Flaix FM",
            "flaix" to "Flaix FM",
            
            // Radio Marca
            "radio marca" to "Radio Marca",
            "marca" to "Radio Marca",
            
            // Mega
            "mega" to "MegaStar FM",
            "megastar" to "MegaStar FM",
            "megastar fm" to "MegaStar FM"
        )
    }
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Log all intent extras for debugging
        logIntent(intent)
        
        // Parse voice query from intent
        val rawQuery = extractVoiceQuery(intent)
        Log.d(TAG, "Raw query: $rawQuery")
        
        if (rawQuery.isNullOrBlank()) {
            // No query - just open the main app
            Log.d(TAG, "No query found, opening main app")
            openMainApp()
            return
        }
        
        // Clean the query to extract the station name
        val stationQuery = cleanQuery(rawQuery)
        Log.d(TAG, "Cleaned query: $stationQuery")
        
        // Find matching station
        val station = findStationByQuery(stationQuery)
        
        if (station != null) {
            Log.d(TAG, "Found station: ${station.name}")
            // Play the station via MediaController
            playStation(station)
            Toast.makeText(this, "🎵 Reproduciendo ${station.name}", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "No station found for query: $stationQuery")
            // No match found
            Toast.makeText(this, "❌ No encontré \"$stationQuery\"", Toast.LENGTH_SHORT).show()
            openMainApp()
        }
    }
    
    private fun logIntent(intent: Intent?) {
        if (intent == null) {
            Log.d(TAG, "Intent is null")
            return
        }
        
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent data: ${intent.data}")
        Log.d(TAG, "Intent type: ${intent.type}")
        
        intent.extras?.let { extras ->
            Log.d(TAG, "Intent extras:")
            for (key in extras.keySet()) {
                @Suppress("DEPRECATION")
                Log.d(TAG, "  $key = ${extras.get(key)}")
            }
        }
    }
    
    private fun extractVoiceQuery(intent: Intent?): String? {
        if (intent == null) return null
        
        // Try multiple sources for the query - ordered by priority
        val sources = listOf(
            intent.getStringExtra("query"),  // From App Actions
            intent.getStringExtra(android.app.SearchManager.QUERY),  // From standard search
            intent.getStringExtra("android.intent.extra.FOCUS"),  // Some assistants use this
            intent.getStringExtra("android.intent.extra.TEXT"),  // Text to speech
            intent.data?.getQueryParameter("query"),  // From URI
            intent.data?.lastPathSegment  // Last path segment of URI
        )
        
        for (source in sources) {
            if (!source.isNullOrBlank()) {
                Log.d(TAG, "Found query from source: $source")
                return source
            }
        }
        
        return null
    }
    
    /**
     * Clean the query by removing command prefixes and app mentions
     */
    private fun cleanQuery(query: String): String {
        var cleaned = query.lowercase().trim()
        
        // Remove app mentions from the end first
        for (mention in APP_MENTIONS) {
            if (cleaned.endsWith(mention)) {
                cleaned = cleaned.removeSuffix(mention).trim()
                Log.d(TAG, "Removed app mention '$mention', now: $cleaned")
            }
        }
        
        // Remove command prefixes from the start
        for (prefix in COMMAND_PREFIXES) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.removePrefix(prefix).trim()
                Log.d(TAG, "Removed prefix '$prefix', now: $cleaned")
                break  // Only remove one prefix
            }
        }
        
        return cleaned
    }
    
    private fun findStationByQuery(query: String): com.radioandroid.data.RadioStation? {
        val normalizedQuery = query.lowercase().trim()
        Log.d(TAG, "Searching for station with query: $normalizedQuery")
        
        // 0. Check aliases first - exact match
        STATION_ALIASES[normalizedQuery]?.let { officialName ->
            Log.d(TAG, "Found alias match: $normalizedQuery -> $officialName")
            RadioStations.stations.find { 
                it.name.equals(officialName, ignoreCase = true) 
            }?.let { return it }
        }
        
        // 1. Exact name match (case insensitive)
        RadioStations.stations.find { 
            it.name.lowercase() == normalizedQuery 
        }?.let { 
            Log.d(TAG, "Exact name match: ${it.name}")
            return it 
        }
        
        // 2. Name contains query
        RadioStations.stations.find { 
            it.name.lowercase().contains(normalizedQuery) 
        }?.let { 
            Log.d(TAG, "Name contains query: ${it.name}")
            return it 
        }
        
        // 3. Query contains station name
        RadioStations.stations.find { 
            normalizedQuery.contains(it.name.lowercase()) 
        }?.let { 
            Log.d(TAG, "Query contains name: ${it.name}")
            return it 
        }
        
        // 4. Partial alias match
        for ((alias, officialName) in STATION_ALIASES) {
            if (normalizedQuery.contains(alias) || alias.contains(normalizedQuery)) {
                Log.d(TAG, "Partial alias match: $normalizedQuery ~ $alias -> $officialName")
                RadioStations.stations.find { 
                    it.name.equals(officialName, ignoreCase = true) 
                }?.let { return it }
            }
        }
        
        // 5. Fuzzy match - split words and find partial matches
        val queryWords = normalizedQuery.split(" ", "-", "_").filter { it.length > 2 }
        RadioStations.stations.find { station ->
            val stationWords = station.name.lowercase().split(" ", "-", "_")
            queryWords.any { queryWord -> 
                stationWords.any { stationWord -> 
                    stationWord.contains(queryWord) || queryWord.contains(stationWord)
                }
            }
        }?.let { 
            Log.d(TAG, "Fuzzy word match: ${it.name}")
            return it 
        }
        
        // 6. Match by genre (as fallback)
        RadioStations.stations.find { 
            it.genre.lowercase().contains(normalizedQuery) 
        }?.let { 
            Log.d(TAG, "Genre match: ${it.name} (${it.genre})")
            return it 
        }
        
        Log.d(TAG, "No match found for: $normalizedQuery")
        return null
    }
    
    private fun playStation(station: com.radioandroid.data.RadioStation) {
        val sessionToken = SessionToken(this, ComponentName(this, RadioMediaService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                controller?.let {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(station.id.toString())
                        .setUri(station.streamUrl)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setSubtitle(station.genre)
                                .setArtist(station.genre)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                                .build()
                        )
                        .build()
                    
                    it.setMediaItem(mediaItem)
                    it.prepare()
                    it.play()
                }
                
                // Finish activity after starting playback
                finishAfterTransition()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing station", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                openMainApp()
            }
        }, MoreExecutors.directExecutor())
    }
    
    private fun openMainApp() {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }
    
    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
