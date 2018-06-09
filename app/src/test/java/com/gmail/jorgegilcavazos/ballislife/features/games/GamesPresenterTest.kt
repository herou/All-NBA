package com.gmail.jorgegilcavazos.ballislife.features.games

import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class GamesPresenterTest {

  @Mock private lateinit var mockView: GamesView
  @Mock private lateinit var mockGamesModelTransformer: GamesModelTransformer
  @Mock private lateinit var mockNetworkUtils: NetworkUtils
  @Mock private lateinit var mockErrorHandler: ErrorHandler
  @Mock private lateinit var localRepository: LocalRepository

  private val loadGamesEvents = PublishRelay.create<GamesUiEvent.LoadGamesEvent>()
  private val refreshGamesEvents = PublishRelay.create<GamesUiEvent.RefreshGamesEvent>()
  private val openGameEvents = PublishRelay.create<GamesUiEvent.OpenGameEvent>()

  private lateinit var presenter: GamesPresenter

  @Before
  fun setup() {
    `when`(mockView.loadGamesEvents()).thenReturn(loadGamesEvents)
    `when`(mockView.refreshGamesEvents()).thenReturn(refreshGamesEvents)
    `when`(mockView.openGameEvents()).thenReturn(openGameEvents)

    presenter = GamesPresenter(
        localRepository,
        mockGamesModelTransformer,
        CompositeDisposable(),
        mockNetworkUtils,
        mockErrorHandler
    )
  }

  @Test
  fun idleUiModel() {
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.Idle))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(false)
  }

  @Test
  fun memoryInProgressUiModel() {
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.MemoryInProgress))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(false)
    verify(mockView).hideGames()
    verify(mockView).setLoadingIndicator(false)
    verify(mockView).dismissSnackbar()
  }

  @Test
  fun networkInProgressUiModel() {
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.NetworkInProgress))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(false)
    verify(mockView).dismissSnackbar()
  }

  @Test
  fun successUiModel() {
    val games = listOf(createGameV2(), createGameV2())
    val date = Calendar.getInstance()
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.Success(games, date)))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(false)
    verify(mockView).setLoadingIndicator(false)
    verify(mockView).showGames(games)
  }

  @Test
  fun noCachedGamesUiModel() {
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.NoCachedGames))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(false)
    verify(mockView).setLoadingIndicator(true)
  }

  @Test
  fun noGamesUiModel() {
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.NoGames))

    presenter.attachView(mockView)

    verify(mockView).setNoGamesIndicator(true)
    verify(mockView).setLoadingIndicator(false)
  }

  @Test
  fun failureUiModelWithNetworkAvailable() {
    val error = Exception()
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.Failure(error)))
    `when`(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
    `when`(mockErrorHandler.handleError(error)).thenReturn(404)

    presenter.attachView(mockView)

    verify(mockView).showErrorSnackbar(404)
    verify(mockView).setNoGamesIndicator(false)
  }

  @Test
  fun failureUiModelWithNoNetworkAvailable() {
    val error = Exception()
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.Failure(error)))
    `when`(mockNetworkUtils.isNetworkAvailable()).thenReturn(false)

    presenter.attachView(mockView)

    verify(mockView).showNoNetSnackbar()
    verify(mockView).setNoGamesIndicator(false)
  }

  @Test
  fun openGameScreenUiModel() {
    val game = createGameV2()
    `when`(mockGamesModelTransformer.uiModels(anyObject()))
        .thenReturn(Observable.just(GamesUiModelV2.OpenGameScreen(game)))

    presenter.attachView(mockView)

    verify(mockView).showGameDetails(game)
  }

  @Test
  fun dismissSnackbarOnDetach() {
    `when`(mockGamesModelTransformer.uiModels(anyObject())).thenReturn(Observable.empty())
    presenter.attachView(mockView)
    presenter.detachView()

    verify(mockView).dismissSnackbar()
  }

  private fun createGameV2(): GameV2 {
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
        id = "",
        periodName = "",
        periodStatus = "",
        periodValue = "",
        time = "",
        timeUtc = 23894341,
        totalPeriods = "",
        broadcasters = mapOf(),
        seriesSummary = null)
  }

  private fun <T> anyObject(): T = Mockito.anyObject<T>()
}
