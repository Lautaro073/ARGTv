package com.argtv.ui.player

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.argtv.R

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private val TAG = "PlayerActivity"
    
    private val prefs by lazy { getSharedPreferences("argtv_progress", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        val channelUrl: String = intent.getStringExtra("channel_url") ?: ""
        val channelName: String = intent.getStringExtra("channel_name") ?: "Unknown"
        val channelId: String = intent.getStringExtra("channel_id") ?: channelUrl

        if (channelUrl.isNotEmpty()) {
            Toast.makeText(this, "Loading: $channelName", Toast.LENGTH_SHORT).show()
            initializePlayer(channelUrl, channelId)
        }
    }

    private fun initializePlayer(url: String, channelId: String) {
        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                addListener(playerListener)
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                
                val savedPosition = prefs.getLong(channelId, 0)
                if (savedPosition > 0) {
                    Log.d(TAG, "Resuming from: $savedPosition")
                    seekTo(savedPosition)
                }
                
                prepare()
                playWhenReady = true
            }

        findViewById<android.view.View>(R.id.player_view).apply {
            (this as androidx.media3.ui.PlayerView).apply {
                this.player = this@PlayerActivity.player
                useController = true
                setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Log.d(TAG, "State: $state")
            runOnUiThread {
                when (state) {
                    Player.STATE_BUFFERING -> Toast.makeText(this@PlayerActivity, "Buffering...", Toast.LENGTH_SHORT).show()
                    Player.STATE_READY -> Toast.makeText(this@PlayerActivity, "Ready to play", Toast.LENGTH_SHORT).show()
                    Player.STATE_ENDED -> Toast.makeText(this@PlayerActivity, "Ended", Toast.LENGTH_SHORT).show()
                    else -> {}
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Error: ${error.message}")
            runOnUiThread {
                Toast.makeText(this@PlayerActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
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
            val pos = p.currentPosition
            val id = intent.getStringExtra("channel_id") ?: intent.getStringExtra("channel_url") ?: return
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
        savePosition()
        player?.release()
        player = null
    }
}