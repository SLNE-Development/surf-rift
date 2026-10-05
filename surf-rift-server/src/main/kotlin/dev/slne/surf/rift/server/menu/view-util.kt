package dev.slne.surf.rift.server.menu

import dev.slne.surf.api.minestom.inventory.framework.view.AbstractSurfView

fun AbstractSurfView.updateAll() {
    internalContexts.toList().forEach { it.update() }
}

fun AbstractSurfView.closeAll() {
    internalContexts.toList().forEach { it.closeForEveryone() }
}

fun closeAllViews() {
    settingsView.closeAll()
    teamSelectionView.closeAll()
    spectatorView.closeAll()
}
