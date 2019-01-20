package com.gmail.jorgegilcavazos.ballislife.features.gamethread

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor.Result.Failure
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor.Result.Update
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryUIView.GameSummaryUiEvent
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesFragment
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.PagerAdapter.BOX_SCORE_TAB
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.PagerAdapter.POST_GAME_TAB
import com.gmail.jorgegilcavazos.ballislife.features.main.BaseNoActionBarActivity
import com.gmail.jorgegilcavazos.ballislife.features.model.NbaGame
import com.gmail.jorgegilcavazos.ballislife.features.model.Team
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.comments_activity.fab
import kotlinx.android.synthetic.main.comments_activity.gameSummary
import kotlinx.android.synthetic.main.comments_activity.pager
import kotlinx.android.synthetic.main.comments_activity.tabLayout
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject


class CommentsActivity : BaseNoActionBarActivity(), TabLayout.OnTabSelectedListener,
    ViewPager.OnPageChangeListener, View.OnClickListener {

  @Inject lateinit var eventLogger: EventLogger
  @Inject lateinit var gameUpdatesInteractor: GameUpdatesInteractor
  @Inject lateinit var disposables: CompositeDisposable

  private lateinit var homeTeam: String
  private lateinit var visitorTeam: String
  private lateinit var gameId: String
  private var date = 0L
  private lateinit var gameStatus: String

  private var pagerAdapter: PagerAdapter? = null

  private val gameSummaryEvents = PublishRelay.create<GameSummaryComponent.Event>()
  private lateinit var gameSummaryComponent: GameSummaryComponent

  override fun injectAppComponent() {
    BallIsLifeApplication.getAppComponent().inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.comments_activity)

    val intent = intent
    homeTeam = intent.getStringExtra(GamesFragment.GAME_THREAD_HOME)
    visitorTeam = intent.getStringExtra(GamesFragment.GAME_THREAD_AWAY)
    gameId = intent.getStringExtra(GamesFragment.GAME_ID)
    date = intent.getLongExtra(GamesFragment.GAME_DATE, -1)
    gameStatus = intent.getStringExtra(GamesFragment.GAME_STATUS)

    val bundle = Bundle()
    bundle.putString(HOME_TEAM_KEY, homeTeam)
    bundle.putString(AWAY_TEAM_KEY, visitorTeam)
    bundle.putString(GAME_ID_KEY, gameId)
    bundle.putLong(GameThreadFragment.GAME_DATE_KEY, date)

    // Initialize tab layout and add three tabs.
    tabLayout!!.tabGravity = TabLayout.GRAVITY_FILL
    tabLayout!!.addTab(tabLayout!!.newTab().setText(R.string.game_thread))
    tabLayout!!.addTab(tabLayout!!.newTab().setText(R.string.box_score))
    tabLayout!!.addTab(tabLayout!!.newTab().setText(R.string.post_game_thread))
    fab!!.hide()

    pagerAdapter = PagerAdapter(supportFragmentManager, tabLayout!!.tabCount, bundle)
    pager!!.adapter = pagerAdapter
    pager!!.offscreenPageLimit = 2
    pager!!.addOnPageChangeListener(this)
    tabLayout!!.addOnTabSelectedListener(this)

    setSelectedTab(intent.getStringExtra(GamesFragment.GAME_STATUS))

    fab!!.setOnClickListener(this)

    gameSummaryComponent = GameSummaryComponent(
        parent = gameSummary,
        events = gameSummaryEvents,
        homeTeam = Team.fromKey(homeTeam),
        visitorTeam = Team.fromKey(visitorTeam)
    )
  }

  override fun onStart() {
    super.onStart()
    gameSummaryComponent.getUiEvents()
        .subscribe { event ->
          when (event) {
            GameSummaryUiEvent.BackPressed -> finish()
          }
        }
        .addTo(disposables)

    gameUpdatesInteractor.updates
        .map { result ->
          when (result) {
            is Update -> GameSummaryComponent.Event.GameUpdated(result.game)
            is Failure -> GameSummaryComponent.Event.GameUpdateFailed(result.e)
          }
        }
        .startWith(GameSummaryComponent.Event.GameLoading)
        .subscribe(gameSummaryEvents)
        .addTo(disposables)

    Observable.interval(30, SECONDS)
        .filter { gameStatus == NbaGame.PRE_GAME || gameStatus == NbaGame.IN_GAME }
        .subscribe { gameUpdatesInteractor.refresh(gameId) }
        .addTo(disposables)

    gameUpdatesInteractor.refresh(gameId)
  }

  override fun onResume() {
    super.onResume()
    eventLogger.setCurrentScreen(this, SwishScreen.GAME_DETAIL)
  }

  override fun onStop() {
    disposables.clear()
    super.onStop()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      // Respond to the action bar's Up/Home button
      android.R.id.home -> {
        onBackPressed()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onTabSelected(tab: TabLayout.Tab) {
    pager.currentItem = tab.position
    when (tab.position) {
      1 -> {
        fab!!.hide()
        expandToolbar()
      }
    }
  }

  override fun onTabUnselected(tab: TabLayout.Tab) {

  }

  override fun onTabReselected(tab: TabLayout.Tab) {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.fab -> addComment()
    }
  }

  private fun addComment() {
    val pos = pager.currentItem
    val fragment = pagerAdapter!!.getItem(pos)

    if (pos == 0 || pos == 2) {
      val gameThreadFragment = fragment as GameThreadFragment
      gameThreadFragment.fabClicked()
    }
  }

  private fun expandToolbar() {
    /*
    if (toolbar!!.parent is AppBarLayout) {
      (toolbar!!.parent as AppBarLayout).setExpanded(true, true)
    }
    */
  }

  fun getFab(): FloatingActionButton = fab

  fun showFab() {
    if (pager.currentItem != 1) {
      fab.show()
    }
  }

  fun hideFab() {
    fab.hide()
  }

  override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

  }

  override fun onPageSelected(position: Int) {
    tabLayout!!.setScrollPosition(position, 0f, true)
    when (position) {
      1 -> {
        fab!!.hide()
        //expandToolbar()
      }
    }
  }

  override fun onPageScrollStateChanged(state: Int) {

  }

  private fun setSelectedTab(gameStatus: String) {
    if (localRepository.openBoxScoreByDefault) {
      val boxScoreTab = tabLayout!!.getTabAt(BOX_SCORE_TAB)
      if (boxScoreTab != null) {
        boxScoreTab.select()
      } else {
        pager.currentItem = BOX_SCORE_TAB
      }
    } else if (gameStatus == NbaGame.POST_GAME) {
      val postGameTab = tabLayout!!.getTabAt(POST_GAME_TAB)
      if (postGameTab != null) {
        postGameTab.select()
      } else {
        pager.currentItem = POST_GAME_TAB
      }
    }
  }

  companion object {
    const val GAME_ID_KEY = "gameId"
    const val HOME_TEAM_KEY = "homeTeamKey"
    const val AWAY_TEAM_KEY = "awayTeamKey"
  }
}
