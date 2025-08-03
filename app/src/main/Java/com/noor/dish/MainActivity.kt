package com.noor.mytb

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var playerView: StyledPlayerView
    private var player: ExoPlayer? = null
    private lateinit var pipBtn: ImageView
    private lateinit var rotateBtn: ImageView
    private lateinit var resizeBtn: ImageView
    private lateinit var videoTitle: TextView

    private var channelList: List<Channel> = listOf()
    private var resizeModeIndex = 0
    private val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    )

    private val remoteJsonUrl =
        "https://raw.githubusercontent.com/br8828982/M3U/refs/heads/main/channel.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        } else {
            window.decorView.systemUiVisibility =
                (window.decorView.systemUiVisibility
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        playerView = findViewById(R.id.player_view)
        videoTitle = playerView.findViewById(R.id.video_title)
		
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.updatePadding(top = topInset)
            insets
        }
		
        val topBar: View = playerView.findViewById(R.id.top_bar)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val extraPadding = (8 * resources.displayMetrics.density).toInt()
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + extraPadding,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        pipBtn = playerView.findViewById(R.id.exo_pip)
        rotateBtn = playerView.findViewById(R.id.exo_rotate)
        resizeBtn = playerView.findViewById(R.id.exo_resize)

        pipBtn.setOnClickListener { enterPIPMode() }
        rotateBtn.setOnClickListener { toggleOrientation() }
        resizeBtn.setOnClickListener { cycleResizeMode() }

        loadChannelsFromUrl { channels ->
            if (channels != null) {
                channelList = channels
                setupRecycler(channelList)
            } else {
                Toast.makeText(this, "Failed to load channel list!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadChannelsFromUrl(onLoaded: (List<Channel>?) -> Unit) {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(remoteJsonUrl).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string()
                if (json != null) {
                    val type = object : TypeToken<List<Channel>>() {}.type
                    val channels: List<Channel> = Gson().fromJson(json, type)
                    runOnUiThread { onLoaded(channels) }
                } else {
                    runOnUiThread { onLoaded(null) }
                }
            } catch (e: IOException) {
                runOnUiThread { onLoaded(null) }
            }
        }.start()
    }

    private fun setupRecycler(channels: List<Channel>) {
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = ChannelAdapter(channels) { channel ->
            playChannel(channel)
        }
    }

    private fun playChannel(channel: Channel) {
        releasePlayer()
        recyclerView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        videoTitle.text = channel.title

        setImmersiveMode(true)

        val mediaItemBuilder = MediaItem.Builder().setUri(channel.stream_url)
        if (!channel.license_url.isNullOrBlank()) {
            val drmConfig = MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                .setLicenseUri(channel.license_url!!)
                .build()
            mediaItemBuilder.setDrmConfiguration(drmConfig)
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
        } else {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        val mediaItem = mediaItemBuilder.build()
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build().also {
                playerView.player = it
                it.setMediaItem(mediaItem)
                it.prepare()
                it.playWhenReady = true
            }
        playerView.resizeMode = resizeModes[resizeModeIndex]
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        recyclerView.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        setImmersiveMode(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onBackPressed() {
        if (player != null) {
            releasePlayer()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && playerView.visibility == View.VISIBLE) {
            enterPIPMode()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPIPMode() {
        val aspectRatio = Rational(playerView.width, playerView.height)
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
        )
    }

    private fun toggleOrientation() {
        val toLandscape = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        requestedOrientation = if (toLandscape)
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

    private fun cycleResizeMode() {
        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
        playerView.resizeMode = resizeModes[resizeModeIndex]
    }
}
