import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/generate_token") {
                val token = generateToken()
                val json = Gson().toJson(mapOf("token" to token))
                call.respondText(json, contentType = io.ktor.http.ContentType.Application.Json)
            }
        }
    }.start(wait = true)
}

suspend fun generateToken(): String {
    val token = UUID.randomUUID().toString()
    // Simulate token expiration after 30 seconds
    delay(TimeUnit.SECONDS.toMillis(30))
    return token
}
