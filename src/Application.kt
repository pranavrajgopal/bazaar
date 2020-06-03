
import com.google.gson.Gson
import data.Move
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.features.*
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import kotlinx.coroutines.*
import io.ktor.client.engine.cio.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.http.cio.websocket.*
import java.time.*
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.channels.consumeEach

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val gameApplication = GameApplication()
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    val client = HttpClient(CIO) {
        install(Auth) {
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
    runBlocking {
        // Sample for making a HTTP Client request
        /*
        val message = client.post<JsonSampleClass> {
            url("http://127.0.0.1:8080/path/to/endpoint")
            contentType(ContentType.Application.Json)
            body = JsonSampleClass(hello = "world")
        }
        */
    }
    install(CallLogging)
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/createGame") {
            val gameName: String? = call.request.queryParameters["name"]
            if(gameName.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Game name is invalid")
                return@get
            }

            if(!gameApplication.createGame(gameName)) {
                call.respond(HttpStatusCode.BadRequest, "Game name exists. Choose another")
                return@get
            }

            call.respondText(Gson().toJson(SuccessResponse("Created Game $gameName")), contentType = ContentType.Application.Json)
        }

        get("/addPlayer/{playerName}/game/{gameName}") {
            val gameName: String? = call.parameters["gameName"]
            val playerName: String? = call.parameters["playerName"]
            if(gameName.isNullOrEmpty() || playerName.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Game name or player name is invalid")
                return@get
            }

            val status = gameApplication.addPlayerToGame(playerName, gameName)
            if(status == AddPlayerStatus.Ok || status == AddPlayerStatus.PlayerAlreadyAdded) {
                val response = SuccessResponse(status = "Added player $playerName to game $gameName")
                call.respondText(Gson().toJson(response), contentType = ContentType.Application.Json)
            }

            call.respond(HttpStatusCode.BadRequest, status.toString())

        }

        webSocket("/gameStatus/{gameName}/playerName/{playerName}") {
            val gameName: String? = call.parameters["gameName"]
            val playerName: String? = call.parameters["playerName"]
            println("WEB SOCKET!!")
            if(gameName.isNullOrEmpty() || playerName.isNullOrEmpty()) {
                println("Invalid game or player name")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid game or player name"))
                return@webSocket
            }

            println("Sending hi to client")
            send(Frame.Text("Hi from server"))

            if(!gameApplication.registerPlayerSocketToGame(playerName, gameName, this)) {
                println("Error registering socket")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Error registering player socket"))
                return@webSocket
            }

            try {
                incoming.consumeEach { frame ->
                    // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                    // We are only interested in textual messages, so we filter it.
                    if (frame is Frame.Text) {
                        val command = frame.readText()
                        when {
                            command.startsWith("startGame") -> gameApplication.startGame(gameName)
                            command.startsWith(prefix = "playerMove") -> {
                                val move: Move = Gson().fromJson(command.substringAfter("playerMove:"), Move::class.java)
                                gameApplication.move(gameName, move)
                            }
                        }
                    }
                }
            } finally {
                // Either if there was an error, of it the connection was closed gracefully.
                // We notify the server that the member left.
            }
        }
    }
}
data class SuccessResponse(val status: String)