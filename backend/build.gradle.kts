plugins {
    id("java")
    id("org.springframework.boot") version "4.0.1"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("co.elastic.otel:elastic-otel-runtime-attach:1.8.0")
}