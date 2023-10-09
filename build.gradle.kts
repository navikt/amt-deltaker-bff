group = "no.nav.amt-deltaker-bff"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.ktor.plugin") version "2.3.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
}

repositories {
    mavenCentral()
}

val kotlinVersion = "1.9.10"
val ktorVersion = "2.3.5"
val logbackVersion = "1.4.11"
val prometeusVersion = "1.11.4"
val ktlintVersion = "0.49.1"
val jacksonVersion = "2.15.2"

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
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("no.nav.amt.deltaker.bff.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "no.nav.amt.deltaker.bff.ApplicationKt",
        )
    }
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(ktlintVersion)
}
