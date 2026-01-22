import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("co.elastic.otel.android.agent") version "1.5.0"
    id("co.elastic.otel.android.instrumentation.okhttp") version "1.5.0"
}

android {
    namespace = "co.elastic.otel.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.elastic.otel.android.demo"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        viewBinding = true
    }
}
kotlin {
    compilerOptions {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
}