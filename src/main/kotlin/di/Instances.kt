package di

import com.zaxxer.hikari.HikariDataSource
import repository.DatabaseConfiguration

object Instances {
    val database: HikariDataSource = DatabaseConfiguration.init()
}