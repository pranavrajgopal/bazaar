import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import data.*
import java.lang.Exception
import kotlin.reflect.typeOf

fun moveAndGameValid(move: Move, game: Game): MoveErrorStatus {
    return when {
        game.pending() -> MoveErrorStatus.InvalidGame
        game.currentPlayer?.name != move.player.name -> MoveErrorStatus.WrongPlayer
        else -> MoveErrorStatus.None
    }
}

fun updateLastMoveAndErrorStatus(move: Move, game: Game, moveErrorStatus: MoveErrorStatus) {
    move.moveStatus = MoveStatus.Invalid
    move.moveErrorStatus = moveErrorStatus
    game.lastMove = move
}

inline fun <reified T> parseJsonObjectOrNull(jsonObject: JsonObject): T? {
    return try {
        Gson().fromJson(jsonObject, T::class.java)
    } catch(e: JsonParseException) {
        null
    }
}

class DrawCardMove {
    companion object {
        fun moveValidation(move: Move, game: Game): MoveErrorStatus {
            val gameStatus: MoveErrorStatus = moveAndGameValid(move, game)
            if (gameStatus != MoveErrorStatus.None) return gameStatus
            if(move.movePhase == MovePhase.Invest) return MoveErrorStatus.WrongPhase
            if(game.marketInvestments.isEmpty()) return MoveErrorStatus.None

            if(game.lastMove!= null
                && game.lastMove!!.player.name == move.player.name
                && game.lastMove!!.moveStatus == MoveStatus.Valid
                && game.lastMove!!.moveType == MoveType.PlaceMoneyOnMarketInvestments) {
                return MoveErrorStatus.None
            }

            val playerHasMonopoly = game.marketInvestments.all {
                game.monopolies[it.company.type]?.name == move.player.name
            }

            return when {
                playerHasMonopoly -> MoveErrorStatus.None
                else -> MoveErrorStatus.PlaceMoneyOnMarketInvestments
            }
        }

        fun updateGameState(move: Move, moveErrorStatus: MoveErrorStatus, game: Game) {
            updateLastMoveAndErrorStatus(move, game, moveErrorStatus)
            if(moveErrorStatus != MoveErrorStatus.None) return

            game.currentPlayer!!.hand.add(game.deck.pop())
        }
    }
}

data class DrawMarketInvestmentMove(val marketInvestment: MarketInvestment) {
    companion object {
        fun moveValidation(move: Move, game: Game): MoveErrorStatus {
            val gameStatus = moveAndGameValid(move, game)
            if(gameStatus != MoveErrorStatus.None) return gameStatus

            if(move.moveType != MoveType.DrawMarketItem) return MoveErrorStatus.InvalidMoveType

            val moveData = parseJsonObjectOrNull<DrawMarketInvestmentMove>(move.moveData)
            if(moveData == null || game.marketInvestments.none { it.company.type == moveData.marketInvestment.company.type}) {
                return MoveErrorStatus.InvalidMoveData
            }

            return when {
                game.monopolies[moveData.marketInvestment.company.type]?.name == move.player.name -> MoveErrorStatus.HasMonopoly
                else -> MoveErrorStatus.None
            }
        }

        fun updateGameState(move: Move, moveErrorStatus: MoveErrorStatus, game: Game) {
            updateLastMoveAndErrorStatus(move, game, moveErrorStatus)
            if(moveErrorStatus != MoveErrorStatus.None) return

            val moveData = parseJsonObjectOrNull<DrawMarketInvestmentMove>(move.moveData)!!
            val currentPlayer = game.currentPlayer!!
            currentPlayer.hand.add(moveData.marketInvestment.company)
            currentPlayer.money.oneDollarCount += moveData.marketInvestment.dollarCount

            game.marketInvestments.remove(moveData.marketInvestment)
        }
    }
}

