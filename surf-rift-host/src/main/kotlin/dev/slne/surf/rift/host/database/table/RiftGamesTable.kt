package dev.slne.surf.rift.host.database.table

import dev.slne.surf.database.columns.nativeUuid
import dev.slne.surf.database.columns.time.offsetDateTime
import dev.slne.surf.database.table.AuditableLongIdTable
import dev.slne.surf.rift.common.stats.RiftGameEndReason

object RiftGamesTable : AuditableLongIdTable("rift_games") {
    val gameId = nativeUuid("game_id").uniqueIndex()
    val map = varchar("map", 64)
    val hostUuid = nativeUuid("host_uuid").nullable()
    val startedAt = offsetDateTime("started_at")
    val endedAt = offsetDateTime("ended_at")
    val endReason = enumerationByName<RiftGameEndReason>("end_reason", 32)
    val winnerUuid = nativeUuid("winner_uuid").nullable()
    val playerCount = integer("player_count")
    val teamSize = integer("team_size")
    val durationSeconds = integer("duration_seconds")
    val lootLevel = varchar("loot_level", 32)
    val fallDamage = bool("fall_damage")
    val healOnKill = bool("heal_on_kill")
    val flightSpeed = varchar("flight_speed", 32)
}
