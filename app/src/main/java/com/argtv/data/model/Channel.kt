package com.argtv.data.model

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String = "",
    val category: String = "general"
)