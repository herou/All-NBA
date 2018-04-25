package com.gmail.jorgegilcavazos.ballislife.features.games

import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.LoadGamesEvent
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.OpenGameEvent
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.RefreshGamesEvent
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import io.reactivex.Observable

interface GamesView {

  fun openGameEvents(): Observable<OpenGameEvent>

  fun loadGamesEvents(): Observable<LoadGamesEvent>

  fun refreshGamesEvents(): Observable<RefreshGamesEvent>

  fun setLoadingIndicator(active: Boolean)

  fun hideGames()

  fun showGames(games: List<GameV2>)

  fun showGameDetails(game: GameV2)

  fun setNoGamesIndicator(active: Boolean)

  fun showNoNetSnackbar()

  fun showErrorSnackbar(code: Int)

  fun dismissSnackbar()
}
