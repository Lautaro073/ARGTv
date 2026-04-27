package com.argtv.ui.player

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on and fullscreen
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_player)

        val channelUrl = intent.getStringExtra("channel_url")
        val channelName = intent.getStringExtra("channel_name")

        Toast.makeText(this, "Loading: $channelName", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Loading URL: $channelUrl")

        if (channelUrl != null) {
            initializePlayer(channelUrl)
        }
    }

    private fun initializePlayer(url: String) {
        player = ExoPlayer.Builder(this)
            .build()
            .apply {
                addListener(playerListener)
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
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
            Log.d(TAG, "State changed: $state")
            runOnUiThread {
                when (state) {
                    Player.STATE_BUFFERING -> Toast.makeText(this@PlayerActivity, "Buffering...", Toast.LENGTH_SHORT).show()
                    Player.STATE_READY -> Toast.makeText(this@PlayerActivity, "Ready to play", Toast.LENGTH_SHORT).show()
                    Player.STATE_ENDED -> Toast.makeText(this@PlayerActivity, "Stream ended", Toast.LENGTH_SHORT).show()
                    Player.STATE_IDLE -> Log.d(TAG, "Idle")
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

    override fun onStart() {
        super.onStart()
        player?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}