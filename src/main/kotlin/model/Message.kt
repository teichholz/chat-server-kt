package model

data class Message(val content: String, val sender: Receiver, val receiver: Receiver, val sent: Boolean = false)
