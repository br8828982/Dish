package com.noor.dishtv

import android.content.Intent
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import java.io.IOException
import kotlin.concurrent.thread

class ChannelListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel_list)

        recyclerView = findViewById(R.id.channelsRecyclerView)
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
                        // Start PlayerActivity on channel click, passing the channel
                        val intent = Intent(this, PlayerActivity::class.java)
                        intent.putExtra("CHANNEL", gson.toJson(channel))
                        startActivity(intent)
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
