pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.12.0"
        id("org.jetbrains.kotlin.android") version "2.1.20"
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        val brownfieldRepo = rootDir.resolve("../brownfield-output").normalize()
        maven {
            name = "brownfieldLocal"
            url = uri(brownfieldRepo)
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "brownfield-host-test"
include(":app")
