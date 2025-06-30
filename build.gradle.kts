import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "no.nav.amt-deltaker-bff"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "2.2.0"

    kotlin("jvm") version kotlinVersion
    id("io.ktor.plugin") version "3.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    id("com.gradleup.shadow") version "8.3.7"
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val kotlinVersion = "2.1.10"
val ktorVersion = "3.2.0"
val logbackVersion = "1.5.18"
val prometeusVersion = "1.15.1"
val ktlintVersion = "1.2.1"
val jacksonVersion = "2.19.1"
val logstashEncoderVersion = "8.1"
val commonVersion = "3.2024.10.25_13.44-9db48a0dbe67"
val poaoTilgangVersion = "2024.10.29_14.10-4fff597d6e1b"
val kotestVersion = "5.9.1"
val flywayVersion = "11.10.0"
val hikariVersion = "6.3.0"
val kotliqueryVersion = "1.9.1"
val postgresVersion = "42.7.7"
val caffeineVersion = "3.2.1"
val mockkVersion = "1.14.4"
val nimbusVersion = "10.3"
val amtLibVersion = "1.2025.06.30_08.01-66bf5925c17b"
val unleashVersion = "11.0.0"

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav.common:log:$commonVersion")
    implementation("no.nav.common:audit-log:$commonVersion")

    implementation("no.nav.poao-tilgang:client:$poaoTilgangVersion")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("io.getunleash:unleash-client-java:$unleashVersion")

    implementation("no.nav.amt.lib:kafka:$amtLibVersion")
    implementation("no.nav.amt.lib:utils:$amtLibVersion")
    implementation("no.nav.amt.lib:models:$amtLibVersion")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
    testImplementation("no.nav.amt.lib:testing:$amtLibVersion")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("no.nav.amt.deltaker.bff.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.test {
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "no.nav.amt.deltaker.bff.ApplicationKt",
        )
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(ktlintVersion)
}
