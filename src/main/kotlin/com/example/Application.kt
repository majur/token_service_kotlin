import Tokens.select
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.TimeUnit


// Define a table for storing tokens
object Tokens : Table() {
    val id = uuid("id").uniqueIndex()
    val expiration = long("expiration")
}

fun main() {
    // Initialize the database connection
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    // Create the tokens table
    transaction {
        SchemaUtils.create(Tokens)
    }

    // Start the server
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/generate_token") {
                val token = generateToken()
                val json = Gson().toJson(mapOf("token" to token))
                call.respondText(json, contentType = ContentType.Application.Json)
            }

            get("/validate_token") {
                val token = call.parameters["token"]
                if (token != null && isValidToken(token)) {
                    call.respondText("Token is valid", contentType = ContentType.Text.Plain)
                } else {
                    call.respondText("Invalid or expired token", contentType = ContentType.Text.Plain)
                }
            }
        }
    }.start(wait = true)
}

suspend fun generateToken(): String {
    val token = UUID.randomUUID().toString()
    // Store the token and its expiration time in the database
    transaction {
        Tokens.insert {
            it[id] = UUID.fromString(token)
            it[expiration] = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)
        }
    }
    return token
}

fun isValidToken(token: String): Boolean {
    // Retrieve the token from the database and check its expiration
    val uuid = UUID.fromString(token)
    return transaction {
        Tokens.select { Tokens.id eq uuid }
            .map { it[Tokens.expiration] }
            .singleOrNull()
            ?.let { expiration -> expiration > System.currentTimeMillis() }
            ?: false
    }
}
