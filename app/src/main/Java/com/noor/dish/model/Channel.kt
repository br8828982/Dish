package com.noor.dish.model

data class Channel(
    val id: String,
    val title: String,
    val logo_url: String,
    val stream_url: String,
    val license_url: String?,
)
