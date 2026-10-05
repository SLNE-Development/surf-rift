package dev.slne.surf.rift.host.database.repository

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.eq
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.batchInsert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.rift.common.stats.RiftGameResult
import dev.slne.surf.rift.host.database.table.RiftGamePlayersTable
import dev.slne.surf.rift.host.database.table.RiftGamesTable
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object RiftGameResultRepository {
    suspend fun exists(gameId: UUID): Boolean = suspendTransaction {
        RiftGamesTable.selectAll()
            .where { RiftGamesTable.gameId eq gameId }
            .firstOrNull() != null
    }

    suspend fun insert(result: RiftGameResult) = suspendTransaction {
        val id = RiftGamesTable.insertAndGetId {
            it[gameId] = UUID.fromString(result.gameId)
            it[map] = result.map
            it[hostUuid] = result.hostUuid?.let(UUID::fromString)
            it[startedAt] = result.startedAt.toOffsetDateTime()
            it[endedAt] = result.endedAt.toOffsetDateTime()
            it[endReason] = result.endReason
            it[winnerUuid] = result.winnerUuid?.let(UUID::fromString)
            it[playerCount] = result.players.size
            it[teamSize] = result.teamSize
            it[durationSeconds] = result.settings.durationSeconds
            it[lootLevel] = result.settings.lootLevel
            it[fallDamage] = result.settings.fallDamage
            it[healOnKill] = result.settings.healOnKill
            it[flightSpeed] = result.settings.flightSpeed
        }

        RiftGamePlayersTable.batchInsert(result.players, shouldReturnGeneratedValues = false) { player ->
            this[RiftGamePlayersTable.game] = id
            this[RiftGamePlayersTable.playerUuid] = UUID.fromString(player.uuid)
            this[RiftGamePlayersTable.playerName] = player.name.take(16)
            this[RiftGamePlayersTable.team] = player.team
            this[RiftGamePlayersTable.placement] = player.placement
            this[RiftGamePlayersTable.winner] = player.winner
            this[RiftGamePlayersTable.kills] = player.kills
            this[RiftGamePlayersTable.assists] = player.assists
            this[RiftGamePlayersTable.points] = player.points
            this[RiftGamePlayersTable.damageDealt] = player.damageDealt
            this[RiftGamePlayersTable.damageTaken] = player.damageTaken
            this[RiftGamePlayersTable.chestsOpened] = player.chestsOpened
            this[RiftGamePlayersTable.survivedSeconds] = player.survivedSeconds
            this[RiftGamePlayersTable.killerUuid] = player.killerUuid?.let(UUID::fromString)
            this[RiftGamePlayersTable.deathCause] = player.deathCause?.take(32)
        }
    }

    private fun Long.toOffsetDateTime(): OffsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
}
