import data.Game
import data.GameStatus
import data.Move
import io.ktor.http.cio.websocket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

public class GameApplication {
    private val currentGames = ConcurrentHashMap<String, GameServer>()

    public fun createGame(name: String): Boolean {
        if(name.isEmpty() || currentGames.containsKey(name)) return false

        currentGames[name] = GameServer(name)
        return true
    }

    suspend fun addPlayerToGame(playerName: String, gameName: String): AddPlayerStatus {
        val gameServer = currentGames[gameName]
        gameServer?.let {
            return it.addPlayer(playerName)
        }

        return AddPlayerStatus.InvalidGame
    }

    suspend fun registerPlayerSocketToGame(playerName: String,
                                          gameName: String,
                                          webSocketSession: WebSocketSession): Boolean {
         currentGames[gameName]?.let {
            return it.registerPlayerWebSocket(playerName, webSocketSession)
        }

        return false
    }

    suspend fun broadcastGameStatus(gameName: String) {
        currentGames[gameName]?.let {
            return it.broadCastGameStatus()
        }
    }

    suspend fun startGame(gameName: String) {
        currentGames[gameName]?.startGame()
    }

    suspend fun move(gameName: String, move: Move) {
        currentGames[gameName]?.playerMove(move)
    }
}

public enum class AddPlayerStatus {
    Ok,
    InvalidGame,
    InvalidPlayer,
    PlayerLimitExceeded,
    PlayerAlreadyAdded,
    GameAlreadyStarted,
    UnknownError
}