package com.gmail.jorgegilcavazos.ballislife.features.gamedetail

import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.common.ErrorType
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.BoxScoreSelected
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.GameUpdated
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.LiveThreadSelected
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.PostThreadSelected
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.StreamingDisabled
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event.StreamingEnabled
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
    visitorTeam: Team,
    noSpoilersModeEnabled: Boolean
) {

  private val uiView = GameSummaryUIView(parent)

  init {
    uiView.setHomeTeamInfo(homeTeam)
    uiView.setVisitorTeamInfo(visitorTeam)
    if (noSpoilersModeEnabled) uiView.setScoresToHyphen()

    val disposable = events
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

                  if (!noSpoilersModeEnabled) {
                    uiView.setHomeScore(game.homeTeamScore)
                    uiView.setVisitorScore(game.awayTeamScore)
                  }
                  uiView.setClock(game.gameClock)
                  uiView.setPeriod(Utilities.getPeriodString(game.periodValue, game.periodName))

                  if (game.periodStatus == HALFTIME_PERIOD_STATUS) {
                    uiView.setHalftimeVisibility(true)
                    uiView.setClockVisibility(false)
                    uiView.setPeriodVisibility(false)
                  } else {
                    uiView.setHalftimeVisibility(false)
                    uiView.setClockVisibility(true)
                    uiView.setPeriodVisibility(true)
                  }
                }
                POST -> {
                  uiView.setGameState(GameState.POST)

                  if (!noSpoilersModeEnabled) {
                    uiView.setHomeScore(game.homeTeamScore)
                    uiView.setVisitorScore(game.awayTeamScore)
                  }
                }
              }
            }
            StreamingEnabled -> uiView.setStreamEnabled(true)
            StreamingDisabled -> uiView.setStreamEnabled(false)
            LiveThreadSelected -> {
              uiView.setStreamSwitchVisibility(visible = true)
              uiView.setDelayButtonVisibility(visible = true)
            }
            BoxScoreSelected, PostThreadSelected -> {
              uiView.setStreamSwitchVisibility(visible = false)
              uiView.setDelayButtonVisibility(visible = false)
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
    object StreamingEnabled : Event()
    object StreamingDisabled : Event()
    object LiveThreadSelected : Event()
    object BoxScoreSelected : Event()
    object PostThreadSelected : Event()
  }

  companion object {
    private const val HALFTIME_PERIOD_STATUS = "Halftime"
  }
}