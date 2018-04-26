package com.gmail.jorgegilcavazos.ballislife.features.playoffs.bracket


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.data.repository.games.MatchUp
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.gmail.jorgegilcavazos.ballislife.util.TeamUtils
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import com.google.firebase.firestore.FirebaseFirestore
import de.aaronoe.rxfirestore.getSingle
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_bracket.box1
import kotlinx.android.synthetic.main.fragment_bracket.box10
import kotlinx.android.synthetic.main.fragment_bracket.box11
import kotlinx.android.synthetic.main.fragment_bracket.box12
import kotlinx.android.synthetic.main.fragment_bracket.box13
import kotlinx.android.synthetic.main.fragment_bracket.box14
import kotlinx.android.synthetic.main.fragment_bracket.box15
import kotlinx.android.synthetic.main.fragment_bracket.box2
import kotlinx.android.synthetic.main.fragment_bracket.box3
import kotlinx.android.synthetic.main.fragment_bracket.box4
import kotlinx.android.synthetic.main.fragment_bracket.box5
import kotlinx.android.synthetic.main.fragment_bracket.box6
import kotlinx.android.synthetic.main.fragment_bracket.box7
import kotlinx.android.synthetic.main.fragment_bracket.box8
import kotlinx.android.synthetic.main.fragment_bracket.box9
import kotlinx.android.synthetic.main.layout_bracket_box.view.team1Logo
import kotlinx.android.synthetic.main.layout_bracket_box.view.team1Name
import kotlinx.android.synthetic.main.layout_bracket_box.view.team1Wins
import kotlinx.android.synthetic.main.layout_bracket_box.view.team2Logo
import kotlinx.android.synthetic.main.layout_bracket_box.view.team2Name
import kotlinx.android.synthetic.main.layout_bracket_box.view.team2Wins
import javax.inject.Inject

class BracketFragment : Fragment() {

  @Inject lateinit var firestore: FirebaseFirestore
  @Inject lateinit var schedulerProvider: BaseSchedulerProvider
  @Inject lateinit var disposable: CompositeDisposable

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    BallIsLifeApplication.getAppComponent().inject(this)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? = inflater.inflate(R.layout.fragment_bracket, container, false)

  override fun onStart() {
    super.onStart()
    val playoffsRef = firestore.collection("playoff_picture").document("2018").collection("1")
    playoffsRef.getSingle<MatchUp>()
        .observeOn(schedulerProvider.ui())
        .subscribe { matchUps ->
          for (matchUp in matchUps) {
            when (matchUp.conference) {
              "west" -> {
                when (matchUp.round) {
                  1 -> {
                    val box = when (matchUp.team1_seed) {
                      1, 8 -> box1
                      2, 7 -> box4
                      3, 6 -> box3
                      4, 5 -> box2
                      else -> throw IllegalStateException()
                    }
                    setBoxData(box, matchUp)
                  }
                  2 -> {
                    val box = when (matchUp.team1_seed) {
                      2, 7, 3, 6 -> box6
                      1, 8, 4, 5 -> box5
                      else -> throw IllegalStateException()
                    }
                    setBoxData(box, matchUp)
                  }
                  3 -> {
                    setBoxData(box7, matchUp)
                  }
                }
              }
              "east" -> {
                when (matchUp.round) {
                  1 -> {
                    val box = when (matchUp.team1_seed) {
                      1, 8 -> box8
                      2, 7 -> box11
                      3, 6 -> box10
                      4, 5 -> box9
                      else -> throw IllegalStateException()
                    }
                    setBoxData(box, matchUp)
                  }
                  2 -> {
                    val box = when (matchUp.team1_seed) {
                      2, 7, 3, 6 -> box13
                      1, 8, 4, 5 -> box12
                      else -> throw IllegalStateException()
                    }
                    setBoxData(box, matchUp)
                  }
                  3 -> {
                    setBoxData(box14, matchUp)
                  }
                }
              }
              "finals" -> {
                setBoxData(box15, matchUp)
              }
            }
          }
        }
        .addTo(disposable)
  }

  override fun onStop() {
    disposable.clear()
    super.onStop()
  }

  private fun setBoxData(box: View, matchUp: MatchUp) {
    box.apply {
      if (matchUp.team1.isNotEmpty()) {
        team1Logo.setImageResource(TeamUtils.getTeamLogo(matchUp.team1))
        team1Name.text = matchUp.team1
        team1Wins.text = matchUp.team1_wins.toString()
      }

      if (matchUp.team2.isNotEmpty()) {
        team2Logo.setImageResource(TeamUtils.getTeamLogo(matchUp.team2))
        team2Name.text = matchUp.team2
        team2Wins.text = matchUp.team2_wins.toString()
      }
    }
  }

  companion object {
    fun newInstance() = BracketFragment()
  }
}
