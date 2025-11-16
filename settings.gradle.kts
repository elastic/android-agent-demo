pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.commons:commons-lang3:3.20.0")
        classpath("org.apache.commons:commons-compress:1.28.0")
        classpath("com.squareup.moshi:moshi-kotlin:1.15.2")
    }
}
rootProject.name = "EDOT Android demo app"
include(":app")
include(":backend")
include(":edot-collector")
