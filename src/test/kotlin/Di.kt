import io.kotest.core.extensions.install
import io.kotest.core.spec.Spec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import org.koin.core.module.Module
import org.koin.dsl.module
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


fun Spec.dbModule(): Module {
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15"))
        .withInitScript("schema.sql")

    val ds = install(JdbcDatabaseContainerExtension(postgres))

    return module {
        single { ds }
    }
}