package com.noor.dishtv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import java.io.IOException
import kotlin.concurrent.thread

data class Channel(
    val id: String,
    val title: String,
    val logo_url: String,
    val stream_url: String,
    val license_url: String,
    val user_agent: String? = null,
    val cookie: String? = null
)

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var videoView: StyledPlayerView
    private var player: ExoPlayer? = null
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.channelsRecyclerView)
        videoView = findViewById(R.id.video_view)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        fetchChannels()
    }

    private fun fetchChannels() {
        thread {
            try {
                val url = "https://raw.githubusercontent.com/br8828982/M3U/refs/heads/main/full_channels.json"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val body = response.body?.string() ?: throw JSONException("Empty Response")

                val listType = object : TypeToken<List<Channel>>() {}.type
                val channels: List<Channel> = gson.fromJson(body, listType)

                runOnUiThread {
                    recyclerView.adapter = ChannelAdapter(channels) { channel ->
                        playChannel(channel)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to load channels", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playChannel(channel: Channel) {
        recyclerView.visibility = View.GONE
        videoView.visibility = View.VISIBLE

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
        if (videoView.visibility == View.VISIBLE) {
            videoView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            player?.release()
            player = null
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    class ChannelAdapter(
        private val channels: List<Channel>,
        private val onItemClick: (Channel) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_channel, parent, false)
            return ChannelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
            val channel = channels[position]
            holder.logoImageView.load(channel.logo_url)
            holder.itemView.setOnClickListener { onItemClick(channel) }
        }

        override fun getItemCount() = channels.size

        class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val logoImageView: ImageView = view.findViewById(R.id.logoImageView)
        }
    }
}
