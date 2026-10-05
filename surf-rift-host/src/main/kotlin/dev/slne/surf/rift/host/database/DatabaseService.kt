package dev.slne.surf.rift.host.database

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.rift.host.database.table.RiftGamePlayersTable
import dev.slne.surf.rift.host.database.table.RiftGamesTable
import java.nio.file.Path

object DatabaseService {
    private lateinit var databaseApi: DatabaseApi

    suspend fun connect(dataPath: Path) {
        databaseApi = DatabaseApi.create(dataPath)

        suspendTransaction {
            SchemaUtils.create(RiftGamesTable, RiftGamePlayersTable)
        }
    }

    fun disconnect() {
        if(::databaseApi.isInitialized) {
            databaseApi.shutdown()
        }
    }
}