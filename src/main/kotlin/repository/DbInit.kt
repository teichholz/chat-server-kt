package repository

import model.tables.records.MessageRecord
import model.tables.records.ReceiverRecord

object DbInit {
    suspend fun initializeDb() {
        clearDb()
        transaction {
            val receivers = testUsers()
            receivers.forEach {
                it.attach(this)
                it.store()
            }
            testMessages(receivers).forEach {
                it.attach(this)
                it.store()
            }
        }
    }

    suspend fun clearDb() {
        transaction {
            sql {
                deleteFrom(model.tables.MessageJ.MESSAGE).execute()
                deleteFrom(model.tables.ReceiverJ.RECEIVER).execute()
            }
        }
    }

    fun testUsers(): List<ReceiverRecord> {
        return (1..100).map { ReceiverRecord().apply { name = "Testuser $it" } }.toList()
    }

    fun testMessages(receivers: List<ReceiverRecord>): List<MessageRecord> {
        return receivers.flatMap {receiverRecord ->
            (1..100).map {
                val from = receiverRecord
                var to = receivers.random()
                while (from == to) {
                    to = receivers.random()
                }

                MessageRecord().apply {
                    content = "Testmessage from ${from.name} to ${to.name}"
                    sender = from.id
                    receiver = to.id
                    date = java.time.LocalDateTime.now()
                }
            }
        }.toList()
    }
}