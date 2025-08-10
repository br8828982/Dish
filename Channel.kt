package com.noor.dishtv

data class Channel(
    val id: String,
    val title: String,
    val logo_url: String,
    val stream_url: String,
    val license_url: String,
    val user_agent: String? = null,
    val cookie: String? = null
)
