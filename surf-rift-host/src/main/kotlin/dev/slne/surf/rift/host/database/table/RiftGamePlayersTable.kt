package dev.slne.surf.rift.host.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.ReferenceOption
import dev.slne.surf.database.table.AuditableLongIdTable

object RiftGamePlayersTable : AuditableLongIdTable("rift_game_players") {
    val game = reference("game", RiftGamesTable, onDelete = ReferenceOption.CASCADE)
    val playerUuid = nativeUuid("player_uuid").index()
    val playerName = varchar("player_name", 16)
    val team = integer("team").nullable()
    val placement = integer("placement")
    val winner = bool("winner")
    val kills = integer("kills")
    val assists = integer("assists")
    val points = integer("points")
    val damageDealt = double("damage_dealt")
    val damageTaken = double("damage_taken")
    val chestsOpened = integer("chests_opened")
    val survivedSeconds = integer("survived_seconds")
    val killerUuid = nativeUuid("killer_uuid").nullable()
    val deathCause = varchar("death_cause", 32).nullable()

    init {
        uniqueIndex(game, playerUuid)
    }
}
