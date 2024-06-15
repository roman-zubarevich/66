package org.sixtysix.persistence

import org.lightcouch.CouchDbClient
import org.lightcouch.CouchDbException
import org.lightcouch.CouchDbProperties
import org.lightcouch.DocumentConflictException
import org.lightcouch.NoDocumentException
import org.sixtysix.model.Game
import org.sixtysix.model.Player
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.util.Properties

object PlaygroundRepository {
    // Any exception here will prevent the server from starting
    private val dbClient = CouchDbClient(getProperties())

    // Only the last failed operation should be retried for each object
    private val failedOperationById = mutableMapOf<String, () -> Unit>()

    private val logger = LoggerFactory.getLogger(this::class.java)

    private fun getProperties(): CouchDbProperties {
        val profile = System.getProperty("profile")
        val properties = Properties()
        val secrets = Properties()
        this::class.java.classLoader.getResourceAsStream("couchdb.$profile.properties").use { properties.load(it) }
        FileInputStream("secret.properties").use { secrets.load(it) }
        return CouchDbProperties(
            properties.getProperty("couchdb.name"),
            properties.getProperty("couchdb.createdb.if-not-exist").toBoolean(),
            properties.getProperty("couchdb.protocol"),
            properties.getProperty("couchdb.host"),
            properties.getProperty("couchdb.port").toInt(),
            properties.getProperty("couchdb.username"),
            secrets.getProperty("couchdb.password"),
        )
    }

    // Any exception here will prevent the server from starting
    fun loadPlayers() = dbClient.findDocs("{ \"selector\": { \"name\": { \"\$exists\": true } } }", Player::class.java)
        .associateBy { it._id }

    // Any exception here will prevent the server from starting
    fun loadGames() = dbClient.findDocs("{ \"selector\": { \"board\": { \"\$exists\": true } } }", Game::class.java)
        .onEach { it.onLoad() }
        .associateBy { it.id }

    @Synchronized
    fun delete(data: Persistable, retryOnFailure: Boolean = true): Boolean {
        try {
            dbClient.remove(data._id, data._rev)
            // Database is up and running at this point
            retryFailedOperations()
            return true
        } catch (e: NoDocumentException) {
            logger.error("Failed to delete object ${data._id}: not found")
            // No further action can be taken
        } catch (e: CouchDbException) {
            logger.error("Failed to delete object ${data._id}", e)
            if (retryOnFailure) failedOperationById[data._id] = { delete(data, false) }
        }
        return false
    }

    @Synchronized
    fun save(data: Persistable, retryOnFailure: Boolean = true): Boolean {
        try {
            val response = if (data._rev == null) dbClient.save(data) else dbClient.update(data)
            data._rev = response.rev
            // Database is up and running at this point
            retryFailedOperations()
            return true
        } catch (e: DocumentConflictException) {
            logger.error("Failed to save object ${data._id}: conflict")
            // No further action can be taken
        } catch (e: CouchDbException) {
            logger.error("Failed to save object ${data._id}", e)
            if (retryOnFailure) failedOperationById[data._id] = { save(data, false) }
        }
        return false
    }

    private fun retryFailedOperations() {
        if (failedOperationById.isNotEmpty()) {
            logger.info("Retrying failed operations")
            failedOperationById.values.forEach { it() }
            failedOperationById.clear()
        }
    }
}