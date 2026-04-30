package com.argtv.ui.player

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import com.argtv.R

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val TAG = "PlayerActivity"
    
    private val prefs by lazy { getSharedPreferences("argtv_progress", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        val channelUrl: String = intent.getStringExtra("stream_url") ?: ""
        val channelName: String = intent.getStringExtra("channel_name") ?: "Unknown"
        val channelId: String = intent.getStringExtra("channel_id") ?: channelUrl
        
        // Get headers from intent
        val headers = mutableMapOf<String, String>()
        intent.extras?.keySet()?.forEach { key ->
            if (key.startsWith("header_")) {
                headers[key.removePrefix("header_")] = intent.getStringExtra(key) ?: ""
            }
        }

        Log.d(TAG, "Loading: $channelName")
        Log.d(TAG, "URL: $channelUrl")
        Log.d(TAG, "Headers: $headers")
        
        if (channelUrl.isNotEmpty()) {
            Toast.makeText(this, "Cargando: $channelName", Toast.LENGTH_SHORT).show()
            initializePlayer(channelUrl, channelId, headers)
        } else {
            Toast.makeText(this, "URL vacía", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializePlayer(url: String, channelId: String, headers: Map<String, String>) {
        try {
            // Default headers
            val defaultHeaders = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Referer" to "https://tvtvhd.com/",
                "Origin" to "https://tvtvhd.com"
            )
            
            // Merge with provided headers
            val mergedHeaders = defaultHeaders + headers
            
            val requestProperties = mergedHeaders.entries
                .filter { it.key.lowercase() != "user-agent" }
                .associate { it.key to it.value }
            
            val userAgent = mergedHeaders["User-Agent"] ?: defaultHeaders["User-Agent"]!!

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(requestProperties)

            val mediaItem = MediaItem.fromUri(url)

            player = ExoPlayer.Builder(this)
                .build()
                .apply {
                    addListener(playerListener)
                    setMediaItem(mediaItem)
                    
                    val savedPosition = prefs.getLong(channelId, 0)
                    if (savedPosition > 0 && savedPosition < 100000000) {
                        seekTo(savedPosition)
                    }
                    
                    prepare()
                    playWhenReady = true
                }

            findViewById<android.view.View>(R.id.player_view).apply {
                (this as androidx.media3.ui.PlayerView).apply {
                    player = this@PlayerActivity.player
                    useController = true
                    setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Log.d(TAG, "State: $state")
            runOnUiThread {
                when (state) {
                    Player.STATE_BUFFERING -> Log.d(TAG, "Buffering...")
                    Player.STATE_READY -> Toast.makeText(this@PlayerActivity, "Reproduciendo", Toast.LENGTH_SHORT).show()
                    Player.STATE_ENDED -> Log.d(TAG, "Ended")
                    else -> {}
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Error: ${error.errorCode} - ${error.message}", error)
            runOnUiThread {
                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Sin conexión"
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Timeout"
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "No encontrado"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato no soportado"
                    else -> error.message ?: "Error"
                }
                Toast.makeText(this@PlayerActivity, "Error: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        savePosition()
    }

    override fun onStop() {
        super.onStop()
        savePosition()
    }
    
    private fun savePosition() {
        player?.let { p ->
            if (p.isPlaying) {
                val pos = p.currentPosition
                val id = intent.getStringExtra("channel_id") ?: intent.getStringExtra("stream_url") ?: return
                prefs.edit().putLong(id, pos).apply()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        savePosition()
        player?.release()
        player = null
    }
}