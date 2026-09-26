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
    }
}

rootProject.name = "MessagingApp"

// Main Application
include(":app")
include(":navigation")

// Core modules
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:realtime")
include(":core:security")
include(":core:media")
include(":core:notifications")

// Domain modules
include(":domain:entities")
include(":domain:repositories")
include(":domain:usecases")

// Data modules
include(":data:dto")
include(":data:mappers")
include(":data:remote")
include(":data:local")
include(":data:repositories")

// Feature modules
include(":features:auth")
include(":features:home")
include(":features:chat")
include(":features:groups")
include(":features:contacts")
include(":features:profile")
include(":features:search")
include(":features:calls")
include(":features:settings")
