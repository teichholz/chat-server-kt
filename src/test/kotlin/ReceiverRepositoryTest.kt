import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.koin.KoinExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import model.tables.records.ReceiverRecord
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import repository.ReceiverRepositoryDB
import repository.transaction

class ReceiverRepositoryTest : FunSpec(), KoinTest {

    override fun extensions() = listOf(KoinExtension(module {
        single { ds }
        single { ReceiverRepositoryDB(get()) }
    }))

    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15")).withInitScript("schema.sql")

    val ds = install(JdbcDatabaseContainerExtension(postgres))

    init {
        val receiverRepository by inject<ReceiverRepositoryDB>()

        beforeTest {

        }

        test("save") {
            val record = ReceiverRecord(null, "test")
            val stored = transaction { receiverRepository.save(record) }
            stored.id shouldNotBe null
        }

        test("saving multiple times keeps the id") {
            transaction {
                val stored = receiverRepository.save(ReceiverRecord(null, "test"))
                val stored2 = receiverRepository.save(stored.apply { name = "test2" })
                stored.id shouldBe stored2.id
            }
        }
    }
}