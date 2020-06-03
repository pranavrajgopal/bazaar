import data.Company
import data.CompanyList
import data.Game
import data.GameStatus
import java.util.*
import kotlin.random.Random


fun initializeGame(game: Game) {
        game.status = GameStatus.Started
        game.currentPlayer = game.players.values.toList()[Random.nextInt(0, game.players.values.size - 1)]
        val deck: Stack<Company> = initializeDeck()
        game.players.values.forEach { player ->
            repeat(3) {
                player.hand.add(deck.pop())
            }
        }
        game.deck = deck
}

private fun initializeDeck(): Stack<Company> {
    val deckList: MutableList<Company> = mutableListOf()
    CompanyList.forEach { company ->
        deckList.addAll(MutableList(company.cardCount) { company.copy() })
    }
    deckList.shuffle()
    return Stack<Company>().apply { this.addAll((deckList.dropLast(5))) }
}