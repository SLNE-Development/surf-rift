package dev.slne.surf.rift.common.stats

import kotlinx.serialization.Serializable

@Serializable
enum class RiftGameEndReason {
    LAST_SURVIVOR,
    TIME_UP,
    NO_PLAYERS,
    CANCELLED
}
