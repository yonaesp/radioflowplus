package com.radioandroid.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import com.radioandroid.util.AppLogger

/**
 * Custom DataSource for Radio Streaming (Clean Version - No Metadata)
 * 
 * Primary Purpose:
 * 1. Robust HTTP connection using OkHttp (handling user-agents, timeouts).
 * 2. Fixing AAC StateEnded bug by always returning C.LENGTH_UNSET (Infinite Stream).
 * 3. NO manual metadata parsing (removed for stability to prevent audio interference).
 */
class IcyDataSource(
    private val callFactory: OkHttpClient
) : BaseDataSource(true), HttpDataSource {

    companion object {
        private const val TAG = "IcyDataSource"
        
        // Static listener removed - Metadata feature disabled
    }
    
    class Factory(private val callFactory: OkHttpClient) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return IcyDataSource(callFactory)
        }
    }

    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var inputStream: InputStream? = null
    private var bytesRead: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.bytesRead = 0
        transferInitializing(dataSpec)

        val url = dataSpec.uri.toString()
        AppLogger.d(TAG, "Opening stream: $url")
        
        val request = Request.Builder()
            .url(url)
            // Note: We deliberately do NOT request Icy-MetaData to avoid byte-stream interference
            .addHeader("Accept-Encoding", "identity")
            .addHeader("User-Agent", "RadioFlow+/1.0 (Android)")
            .build()

        try {
            val call = callFactory.newCall(request)
            val response = call.execute()
            this.response = response

            if (!response.isSuccessful) {
                response.close()
                throw HttpDataSource.HttpDataSourceException(
                    "Unexpected code $response",
                    dataSpec,
                    if (response.code == 404) HttpDataSource.HttpDataSourceException.TYPE_OPEN else HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }

            // Directly use the raw stream without any parsing/interception
            inputStream = response.body?.byteStream() ?: throw IOException("Empty body")
            
            opened = true
            transferStarted(dataSpec)
            
            // CRITICAL: Always return LENGTH_UNSET for live radio to prevent "End of Stream" errors
            // Ensure strict Long type for open() return signature
            return if (C.LENGTH_UNSET == -1) -1L else C.LENGTH_UNSET.toLong()
            
        } catch (e: Exception) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e as? IOException ?: IOException(e),
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val currentDataSpec = dataSpec ?: throw IOException("DataSpec not set")
        val stream = inputStream ?: throw IOException("Stream closed")

        return try {
            val bytesRead = stream.read(buffer, offset, length)
            if (bytesRead == -1) {
                C.RESULT_END_OF_INPUT
            } else {
                this.bytesRead += bytesRead
                bytesTransferred(bytesRead)
                bytesRead
            }
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e, currentDataSpec, HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        inputStream?.close()
        inputStream = null
        response?.close()
        response = null
    }

    override fun getUri(): Uri? = dataSpec?.uri
    
    override fun getResponseHeaders(): Map<String, List<String>> {
        return response?.headers?.toMultimap() ?: emptyMap()
    }

    override fun setRequestProperty(name: String, value: String) {}
    override fun clearRequestProperty(name: String) {}
    override fun clearAllRequestProperties() {}
    override fun getResponseCode(): Int = response?.code ?: -1
}
