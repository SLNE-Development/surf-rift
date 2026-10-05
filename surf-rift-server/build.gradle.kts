plugins {
    id("dev.slne.surf.api.gradle.minestom-server")
}

dependencies {
    api(projects.surfRiftCommon)
    api(projects.surfRiftRedis)
}

surfMinestomServerApi {
    mainClass("dev.slne.surf.rift.server.MainKt")

    withCoreCommon()
    withSurfRedis()
    withSignedChat()
    withLuckPerms()
    withSpark()
    withNpcLib()
    withConsole()
    withPlugins()

    pluginDependencies {
        register("surf-core-minestom")
        register("surf-bitmap-provider-minestom")
        register("surf-redis-minestom")
    }
}