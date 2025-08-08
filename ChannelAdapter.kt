package com.example.exoplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.exoplayer.R
import com.example.exoplayer.data.model.Channel

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
        holder.itemView.setOnClickListener {
            onItemClick(channel)
        }
    }

    override fun getItemCount() = channels.size

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logoImageView: ImageView = view.findViewById(R.id.logoImageView)
    }
}
