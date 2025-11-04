@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.ApplicationAndroidComponentsExtension

plugins {
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.androidLib) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.diffplug.spotless") version "8.0.0"
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

    val spotlessApply = tasks.named("spotlessApply")
    plugins.withId("com.android.application") {
        val componentsExtension = extensions.getByType(ApplicationAndroidComponentsExtension::class)
        componentsExtension.onVariants(componentsExtension.selector().all()) {
            it.lifecycleTasks.registerPreBuild(spotlessApply)
        }
    }
    plugins.withId("java") {
        tasks.named("classes") {
            dependsOn(spotlessApply)
        }
    }
}

spotless {
    flexmark {
        flexmark()
        target("**/*.md")
    }
}