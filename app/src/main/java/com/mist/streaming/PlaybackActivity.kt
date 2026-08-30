package com.mist.streaming

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mist.streaming.data.Channel

/**
 * Fullscreen playback activity for Mist TV.
 *
 * Plays RTMP, HLS, or DASH streams via ExoPlayer/Media3.
 * Shows a brief channel info overlay on launch, then hides it.
 * Back button / BACK key exits the player and returns to the guide.
 */
class PlaybackActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
        private const val OVERLAY_HIDE_DELAY_MS = 4000L
    }

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_playback)

        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CHANNEL, Channel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CHANNEL) as? Channel
        }

        if (channel == null) {
            Toast.makeText(this, "Error: no channel data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        enterFullscreen()
        setupPlayer(channel)
        showChannelOverlay(channel)
    }

    private fun setupPlayer(channel: Channel) {
        playerView = findViewById(R.id.player_view)

        // Use RtmpDataSource for rtmp:// URLs; Media3 auto-handles HLS & DASH
        val dataSourceFactory = if (channel.streamUrl.startsWith("rtmp://")) {
            RtmpDataSource.Factory()
        } else {
            null // Default HTTP data source factory
        }

        val mediaSourceFactory = if (dataSourceFactory != null) {
            DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
        } else {
            DefaultMediaSourceFactory(this)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                playerView.useController = false // hide default controls; use our overlay

                val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true

                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // Log error and attempt auto-retry after a short delay
                        val errorMsg = "Stream error: ${error.message}. Retrying in 3s..."
                        Toast.makeText(this@PlaybackActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        
                        playerView.postDelayed({
                            if (!isFinishing) {
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                        }, 3000)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                // If we're stuck buffering for too long (e.g. 15s), try to kick the player
                                playerView.removeCallbacks(bufferingTimeoutRunnable)
                                playerView.postDelayed(bufferingTimeoutRunnable, 15000)
                            }
                            Player.STATE_READY -> {
                                playerView.removeCallbacks(bufferingTimeoutRunnable)
                            }
                            else -> {}
                        }
                    }
                })
            }
    }

    private val bufferingTimeoutRunnable = Runnable {
        if (!isFinishing && player.playbackState == Player.STATE_BUFFERING) {
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun showChannelOverlay(channel: Channel) {
        val overlay = findViewById<View>(R.id.channel_overlay)
        val logoView = findViewById<ImageView>(R.id.overlay_logo)
        val nameView = findViewById<TextView>(R.id.overlay_channel_name)
        val descView = findViewById<TextView>(R.id.overlay_channel_desc)

        nameView.text = channel.name
        descView.text = channel.description

        Glide.with(this)
            .load(channel.logoUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.placeholder_channel)
            .error(R.drawable.placeholder_channel)
            .into(logoView)

        overlay.visibility = View.VISIBLE

        // Auto-hide overlay after a few seconds
        overlay.postDelayed({
            overlay.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction { overlay.visibility = View.GONE }
                .start()
        }, OVERLAY_HIDE_DELAY_MS)
    }

    private fun enterFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                finish()
                true
            }
            // D-pad center / Enter — toggle overlay visibility
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                toggleOverlay()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun toggleOverlay() {
        val overlay = findViewById<View>(R.id.channel_overlay)
        if (overlay.visibility == View.VISIBLE) {
            overlay.visibility = View.GONE
        } else {
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            overlay.animate().alpha(1f).setDuration(300).start()
            overlay.postDelayed({
                overlay.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction { overlay.visibility = View.GONE }
                    .start()
            }, OVERLAY_HIDE_DELAY_MS)
        }
    }

    override fun onStop() {
        super.onStop()
        playerView.removeCallbacks(bufferingTimeoutRunnable)
        player.release()
    }
}
