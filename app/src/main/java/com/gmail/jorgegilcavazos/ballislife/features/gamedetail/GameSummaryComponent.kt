package com.gmail.jorgegilcavazos.ballislife.features.gamedetail

import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.common.ErrorType
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.GameUpdated
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryUIView.GameState
import com.gmail.jorgegilcavazos.ballislife.features.model.GameStatus
import com.gmail.jorgegilcavazos.ballislife.features.model.GameStatus.LIVE
import com.gmail.jorgegilcavazos.ballislife.features.model.GameStatus.POST
import com.gmail.jorgegilcavazos.ballislife.features.model.GameStatus.PRE
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2
import com.gmail.jorgegilcavazos.ballislife.features.model.MediaSource
import com.gmail.jorgegilcavazos.ballislife.features.model.Team
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil
import com.gmail.jorgegilcavazos.ballislife.util.StringUtils
import com.gmail.jorgegilcavazos.ballislife.util.Utilities
import io.reactivex.Observable

class GameSummaryComponent(
    parent: ViewGroup,
    events: Observable<Event>,
    homeTeam: Team,
    visitorTeam: Team
) {

  private val uiView = GameSummaryUIView(parent)

  init {
    uiView.setHomeTeamInfo(homeTeam)
    uiView.setVisitorTeamInfo(visitorTeam)

    events
        .subscribe { event ->
          when (event) {
            is GameUpdated -> {
              val game = event.game
              val gameStatus = GameStatus.fromCode(game.gameStatus)
              when (gameStatus) {
                PRE -> {
                  uiView.setGameState(GameState.PRE)

                  uiView.setStartTime(DateFormatUtil.localizeGameTime(game.periodStatus))
                  val broadcasters = findNationalBroadcasters(game.broadcasters)
                  uiView.setBroadcasterVisibility(visible = broadcasters != null)
                  if (broadcasters != null) {
                    uiView.setBroadcaster(broadcasters)
                  }
                }
                LIVE -> {
                  uiView.setGameState(GameState.LIVE)

                  // TODO: halftime
                  uiView.setHomeScore(game.homeTeamScore)
                  uiView.setVisitorScore(game.awayTeamScore)
                  uiView.setClock(game.gameClock)
                  uiView.setPeriod(Utilities.getPeriodString(game.periodValue, game.periodName))
                }
                POST -> {
                  uiView.setGameState(GameState.POST)

                  uiView.setHomeScore(game.homeTeamScore)
                  uiView.setVisitorScore(game.awayTeamScore)
                }
              }
            }
          }
        }
  }

  fun getUiEvents(): Observable<GameSummaryUIView.GameSummaryUiEvent> {
    return uiView.getUiEvents()
  }

  private fun findNationalBroadcasters(mapMediaSources: Map<String, MediaSource>?): String? {
    if (mapMediaSources == null) {
      return ""
    }
    var brodcasters = ""
    for ((key, value) in mapMediaSources) {
      if (key == "tv") {
        for (broadcaster in value.broadcaster) {
          if (broadcaster.scope == "natl") {
            brodcasters += broadcaster.displayName + "/"
          }
        }
      }
    }
    if (!StringUtils.isNullOrEmpty(brodcasters)) {
      // Remove last "/"
      brodcasters = brodcasters.substring(0, brodcasters.length - 1)
    }
    return if (brodcasters.isBlank()) {
      null
    } else {
      brodcasters
    }
  }

  sealed class Event {
    object GameLoading : Event()
    data class GameUpdated(val game: GameV2) : Event()
    data class GameUpdateFailed(val e: ErrorType) : Event()
  }
}