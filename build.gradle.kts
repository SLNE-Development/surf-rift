buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/releases")
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
        classpath("dev.slne.surf.microservice:surf-microservice-gradle-plugin:+")
    }
}

allprojects {
    group = "dev.slne.surf.rift"
    version = findProperty("version") as String

    repositories {
        mavenLocal()
    }
}