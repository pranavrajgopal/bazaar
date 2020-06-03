package data

import com.google.gson.JsonObject
import moveAndGameValid
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class Player(val name: String,
                  val investments: ConcurrentHashMap<CompanyType, Investment>,
                  val hand: MutableList<Company>,
                  var money: Money)


data class Money(var oneDollarCount: Int,
                 var threeDollarCount: Int)
data class Investment(val company: Company,
                      var investmentCount: Int,
                      var monopoly: Boolean)

data class Move(val player: Player,
                val moveType:MoveType,
                var moveData: JsonObject,
                var moveStatus: MoveStatus = MoveStatus.Pending,
                var moveErrorStatus: MoveErrorStatus = MoveErrorStatus.None,
                var movePhase: MovePhase = MovePhase.Draw,
                var endTurn: Boolean = false)

enum class MoveType {
    DrawFromDeck,
    DrawMarketItem,
    PlaceMoneyOnMarketInvestments,
    DiscardInvestment,
    PlaceInvestmentInPortfolio
}

enum class MoveErrorStatus {
    None,
    InvalidGame,
    InvalidMoveType,
    WrongPlayer,
    WrongPhase,
    PlaceMoneyOnMarketInvestments,
    InsufficientFunds,
    MoneyPlacementNotRequired,
    InvalidMoveData,
    HasMonopoly,
    InvestmentMissingInPlayerHand,
    Unknown
}
enum class MoveStatus {
    Pending, Valid, Invalid
}

enum class MovePhase {
    Draw, Invest
}
