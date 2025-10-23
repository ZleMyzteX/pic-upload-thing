val kotlinVer: String by project
val logbackVer: String by project
val ktorVer: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.ktor.plugin") version "3.3.1"
    id("com.github.ben-manes.versions") version "0.53.0"
}

group = "er.codes"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVer")
    implementation("io.ktor:ktor-server-netty:$ktorVer")
    implementation("io.ktor:ktor-server-host-common:$ktorVer")

    // Content Negotiation & Serialization
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVer")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVer")

    // CORS & Security
    implementation("io.ktor:ktor-server-cors:$ktorVer")
    implementation("io.ktor:ktor-server-auth:$ktorVer")
    implementation("io.ktor:ktor-server-sessions:$ktorVer")

    // Monitoring & Observability
    implementation("io.ktor:ktor-server-call-logging:$ktorVer")
    implementation("io.ktor:ktor-server-metrics:$ktorVer")
    implementation("io.ktor:ktor-server-status-pages:$ktorVer")

    // Performance & Optimization
    implementation("io.ktor:ktor-server-compression:$ktorVer")
    implementation("io.ktor:ktor-server-partial-content:$ktorVer")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVer")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVer")
    implementation("io.ktor:ktor-server-rate-limit:$ktorVer")

    // Request Validation
    implementation("io.ktor:ktor-server-request-validation:$ktorVer")

    // Configuration
    implementation("io.ktor:ktor-server-config-yaml:$ktorVer")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVer")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVer")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVer")
}
