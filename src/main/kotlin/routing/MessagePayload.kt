package routing

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(val from: ReceiverPayload, val message: String, val receiver: Int)
