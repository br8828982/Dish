package com.example.exoplayer.ui.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayer.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.MimeTypes

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            val exoPlayer = ExoPlayer.Builder(this).build()
            player = exoPlayer
            findViewById<StyledPlayerView>(R.id.video_view).player = exoPlayer

            val videoUrl = intent.getStringExtra("video_url") ?: return
            val licenseUrl = intent.getStringExtra("license_url")
            val title = intent.getStringExtra("title")

        	val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoUrl)

            if (!licenseUrl.isNullOrEmpty()) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)

                mediaItemBuilder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                        .setLicenseUri(licenseUrl)
                        .build()
                )
            } else {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }

            val mediaItem = mediaItemBuilder.build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }
}
