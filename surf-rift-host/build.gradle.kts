import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.standalone")
    id("dev.slne.surf.microservice")
}

dependencies {
    api(projects.surfRiftRedis)
}

surfStandaloneApi {
    withSurfRedis()
    withSurfDatabaseR2dbc("2.3.4", "dev.slne.surf.rift.libs")
}

surfMicroservice {
    withMicroserviceApi()
    withRabbitModule(RabbitModule.SERVER_API, true)
}