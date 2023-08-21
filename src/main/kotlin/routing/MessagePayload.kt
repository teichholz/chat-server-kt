package routing

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(val message: String, val receiver: Int)
