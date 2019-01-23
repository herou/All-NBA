package com.gmail.jorgegilcavazos.ballislife.features.gamestats

import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.features.boxscore.QuarterByQuarterStats
import com.gmail.jorgegilcavazos.ballislife.features.gamestats.QuarterByQuarterScoreComponent.Event.StatsUpdated
import com.gmail.jorgegilcavazos.ballislife.features.model.Team
import io.reactivex.Observable

class QuarterByQuarterScoreComponent(
    parent: ViewGroup,
    events: Observable<Event>,
    homeTeam: Team,
    visitorTeam: Team
) {

  private val uiView = QuarterByQuarterScoreUIView(parent)

  init {
    uiView.setHomeTeamName(homeTeam.key)
    uiView.setHomeTeamLogo(
        parent.context.resources.getIdentifier(
            homeTeam.key.toLowerCase(), "drawable", parent.context.packageName)
    )
    uiView.setVisitorTeamName(visitorTeam.key)
    uiView.setVisitorTeamLogo(
        parent.context.resources.getIdentifier(
            visitorTeam.key.toLowerCase(), "drawable", parent.context.packageName)
    )
    uiView.setOTVisibility(false) // Hidden by default

    val disposable = events
        .subscribe { event ->
          when (event) {
            is StatsUpdated -> {
              uiView.setHomeTeamQtrByQtrStats(event.stats.homeTeamQuarterStats)
              uiView.setVisitorTeamQtrByQtrStats(event.stats.visitorTeamQuarterStats)
              uiView.setOTVisibility(event.stats.numPeriods > 4)
            }
          }
        }
  }

  sealed class Event {
    data class StatsUpdated(val stats: QuarterByQuarterStats) : Event()
  }
}