//import io.ktor.application.*
//import io.ktor.response.*
//import io.ktor.request.*
//import io.ktor.features.*
//import io.ktor.client.*
//import io.ktor.client.engine.apache.*
//import io.ktor.client.features.auth.*
//import io.ktor.client.features.json.*
//import io.ktor.client.request.*
//import kotlinx.coroutines.*
//import io.ktor.client.engine.cio.*
//import io.ktor.routing.*
//import io.ktor.http.*
//import io.ktor.websocket.*
//import io.ktor.http.cio.websocket.*
//import java.time.*
//import io.ktor.client.features.websocket.*
//import io.ktor.client.features.websocket.WebSockets
//import io.ktor.http.cio.websocket.Frame
//import kotlinx.coroutines.channels.*
//
//object WsClientApp {
//    @JvmStatic
//    fun main(args: Array<String>) {
//        runBlocking {
//            val client = HttpClient(CIO).config { install(WebSockets) }
//            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/myws/echo") {
//                send(Frame.Text("Hello World"))
//                for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
//                    println("Server said: " + message.readText())
//                }
//            }
//        }
//    }
//}
