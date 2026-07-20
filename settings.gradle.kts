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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MediPro"

include(":app")
include(":core:common")
include(":core:search")
include(":core:database")
include(":core:designsystem")
include(":core:security")
include(":core:datastore")
include(":core:worker")
include(":domain")
include(":data")
include(":feature:dashboard")
include(":feature:medicine")
include(":feature:supplier")
include(":feature:customer")
include(":feature:purchase")
include(":feature:sales")
include(":feature:inventory")
include(":feature:expiry")
include(":feature:reports")
include(":feature:accounting")
include(":feature:backup")
include(":feature:settings")
include(":feature:license")
include(":feature:profile")
include(":feature:notification")
include(":feature:scanner")
include(":feature:globalsearch")
