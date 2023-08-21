package routing

import kotlinx.serialization.Serializable

@Serializable
data class ReceiverPayload(val name: String)

@Serializable
data class ReceiverPayloadWithId(val id: Int, val name: String)