class PlaceMoneyOnMarketInvestmentsMove {
    companion object {
        fun moveValidation(move: Move, game: Game): MoveErrorStatus {
            val gameStatus = moveAndGameValid(move, game)
            if(gameStatus != MoveErrorStatus.None) return gameStatus

            if(move.moveType != MoveType.PlaceMoneyOnMarketInvestments) return MoveErrorStatus.InvalidMoveType

            val investmentCount = game.marketInvestments.filter {
                game.monopolies[it.company.type]?.name != move.player.name
            }.count()

            return when {
                investmentCount == 0 -> MoveErrorStatus.MoneyPlacementNotRequired
                investmentCount > move.player.money.oneDollarCount -> MoveErrorStatus.InsufficientFunds
                else -> MoveErrorStatus.None
            }
        }

        fun updateGameState(move: Move, moveErrorStatus: MoveErrorStatus, game: Game) {
            updateLastMoveAndErrorStatus(move, game, moveErrorStatus)
            if(moveErrorStatus != MoveErrorStatus.None) return

            game.marketInvestments.filter {
                game.monopolies[it.company.type]?.name != move.player.name
            }.forEach {
                it.dollarCount += 1
                game.currentPlayer!!.money.oneDollarCount -= 1
            }
        }
    }
}

data class PlaceInvestmentInPortfolioMove(val company: Company) {
    companion object {
        fun moveValidation(move: Move, game: Game): MoveErrorStatus {
            val gameStatus = moveAndGameValid(move, game)
            if(gameStatus != MoveErrorStatus.None) return gameStatus

            if(move.moveType != MoveType.PlaceInvestmentInPortfolio) return MoveErrorStatus.InvalidMoveType
            val moveData = parseJsonObjectOrNull<PlaceInvestmentInPortfolioMove>(move.moveData) ?: return MoveErrorStatus.InvalidMoveData

            return when {
                move.player.hand.all { it.type != moveData.company.type } -> MoveErrorStatus.InvestmentMissingInPlayerHand
                else -> MoveErrorStatus.None
            }
        }

        fun updateGame(move: Move, moveErrorStatus: MoveErrorStatus, game: Game) {
            updateLastMoveAndErrorStatus(move, game, moveErrorStatus)
            if(moveErrorStatus != MoveErrorStatus.None) return

            val moveData = parseJsonObjectOrNull<PlaceInvestmentInPortfolioMove>(move.moveData)!!
            val companyType = moveData.company.type
            val player = game.currentPlayer!!

            when {
                player.investments.containsKey(moveData.company.type) -> {
                    player.investments[companyType]!!.investmentCount++
                }
                else -> player.investments[companyType] = Investment(moveData.company, 1, false)
            }

            val investmentCount = player.investments[companyType]!!.investmentCount
            val currentMonopolyPlayer = game.monopolies[companyType]
            val hasMonopoly = currentMonopolyPlayer == null
                    || currentMonopolyPlayer.name == move.player.name
                    || currentMonopolyPlayer.investments[companyType]!!.investmentCount < investmentCount

            if(hasMonopoly) game.monopolies[companyType] = player
        }
    }
}

data class DiscardInvestmentToMarket(val company: Company) {
    companion object {
        fun moveValidation(move: Move, game: Game): MoveErrorStatus {
            val gameStatus = moveAndGameValid(move, game)
            if(gameStatus != MoveErrorStatus.None) return gameStatus

            if(move.moveType != MoveType.DiscardInvestment) return MoveErrorStatus.InvalidMoveType
            val moveData = parseJsonObjectOrNull<DiscardInvestmentToMarket>(move.moveData) ?: return MoveErrorStatus.InvalidMoveData

            return when {
                move.player.hand.all { it.type != moveData.company.type } -> MoveErrorStatus.InvestmentMissingInPlayerHand
                else -> MoveErrorStatus.None
            }
        }

        fun updateGame(move: Move, moveErrorStatus: MoveErrorStatus, game: Game) {
            updateLastMoveAndErrorStatus(move, game, moveErrorStatus)
            if(moveErrorStatus != MoveErrorStatus.None) return

            val moveData = parseJsonObjectOrNull<DiscardInvestmentToMarket>(move.moveData)!!
            game.marketInvestments.add(MarketInvestment(moveData.company, 0))
            val currentHand = game.currentPlayer!!.hand
            currentHand.forEachIndexed loop@{ index, company ->
                if(company.type == moveData.company.type) {
                    currentHand.removeAt(index)
                    return@loop
                }
            }
        }
    }
}

