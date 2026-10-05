package dev.slne.surf.rift.host.pterodactyl.request.create

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PterodactylServer(
    val id: Int,
    @SerialName("external_id")
    val externalId: String?,
    val uuid: String,
    val identifier: String,
    val name: String,
    val description: String,
    val status: String?,
    val suspended: Boolean,
    val limits: PterodactylServerLimits,
    @SerialName("feature_limits")
    val featureLimits: PterodactylServerFeatureLimits,
    val user: Int,
    val node: Int,
    val allocation: Int,
    val nest: Int,
    val egg: Int,
    val container: PterodactylServerContainer,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)
