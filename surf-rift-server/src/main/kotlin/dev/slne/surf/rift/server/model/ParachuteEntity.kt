package dev.slne.surf.rift.server.model

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.worldseed.multipart.ModelEntity
import net.worldseed.multipart.animations.AnimationHandlerImpl
import java.util.UUID

class ParachuteEntity(
    instance: Instance,
    position: Pos,
    val owner: UUID? = null,
) : ModelEntity(EntityType.ARMOR_STAND, ParachuteModel(), instance, position) {
    val animations = AnimationHandlerImpl(model)
    private var current: String? = null

    init {
        setNoGravity(true)
        isInvulnerable = true
        animations.playOnce("parachute_deploy") {
            loop("parachute_glide")
        }
        viewers.filter { it.uuid == owner }.forEach(::hidePack)
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        if (player.uuid == owner) hidePack(player)
    }

    private fun hidePack(player: Player) {
        model.getPart("pack")?.removeViewer(player)
    }

    fun loop(animation: String) {
        if (current == animation || isRemoved) {
            return
        }
        current?.let { runCatching { animations.stopRepeat(it) } }
        current = animation
        animations.playRepeat(animation, 4)
    }

    fun land(onComplete: () -> Unit) {
        current?.let { runCatching { animations.stopRepeat(it) } }
        current = null
        animations.playOnce("parachute_land", 2) { onComplete() }
    }

    override fun remove() {
        animations.destroy()
        super.remove()
    }
}
