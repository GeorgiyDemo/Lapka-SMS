package org.lapka.sms

data class Message(
    val text: String,
    val channelId: Int? = null
)
