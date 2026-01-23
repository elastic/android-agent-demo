plugins {
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.androidLib) apply false
    id("com.diffplug.spotless") version "8.2.0"
}

subprojects {
    plugins.apply("com.diffplug.spotless")

    spotless {
        java {
            googleJavaFormat()
            target("src/*/java/**/*.java")
            licenseHeaderFile(rootProject.file("source_license.txt"))
        }
        kotlin {
            ktfmt()
            target("src/*/java/**/*.kt")
            licenseHeaderFile(rootProject.file("source_license.txt"))
        }
    }
}

spotless {
    flexmark {
        flexmark()
        target("**/*.md")
    }
}