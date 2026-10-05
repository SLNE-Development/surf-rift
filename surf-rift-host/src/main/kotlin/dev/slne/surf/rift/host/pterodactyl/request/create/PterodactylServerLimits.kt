package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylServerLimits(
    val memory: Int,
    val swap: Int,
    val disk: Int,
    val io: Int,
    val cpu: Int,
    val threads: Int?,
    @SerialName("oom_disabled")
    val oomDisabled: Boolean
)
