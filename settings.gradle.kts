pluginManagement {
    repositories {
        google()
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

rootProject.name = "TodoEnUno"
include(":app", ":core:catalog", ":feature:utilities", ":feature:audio", ":feature:recorder", ":feature:finance", ":feature:camera")

include(":feature:math-tools", ":feature:sensor-tools", ":feature:location-tools")

include(":feature:education-tools")

include(":feature:document-tools")
