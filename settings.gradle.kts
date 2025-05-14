pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This line is likely the cause of the error, ensures project repos are not used
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Async Payments" // Replace with your actual project name
include(":app")