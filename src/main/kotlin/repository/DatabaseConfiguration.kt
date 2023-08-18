package repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.koin.core.annotation.Single

object DatabaseConfiguration {
    @Single
    fun init(): HikariDataSource {
        return createHikariDataSource(
            url = "jdbc:postgresql://localhost:5432/postgres",
            driver = "org.postgresql.Driver")
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