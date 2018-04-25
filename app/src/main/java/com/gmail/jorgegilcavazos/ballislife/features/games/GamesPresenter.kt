package com.gmail.jorgegilcavazos.ballislife.features.games

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils
import com.gmail.jorgegilcavazos.ballislife.util.TeamUtils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.OnErrorNotImplementedException
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject

class GamesPresenter @Inject constructor(
    private val localRepository: LocalRepository,
    private val gamesModelTransformer: GamesModelTransformer,
    private val disposables: CompositeDisposable,
    private val networkUtils: NetworkUtils,
    private val errorHandler: ErrorHandler) : BasePresenter<GamesView>() {

  override fun attachView(view: GamesView) {
    super.attachView(view)

    val uiEvents = Observable.merge(
        view.loadGamesEvents(),
        view.refreshGamesEvents(),
        view.openGameEvents()
    )

    gamesModelTransformer.uiModels(uiEvents).subscribe({ model ->
      when (model) {
        is GamesUiModelV2.Idle -> {
          view.setNoGamesIndicator(false)
        }
        is GamesUiModelV2.MemoryInProgress -> {
          view.hideGames()
          view.setLoadingIndicator(false)
          view.setNoGamesIndicator(false)
          view.dismissSnackbar()
        }
        is GamesUiModelV2.NetworkInProgress -> {
          view.setNoGamesIndicator(false)
          view.dismissSnackbar()
        }
        is GamesUiModelV2.Success -> {
          view.showGames(gamesListWithFavoriteTeamFirst(model.games.toMutableList()))
          view.setLoadingIndicator(false)
          view.setNoGamesIndicator(false)
        }
        is GamesUiModelV2.NoCachedGames -> {
          view.setLoadingIndicator(true)
          view.setNoGamesIndicator(false)
        }
        is GamesUiModelV2.NoGames -> {
          view.setNoGamesIndicator(true)
          view.setLoadingIndicator(false)
        }
        is GamesUiModelV2.Failure -> {
          view.setLoadingIndicator(false)
          if (networkUtils.isNetworkAvailable()) {
            view.showErrorSnackbar(errorHandler.handleError(model.t))
          } else {
            view.showNoNetSnackbar()
          }
          view.setNoGamesIndicator(false)
        }
        is GamesUiModelV2.OpenGameScreen -> {
          view.showGameDetails(model.game)
        }
      }
    }, { t ->
      throw OnErrorNotImplementedException(t)
    }).addTo(disposables)
  }

  override fun detachView() {
    disposables.clear()
    view.dismissSnackbar()
    super.detachView()
  }

  /**
   * Moves the user's favorite team to the top if one is selected.
   */
  private fun gamesListWithFavoriteTeamFirst(games: MutableList<GameV2>): List<GameV2> {
    val favTeam = localRepository.favoriteTeam
    if (favTeam == null) {
      // No favorite team selected, return unchanged list.
      return games
    } else {
      val favTeamId = TeamUtils.getTeamId(favTeam)
      val favTeamGame = games.find { it.homeTeamId == favTeamId || it.awayTeamId == favTeamId }
      return if (favTeamGame == null) {
        // Favorite team not in list of games, return unchanged list.
        games
      } else {
        // Remove favorite team from current position and place at index 0, then return that list.
        games.remove(favTeamGame)
        games.add(0, favTeamGame)
        games
      }
    }
  }

}
