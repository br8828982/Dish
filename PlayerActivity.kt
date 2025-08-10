package com.noor.dishtv

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.gson.Gson

class PlayerActivity : AppCompatActivity() {

    private lateinit var videoView: StyledPlayerView
    private var player: ExoPlayer? = null
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoView = findViewById(R.id.video_view)

        // Get channel JSON string from intent extras and parse it back to Channel object
        val channelJson = intent.getStringExtra("CHANNEL")
        if (channelJson == null) {
            Toast.makeText(this, "Invalid channel data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val channel = gson.fromJson(channelJson, Channel::class.java)
        playChannel(channel)
    }

    private fun playChannel(channel: Channel) {
        player?.release()

        val dataSourceFactory = DefaultHttpDataSource.Factory()

        if (!channel.user_agent.isNullOrBlank()) {
            dataSourceFactory.setUserAgent(channel.user_agent)
        }

        val defaultHeaders = mutableMapOf<String, String>()
        if (!channel.cookie.isNullOrBlank()) {
            defaultHeaders["Cookie"] = channel.cookie
        }
        if (defaultHeaders.isNotEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(defaultHeaders)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        videoView.player = player

        val mediaItemBuilder = MediaItem.Builder().setUri(channel.stream_url)

        if (!channel.license_url.isNullOrBlank()) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                    .setLicenseUri(channel.license_url)
                    .build()
            )
        } else {
            when {
                channel.stream_url.endsWith(".mpd", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                channel.stream_url.endsWith(".m3u8", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                channel.stream_url.endsWith(".ts", ignoreCase = true) -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }

        val mediaItem = mediaItemBuilder.build()

        player?.apply {
            setMediaItem(mediaItem)
            playWhenReady = true
            prepare()
        }
    }

    override fun onBackPressed() {
        player?.release()
        player = null
        super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
