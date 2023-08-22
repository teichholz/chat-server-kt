package model

import kotlinx.datetime.LocalDateTime


data class Message(val content: String, val date: LocalDateTime, val sender: Receiver, val receiver: Receiver, val sent: Boolean = false)
