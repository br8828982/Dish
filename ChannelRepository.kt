package com.example.exoplayer.data.remote

import com.example.exoplayer.data.model.Channel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request

class ChannelRepository {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetchChannels(url: String): List<Channel> {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")

            val bodyString = response.body?.string()
                ?: throw Exception("Response body is null")

            val listType = object : TypeToken<List<Channel>>() {}.type
            return gson.fromJson(bodyString, listType)
        }
    }
}
