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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import com.argtv.R

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val TAG = "PlayerActivity"
    
    private val prefs by lazy { getSharedPreferences("argtv_progress", Context.MODE_PRIVATE) }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val REFERER = "https://tvtvhd.com/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        val channelUrl: String = intent.getStringExtra("stream_url") ?: ""
        val channelName: String = intent.getStringExtra("channel_name") ?: "Unknown"
        val channelId: String = intent.getStringExtra("channel_id") ?: channelUrl

        Log.d(TAG, "Playing: $channelName  URL: $channelUrl")
        
        if (channelUrl.isNotEmpty()) {
            Toast.makeText(this, "Cargando: $channelName", Toast.LENGTH_SHORT).show()
            initializePlayer(channelUrl, channelId)
        } else {
            Toast.makeText(this, "Stream no disponible", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializePlayer(url: String, channelId: String) {
        try {
            // Configuración de headers como localTv
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf(
                    "Referer" to REFERER,
                    "Origin" to "https://tvtvhd.com"
                ))
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 15000, 500, 1000)
                .build()

            val mediaItem = MediaItem.fromUri(url)

            player = ExoPlayer.Builder(this)
                .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
                .setLoadControl(loadControl)
                .build()
                .apply {
                    addListener(playerListener)
                    setMediaItem(mediaItem)
                    
                    val savedPosition = prefs.getLong(channelId, 0)
                    if (savedPosition > 0 && savedPosition < 100000000) {
                        Log.d(TAG, "Resuming from: $savedPosition")
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
            when (state) {
                Player.STATE_BUFFERING -> runOnUiThread { 
                    Toast.makeText(this@PlayerActivity, "Buffering...", Toast.LENGTH_SHORT).show() 
                }
                Player.STATE_READY -> Log.d(TAG, "Ready to play")
                Player.STATE_ENDED -> runOnUiThread { 
                    Toast.makeText(this@PlayerActivity, "Ended", Toast.LENGTH_SHORT).show() 
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
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Manifest"
                    else -> error.message ?: "Error"
                }
                Toast.makeText(this@PlayerActivity, "Error: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        player?.let { if (it.isPlaying) savePosition() }
    }

    override fun onStop() {
        super.onStop()
        player?.let { if (it.isPlaying) savePosition() }
    }
    
    private fun savePosition() {
        player?.let { p ->
            val pos = p.currentPosition
            val id = intent.getStringExtra("channel_id") ?: intent.getStringExtra("stream_url") ?: return
            prefs.edit().putLong(id, pos).apply()
            Log.d(TAG, "Saved: $pos")
        }
    }

    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}