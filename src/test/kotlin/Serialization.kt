import chat.commons.protocol.AuthPayloadSocket
import chat.commons.protocol.Protocol
import chat.commons.protocol.auth
import chat.commons.routing.ReceiverPayloadWithId
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SerializationTest : FunSpec() {
    init {
        test("Protocol test") {
            val json = Json {
                isLenient = true
                classDiscriminator = "key"
            }

            val auth = auth {
                payload = AuthPayloadSocket(receiver = ReceiverPayloadWithId(0, "Test"))
            }

            val string = json.encodeToString(auth)

            val deserialized: Protocol = json.decodeFromString(string)

            println(deserialized is Protocol.AUTH)
            println((deserialized as Protocol.AUTH).payload)
        }
    }
}