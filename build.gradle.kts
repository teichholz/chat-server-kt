import nu.studer.gradle.jooq.JooqEdition
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersTableType

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val h2_version: String by project
val exposed_version: String by project
val slf4j_version: String by project
val hikaricp_version: String by project

plugins {
    kotlin("jvm") version "1.9.0"
    id("nu.studer.jooq") version "8.2"
    id("com.google.devtools.ksp") version "1.9.0-1.0.11"
    application
}

sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}

group = "coroutines"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.zaxxer:HikariCP:$hikaricp_version")


    implementation("org.slf4j:slf4j-api:$slf4j_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("ch.qos.logback:logback-core:$logback_version")

    implementation("io.arrow-kt:arrow-core:1.2.0")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0")
    implementation("io.arrow-kt:arrow-fx-stm:1.2.0")
    // implementation("io.arrow-kt:suspendapp:0.4.1-alpha.5")
    implementation("io.arrow-kt:suspendapp-jvm:0.4.1-alpha.5")
    implementation("io.arrow-kt:suspendapp-ktor-jvm:0.4.1-alpha.5")


    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-network-tls:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")


    jooqGenerator("org.postgresql:postgresql:42.5.1")

    implementation("org.jooq:jooq-kotlin-coroutines:3.18.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // koin
    runtimeOnly("io.insert-koin:koin-core:3.4.0")
    implementation("io.insert-koin:koin-ktor:3.4.0")
    implementation("io.insert-koin:koin-logger-slf4j:3.4.0")
    implementation("io.insert-koin:koin-test:3.4.0")

    // koin annotations
    implementation("io.insert-koin:koin-annotations:1.2.2")
    ksp("io.insert-koin:koin-ksp-compiler:1.2.2")


    // kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testImplementation("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.kotest.extensions:kotest-extensions-koin:1.1.0")
    testImplementation("io.kotest:kotest-property:5.5.5")

    // mockk
    testImplementation("io.mockk:mockk:1.13.4")
}


jooq {
    version.set("3.18.4")
    edition.set(JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/postgres?currentSchema=chat"
                    user = "postgres"
                    password = "postgres"
                    properties = listOf()
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        inputSchema = "chat"
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "model"
                        directory = "src/main/kotlin/generated"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    strategy.matchers = Matchers().withTables(
                        MatchersTableType()
                            .withExpression("")
                            .withTableClass(
                                MatcherRule()
                                    .withTransform(MatcherTransformType.PASCAL)
                                    .withExpression("$0_J")
                            )
                    )
                }
            }
        }
    }
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}

application {
    mainClass.set("Server")
}