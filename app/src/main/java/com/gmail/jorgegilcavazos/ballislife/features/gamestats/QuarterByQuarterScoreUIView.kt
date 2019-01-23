package com.gmail.jorgegilcavazos.ballislife.features.gamestats

import android.support.annotation.DrawableRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.features.boxscore.QuarterByQuarterStats
import com.gmail.jorgegilcavazos.ballislife.features.boxscore.TeamQuarterStats
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeLogo
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeName
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreOT
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreQ1
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreQ2
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreQ3
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreQ4
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.homeScoreTotal
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.overtimeViews
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorLogo
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorName
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreOT
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreQ1
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreQ2
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreQ3
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreQ4
import kotlinx.android.synthetic.main.stats_quarter_by_quarter_points.view.visitorScoreTotal

class QuarterByQuarterScoreUIView(parent: ViewGroup) {

  private val view: View = LayoutInflater.from(parent.context)
      .inflate(R.layout.stats_quarter_by_quarter_points, parent, false)

  init {
    parent.addView(view)
  }

  fun setHomeTeamName(name: String) {
    view.homeName.text = name
  }

  fun setHomeTeamLogo(@DrawableRes logoRes: Int) {
    view.homeLogo.setImageResource(logoRes)
  }

  fun setVisitorTeamName(name: String) {
    view.visitorName.text = name
  }

  fun setVisitorTeamLogo(@DrawableRes logoRes: Int) {
    view.visitorLogo.setImageResource(logoRes)
  }

  fun setHomeTeamQtrByQtrStats(teamQuarterStats: TeamQuarterStats) {
    with(teamQuarterStats) {
      view.homeScoreQ1.text = q1.toString()
      view.homeScoreQ2.text = q2.toString()
      view.homeScoreQ3.text = q3.toString()
      view.homeScoreQ4.text = q4.toString()
      view.homeScoreOT.text = (ot1 + ot2 + ot3 + ot4).toString()
      view.homeScoreTotal.text = total.toString()
    }
  }

  fun setVisitorTeamQtrByQtrStats(teamQuarterStats: TeamQuarterStats) {
    with(teamQuarterStats) {
      view.visitorScoreQ1.text = q1.toString()
      view.visitorScoreQ2.text = q2.toString()
      view.visitorScoreQ3.text = q3.toString()
      view.visitorScoreQ4.text = q4.toString()
      view.visitorScoreOT.text = (ot1 + ot2 + ot3 + ot4).toString()
      view.visitorScoreTotal.text = total.toString()
    }
  }

  fun setOTVisibility(visible: Boolean) {
    view.overtimeViews.visibility = if (visible) View.VISIBLE else View.GONE
  }
}