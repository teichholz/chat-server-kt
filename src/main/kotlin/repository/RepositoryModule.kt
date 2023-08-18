package repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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