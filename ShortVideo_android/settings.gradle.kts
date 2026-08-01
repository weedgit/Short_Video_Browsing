pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ShortVideo"
include(":app")
include(":core")
include(":domain")
include(":data")
include(":common:theme")
include(":common:composable")
include(":feature:home")
include(":feature:discover")
include(":feature:upload")
include(":feature:inbox")
include(":feature:profile")
include(":feature:auth")
include(":feature:onboarding")
include(":feature:settings")
