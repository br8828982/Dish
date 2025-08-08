package com.example.exoplayer.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exoplayer.R
import com.example.exoplayer.data.model.Channel
import com.example.exoplayer.data.remote.ChannelRepository
import com.example.exoplayer.ui.adapter.ChannelAdapter
import com.example.exoplayer.ui.player.PlayerActivity
import kotlin.concurrent.thread

class ChannelsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val repository = ChannelRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channels, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.channelsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        fetchChannels()
    }

    private fun fetchChannels() {
        thread {
            try {
                val url = "https://raw.githubusercontent.com/br8828982/M3U/refs/heads/main/channel_alt.json"
                val channels = repository.fetchChannels(url)

                activity?.runOnUiThread {
                    recyclerView.adapter = ChannelAdapter(channels) { channel ->
                        startPlayer(channel)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to load channels", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun startPlayer(channel: Channel) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("video_url", channel.stream_url)
            putExtra("license_url", channel.license_url)
            putExtra("title", channel.title)
        }
        startActivity(intent)
    }
}
