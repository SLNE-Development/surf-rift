package dev.slne.surf.rift.host.database

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.nio.file.Path

object DatabaseService {
    private lateinit var databaseApi: DatabaseApi

    suspend fun connect(dataPath: Path) {
        databaseApi = DatabaseApi.create(dataPath)

        suspendTransaction {
            // TABLES
        }
    }

    fun disconnect() {
        if(::databaseApi.isInitialized) {
            databaseApi.shutdown()
        }
    }
}