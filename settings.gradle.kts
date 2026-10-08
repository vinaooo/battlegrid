val vinkitTag = providers.gradleProperty("vinkit.tag").get()

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            // JitPack serves vinkit's plugins as one jar, not as Gradle plugin markers.
            if (requested.id.id.startsWith("vinkit.")) {
                useModule("com.github.vinaooo.vinkit:convention:" + providers.gradleProperty("vinkit.tag").get())
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    versionCatalogs {
        create("libs") {
            from("com.github.vinaooo.vinkit:catalog:$vinkitTag")
        }
    }
}

rootProject.name = "BattleGrid"

include(":app")
include(":domain")
