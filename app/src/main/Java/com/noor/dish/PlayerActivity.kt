package com.noor.mytvapp

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.MimeTypes

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: StyledPlayerView
    private lateinit var pipBtn: ImageView
    private lateinit var rotateBtn: ImageView
    private lateinit var resizeBtn: ImageView
    private lateinit var videoTitle: TextView

    private var player: ExoPlayer? = null
    private val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )
    private var resizeModeIndex = 0

    private lateinit var streamUrl: String
    private var licenseUrl: String? = null
    private lateinit var title: String

    private var isInLandscape = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContentView(R.layout.activity_player)

        // Get video info from intent
        streamUrl = intent.getStringExtra("CHANNEL_STREAM_URL") ?: ""
        licenseUrl = intent.getStringExtra("CHANNEL_LICENSE_URL")
        title = intent.getStringExtra("CHANNEL_TITLE") ?: "Untitled"

        playerView = findViewById(R.id.player_view)
        videoTitle = playerView.findViewById(R.id.video_title)
        pipBtn = playerView.findViewById(R.id.exo_pip)
        rotateBtn = playerView.findViewById(R.id.exo_rotate)
        resizeBtn = playerView.findViewById(R.id.exo_resize)

        pipBtn.setOnClickListener { enterPIPMode() }
        rotateBtn.setOnClickListener { toggleOrientation() }
        resizeBtn.setOnClickListener { cycleResizeMode() }

        videoTitle.text = title
        playVideo()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Called if new video is sent while this PlayerActivity is still alive (including PiP/background)
        intent?.let {
            val newStreamUrl = it.getStringExtra("CHANNEL_STREAM_URL") ?: ""
            val newLicenseUrl = it.getStringExtra("CHANNEL_LICENSE_URL")
            val newTitle = it.getStringExtra("CHANNEL_TITLE") ?: "Untitled"

            if (newStreamUrl != streamUrl) {
                releasePlayer()
                streamUrl = newStreamUrl
                licenseUrl = newLicenseUrl
                title = newTitle
                videoTitle.text = title
                playVideo()
                // If we are in PiP, exiting PiP and bringing to front happens automatically
            }
        }
    }

    private fun playVideo() {
        val mediaItemBuilder = MediaItem.Builder().setUri(streamUrl)
        if (!licenseUrl.isNullOrBlank()) {
            val drmConfig = MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                .setLicenseUri(licenseUrl!!)
                .build()
            mediaItemBuilder.setDrmConfiguration(drmConfig)
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        } else {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build().also {
                playerView.player = it
                it.setMediaItem(mediaItemBuilder.build())
                it.prepare()
                it.playWhenReady = true
            }
        playerView.resizeMode = resizeModes[resizeModeIndex]
        setImmersiveMode(true)
    }

    private fun cycleResizeMode() {
        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
        playerView.resizeMode = resizeModes[resizeModeIndex]
    }

    // UX: Portrait <-> Landscape (sensor, so user can freely rotate device in landscape, including upside down)
    private fun toggleOrientation() {
        if (isInLandscape) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isInLandscape = false
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isInLandscape = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPIPMode() {
        val pipAspectRatio = Rational(16, 9) // Standard video aspect for PiP
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(pipAspectRatio)
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPIPMode()
        }
    }

    override fun onBackPressed() {
        releasePlayer()
        super.onBackPressed()
    }

    // * Most important: release the player in onStop so PiP close or swipe closes playback!
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        setImmersiveMode(false)
    }

    private fun setImmersiveMode(enabled: Boolean) {
        window.decorView.systemUiVisibility = if (enabled) {
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        } else {
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }
}
