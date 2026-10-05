package dev.slne.surf.rift.server.game.map

import dev.slne.surf.rift.server.env.EnvVariables
import dev.slne.surf.rift.server.game.map.maps.TestMap

object RiftMaps {
    private val maps: Map<String, RiftMap> = listOf<RiftMap>(
        TestMap
    ).associateBy { it.id.lowercase() }

    val all: Collection<RiftMap> get() = maps.values

    fun selected(): RiftMap {
        val id = EnvVariables.RIFT_MAP.string().lowercase()
        return maps[id] ?: error("Unknown rift map '$id', available maps: ${maps.keys.joinToString()}")
    }
}
