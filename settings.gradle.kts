pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.slne.surf.api.gradle.settings") version "+"
}

rootProject.name = "surf-rift"

include("surf-rift-host")
include("surf-rift-server")
include("surf-rift-common")
include("surf-rift-redis")