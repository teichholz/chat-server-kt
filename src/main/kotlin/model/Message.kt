package model

data class Message(val content: String, val receiver: Receiver, val sent: Boolean = false)
