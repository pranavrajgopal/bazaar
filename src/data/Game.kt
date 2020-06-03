package data

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

data class Game(val name: String,
                var players: ConcurrentMap<String, Player>,
                var deck: Stack<Company> = Stack(),
                var marketInvestments: MutableList<MarketInvestment> = mutableListOf(),
                var status: GameStatus = GameStatus.Pending,
                var currentPlayer: Player? = null,
                var monopolies: MutableMap<CompanyType, Player> = mutableMapOf(),
                var lastMove: Move? = null) {
//        fun deepCopy() : Game {
//            val playersMap = ConcurrentHashMap<String,Player>().apply {
//                players.forEach {
//                    this[it.key] = it.value.copy()
//                }
//            }
//            return Game(name,
//                        ,
//                        Stack<Company>().apply { this.addAll(deck) },
//                        status,
//                        ,
//                        HashMap(monopolies),
//                        lastMove.copy())
//        }
        fun pending() = status == GameStatus.Pending
}

enum class CompanyType {
    Gringotts, Ollivanders, WeasleysWizardWheezes, LeakyCauldron, FlourishAndBlotts
}

data class Company(val type: CompanyType, val cardCount: Int)

val CompanyList: List<Company> = listOf(
    Company(CompanyType.WeasleysWizardWheezes, 5),
    Company(CompanyType.Gringotts, 6),
    Company(CompanyType.WeasleysWizardWheezes, 7),
    Company(CompanyType.Ollivanders, 8),
    Company(CompanyType.FlourishAndBlotts, 9),
    Company(CompanyType.LeakyCauldron, 10)
)

data class MarketInvestment(val company: Company,
                            var dollarCount: Int)
enum class GameStatus {
    Pending, Started, Finished
}