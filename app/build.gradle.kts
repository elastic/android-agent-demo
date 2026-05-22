import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("co.elastic.otel.android.agent") version "1.6.0"
    id("co.elastic.otel.android.instrumentation.okhttp") version "1.6.0"
    id("co.elastic.otel.android.instrumentation.crash") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "co.elastic.otel.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.elastic.otel.android.demo"
        minSdk = 26
        versionCode = 1
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
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
    implementation(libs.com.google.android.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
