package com.radioandroid.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * A DataSource wrapper that forces infinite length for live AAC streams.
 * 
 * Problem: StreamTheWorld AAC streams send Content-Length headers that make ExoPlayer
 * think the stream is only ~2 seconds long. When playback position reaches this "duration",
 * ExoPlayer fires STATE_ENDED incorrectly.
 * 
 * Solution: This wrapper intercepts the length reported by the upstream DataSource and
 * returns C.LENGTH_UNSET for streams that should be treated as infinite (live AAC).
 */
class InfiniteAacDataSource(
    private val upstream: DataSource
) : DataSource {
    
    private var currentUri: Uri? = null
    private var isInfiniteStream = false
    
    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }
    
    override fun open(dataSpec: DataSpec): Long {
        currentUri = dataSpec.uri
        
        // Detect if this is a live AAC stream that should be treated as infinite
        val url = dataSpec.uri.toString().lowercase()
        isInfiniteStream = url.endsWith(".aac") || 
                          url.contains("aac", ignoreCase = true) ||
                          url.contains("streamtheworld", ignoreCase = true) ||
                          url.contains("livestream", ignoreCase = true)
        
        val reportedLength = upstream.open(dataSpec)
        
        // For infinite streams, always report unknown length
        // This prevents ExoPlayer from thinking the stream has a finite duration
        return if (isInfiniteStream) {
            C.LENGTH_UNSET.toLong()
        } else {
            reportedLength
        }
    }
    
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream.read(buffer, offset, length)
    }
    
    override fun getUri(): Uri? {
        return upstream.uri ?: currentUri
    }
    
    override fun close() {
        upstream.close()
        currentUri = null
        isInfiniteStream = false
    }
    
    /**
     * Factory for creating InfiniteAacDataSource instances
     */
    class Factory(
        private val upstreamFactory: DataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return InfiniteAacDataSource(upstreamFactory.createDataSource())
        }
    }
}
