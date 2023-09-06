package repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import model.tables.records.ReceiverRecord
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan
class RepositoryModule {
    @Single(createdAtStart = true)
    fun init(): HikariDataSource = createHikariDataSource(
        url = "jdbc:postgresql://localhost:5432/postgres",
        driver = "org.postgresql.Driver"
    )

    suspend fun initializeDb() {
        clearDb()
        transaction {
            testUser().forEach {
                it.insert()
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

    fun testUser(): List<ReceiverRecord> {
        return (1..20).map { ReceiverRecord().apply { name = "Testuser $it" } }.toList()
    }

    private fun createHikariDataSource(
        url: String,
        driver: String
    ) = HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        driverClassName = driver
        schema = "chat"
        username = "postgres"
        password = "postgres"
        maximumPoolSize = 5
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })
}