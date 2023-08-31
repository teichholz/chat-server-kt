package routing

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

interface InSynch: HasId, HasName {}

interface HasId {
    val id: Int
}

interface HasName {
    val name: String
}

@Serializable
data class ReceiverPayloadWithId(val id: Int, val name: String)

@Serializable
data class ReceiverPayload(val name: String)

@Serializable
data class ReceiverPayloadRegister(override val name: String, val timestamp: LocalDateTime?) : HasName

@Serializable
data class ReceiverPayloadLogin(val name: String, val timestamp: LocalDateTime?)

@Serializable
data class ReceiverPayloadLogout(override val id: Int, override val name: String) : InSynch

@Serializable
data class ReceiverPayloadConfiguration(val from: ReceiverPayloadWithId, val to: ReceiverPayloadWithId)
