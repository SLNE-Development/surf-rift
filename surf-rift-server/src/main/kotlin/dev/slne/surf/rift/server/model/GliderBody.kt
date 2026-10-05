package dev.slne.surf.rift.server.model

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.instance.Instance
import net.worldseed.gestures.EmotePlayer
import java.util.*

class GliderBody(
    val owner: UUID,
    instance: Instance,
    position: Pos,
    skin: PlayerSkin,
) : EmotePlayer(instance, position, skin) {
    private var current: String? = null

    init {
        setNoGravity(true)
        isInvulnerable = true
        loadEmotes(ModelLoader.playerAnimations)
        setVanillaAnimationsEnabled(false)
        animationHandler.playOnce("parachute_deploy") {
            loop("parachute_glide")
        }
        viewers.filter { it.uuid == owner }.forEach { model.removeViewer(it) }
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        if (player.uuid == owner) model.removeViewer(player)
    }

    fun loop(animation: String) {
        if (current == animation || isRemoved) {
            return
        }
        current?.let { runCatching { animationHandler.stopRepeat(it) } }
        current = animation
        animationHandler.playRepeat(animation, 4)
    }
}
