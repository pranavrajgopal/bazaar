import com.google.gson.Gson
import data.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import java.util.*
import java.util.concurrent.ConcurrentHashMap

public class GameServer (private val name: String) {
    private val players = ConcurrentHashMap<String, Player>()
    private val playerWebSockets = ConcurrentHashMap<String, WebSocketSession>()
    private var gameState: Game = Game(name, players)

    suspend fun addPlayer(name: String): AddPlayerStatus {
        if(name.isEmpty()) return AddPlayerStatus.InvalidPlayer
        if(!gameState.pending()) return AddPlayerStatus.GameAlreadyStarted
        if(players.size == 7) return AddPlayerStatus.PlayerLimitExceeded
        if(players.containsKey(name)) return AddPlayerStatus.PlayerAlreadyAdded
        players[name] = Player(name, ConcurrentHashMap(), mutableListOf(), Money(10, 0))
        broadCastGameStatus()
        return AddPlayerStatus.Ok
    }

    suspend fun registerPlayerWebSocket(playerName: String, webSocketSession: WebSocketSession): Boolean {
        if(players.containsKey(playerName)) {
            println("player is valid")
            if(playerWebSockets.containsKey(playerName)) {
                playerWebSockets[playerName]?.close()
            }
            playerWebSockets[playerName] = webSocketSession
            webSocketSession.send(Frame.Text("Hello $playerName! You joined the game $name"))
            webSocketSession.send(Frame.Text(Gson().toJson(gameState)))
            return true
        }

        return false
    }

    suspend fun startGame() {
        if(gameState.status == GameStatus.Pending) {
            initializeGame(gameState)
            broadCastGameStatus()
        }
    }

    suspend fun playerMove(move: Move) {
        var moveErrorStatus = MoveErrorStatus.None
        when(move.moveType) {
            MoveType.DrawMarketItem -> {
                moveErrorStatus = DrawMarketInvestmentMove.moveValidation(move, gameState)
                DrawMarketInvestmentMove.updateGameState(move, moveErrorStatus, gameState)
            }
            MoveType.PlaceMoneyOnMarketInvestments -> {
                moveErrorStatus = PlaceMoneyOnMarketInvestmentsMove.moveValidation(move, gameState)
                PlaceMoneyOnMarketInvestmentsMove.updateGameState(move, moveErrorStatus, gameState)
            }
            MoveType.DrawFromDeck -> {
                moveErrorStatus = DrawCardMove.moveValidation(move, gameState)
                DrawCardMove.updateGameState(move, moveErrorStatus, gameState)
            }
            MoveType.DiscardInvestment -> {
                moveErrorStatus = DiscardInvestmentToMarket.moveValidation(move, gameState)
            }
            MoveType.PlaceInvestmentInPortfolio -> {
                moveErrorStatus = PlaceInvestmentInPortfolioMove.moveValidation(move, gameState)
            }
        }

        broadCastGameStatus()
    }

    suspend fun handleInvalidMove(move: Move, moveErrorStatus: MoveErrorStatus) {

    }

    suspend fun broadCastGameStatus() {
        playerWebSockets.iterator().forEach { tuple ->
            println("broadcasting message to clients!")
//            tuple.value.send(Frame.Text("Hello ${tuple.key}! Game data for - ${name}"))
            tuple.value.send(Frame.Text(Gson().toJson(gameState)))
        }
    }
}