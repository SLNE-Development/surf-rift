import dev.slne.surf.microservice.gradle.plugin.rabbit.RabbitModule

plugins {
    id("dev.slne.surf.api.gradle.minestom-server")
    id("dev.slne.surf.microservice")
}

repositories {
    maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    maven("https://reposilite.atlasengine.ca/public") { name = "atlasengine-repository-public" }
}

dependencies {
    api(projects.surfRiftCommon)
    api(projects.surfRiftRedis)

    implementation("net.worldseed.multipart:WorldSeedEntityEngine:13.0.4")
    implementation("net.kyori:adventure-text-minimessage:5.2.0")
}

surfMicroservice {
    withRabbitModule(RabbitModule.CLIENT_API, applyRabbitKspProcessor = false)
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
        register("surf-rabbitmq-minestom")
    }
}

val resourcePackSource = rootProject.layout.projectDirectory.dir("surf-rift-resourcepack")
val resourcePackOutput = layout.buildDirectory.dir("resourcepack")

val generateResourcePack by tasks.registering(JavaExec::class) {
    group = "rift"
    description = "Builds the Rift resource pack including the generated model engine assets."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.slne.surf.rift.server.resourcepack.ResourcePackGeneratorKt"
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    inputs.dir(resourcePackSource)
    inputs.dir(layout.projectDirectory.dir("src/main/resources/models"))
    outputs.dir(resourcePackOutput)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(
            resourcePackSource.asFile.absolutePath,
            resourcePackOutput.get().asFile.absolutePath,
        )
    })
}

afterEvaluate {
    tasks.named<Jar>("shadowJar") {
        manifest {
            attributes["Add-Opens"] = "java.base/java.lang"
        }
    }
}
