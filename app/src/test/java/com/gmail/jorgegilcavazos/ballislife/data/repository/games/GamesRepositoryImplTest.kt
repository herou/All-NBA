package com.gmail.jorgegilcavazos.ballislife.data.repository.games

import com.gmail.jorgegilcavazos.ballislife.data.actions.games.GamesResult
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaGamesService
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import com.google.firebase.firestore.FirebaseFirestore
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.net.SocketTimeoutException
import java.util.*

/**
 * TODO: Hide Firebase Firestore behind mockable interface.
 */
/**
@RunWith(MockitoJUnitRunner::class)
class GamesRepositoryImplTest {

  @Mock private lateinit var gamesService: NbaGamesService

  private lateinit var repository: GamesRepositoryImpl

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)

    //repository = GamesRepositoryImpl(gamesService, mockFirestore, TrampolineSchedulerProvider())
  }

  @Test
  fun gamesWithCacheEmpty() {
    val game1 = createGame("1")
    val response = hashMapOf("9f0ji2" to game1)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))

    val testObserver = repository.games(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it.isMemoryInProgress })
    testObserver.assertValueAt(1, { it.isMemorySuccess && it.games.isEmpty() })
    testObserver.assertValueAt(2, { it.isNetworkInProgress })
    testObserver.assertValueAt(3, { it.isNetworkSuccess && it.games == listOf(game1) })
  }

  @Test
  fun gamesSortedById() {
    val game3 = createGame("3")
    val game1 = createGame("1")
    val game2 = createGame("2")
    val game4 = createGame("4")
    val response = hashMapOf("9f0i2" to game3, "fuewe" to game1, "82hf2" to game2, "fjwoe" to game4)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))

    val testObserver = repository.games(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(3, {
      it.isNetworkSuccess
          && it.games == listOf(game1, game2, game3, game4)
    })
  }

  @Test
  fun gamesWithCacheAvailable() {
    val calendar = Calendar.getInstance()
    val game1 = createGame("1", calendar)
    val game2 = createGame("2", calendar)
    val response = hashMapOf("9f0ji2" to game1, "owei" to game2)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))
    repository.saveGamesInCache(hashMapOf("9f0ji2" to game1), true)

    val testObserver = repository.games(calendar, false).test()

    testObserver.assertValueAt(0, { it.isMemoryInProgress })
    testObserver.assertValueAt(1, { it.isMemorySuccess && it.games == listOf(game1) })
    testObserver.assertValueAt(2, { it.isNetworkInProgress })
    testObserver.assertValueAt(3, { it.isNetworkSuccess && it.games == listOf(game1, game2) })
  }

  @Test
  fun gamesSkipCacheIfForceNetwork() {
    val game1 = createGame("1")
    val response = hashMapOf("9f0ji2" to game1)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))

    val testObserver = repository.games(Calendar.getInstance(), true /* forceNetwork */).test()

    testObserver.assertValueAt(0, { it.isNetworkInProgress })
    testObserver.assertValueAt(1, { it.isNetworkSuccess && it.games == listOf(game1) })
  }

  @Test
  fun cachedGamesDeliveredBeforeNetworkErrors() {
    val exception = SocketTimeoutException()
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.error(exception))

    val testObserver = repository.games(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it.isMemoryInProgress })
    testObserver.assertValueAt(1, { it.isMemorySuccess && it.games.isEmpty() })
    testObserver.assertValueAt(2, { it.isNetworkInProgress })
    testObserver.assertError({ it == exception })
  }

  @Test
  fun loadGamesWithCacheEmptyResult() {
    val game1 = createGame("1")
    val response = hashMapOf("9f0ji2" to game1)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))

    val testObserver = repository.loadGames(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it is GamesResult.LoadGamesResult.MemoryInProgress })
    testObserver.assertValueAt(1, { it is GamesResult.LoadGamesResult.NoCachedGames })
    testObserver.assertValueAt(2, { it is GamesResult.LoadGamesResult.NetworkInProgress})
    testObserver.assertValueAt(3, { it is GamesResult.LoadGamesResult.Success })
    testObserver.assertValueAt(3, { (it as GamesResult.LoadGamesResult.Success).games[0] == game1 })
  }

  @Test
  fun loadGamesWithCacheAvailableResult() {
    val game1 = createGame("1")
    val response = hashMapOf("9f0ji2" to game1)
    repository.saveGamesInCache(response)
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(response))

    val testObserver = repository.loadGames(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it is GamesResult.LoadGamesResult.MemoryInProgress })
    testObserver.assertValueAt(1, { it is GamesResult.LoadGamesResult.Success })
    testObserver.assertValueAt(1, { (it as GamesResult.LoadGamesResult.Success).games[0] == game1 })
    testObserver.assertValueAt(2, { it is GamesResult.LoadGamesResult.NetworkInProgress})
    testObserver.assertValueAt(3, { it is GamesResult.LoadGamesResult.Success })
    testObserver.assertValueAt(3, { (it as GamesResult.LoadGamesResult.Success).games[0] == game1 })
  }

  @Test
  fun loadGamesWithNonAvailableResult() {
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.just(emptyMap()))

    val testObserver = repository.loadGames(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it is GamesResult.LoadGamesResult.MemoryInProgress })
    testObserver.assertValueAt(1, { it is GamesResult.LoadGamesResult.NoCachedGames })
    testObserver.assertValueAt(2, { it is GamesResult.LoadGamesResult.NetworkInProgress})
    testObserver.assertValueAt(3, { it is GamesResult.LoadGamesResult.NoGames })
  }

  @Test
  fun loadGamesFailureResult() {
    val error = Exception()
    `when`(gamesService.getDayGames(anyString(), anyLong(), anyLong()))
        .thenReturn(Single.error(error))

    val testObserver = repository.loadGames(Calendar.getInstance(), false).test()

    testObserver.assertValueAt(0, { it is GamesResult.LoadGamesResult.MemoryInProgress })
    testObserver.assertValueAt(1, { it is GamesResult.LoadGamesResult.NoCachedGames })
    testObserver.assertValueAt(2, { it is GamesResult.LoadGamesResult.NetworkInProgress})
    testObserver.assertValueAt(3, { it is GamesResult.LoadGamesResult.Failure })
    testObserver.assertValueAt(3, { (it as GamesResult.LoadGamesResult.Failure).t == error })
  }

  private fun createGame(id: String, calendar: Calendar = Calendar.getInstance()): GameV2 {
    return GameV2(
        arena = "AT&T",
        awayTeamAbbr = "SAS",
        awayTeamCity = "San Antonio",
        awayTeamId = "1545334321",
        awayTeamKey = "SAS",
        awayTeamNickname = "Spurs",
        awayTeamScore = "110",
        city = "San Antonio",
        date = "20171110",
        gameClock = "",
        gameStatus = "",
        homeTeamAbbr = "",
        homeTeamCity = "",
        homeTeamId = "",
        homeTeamKey = "",
        homeTeamNickname = "",
        homeTeamScore = "",
        id = id,
        periodName = "",
        periodStatus = "",
        periodValue = "",
        time = "",
        timeUtc = calendar.timeInMillis / 1000,
        totalPeriods = "",
        broadcasters = mapOf(),
        seriesSummary = null)
  }
}**/