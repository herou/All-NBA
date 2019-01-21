package com.gmail.jorgegilcavazos.ballislife.features.gamethread

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger
import com.gmail.jorgegilcavazos.ballislife.analytics.GoPremiumOrigin
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEvent
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEventParam
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor.Result.Failure
import com.gmail.jorgegilcavazos.ballislife.data.interactor.GameUpdatesInteractor.Result.Update
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryComponent.Event
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryUIView.GameSummaryUiEvent.BackPressed
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryUIView.GameSummaryUiEvent.StreamChecked
import com.gmail.jorgegilcavazos.ballislife.features.gamedetail.GameSummaryUIView.GameSummaryUiEvent.StreamUnchecked
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesFragment
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.PagerAdapter.BOX_SCORE_TAB
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.PagerAdapter.GAME_THREAD_TAB
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.PagerAdapter.POST_GAME_TAB
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.StreamChangesBus.StreamMode.OFF
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.StreamChangesBus.StreamMode.ON
import com.gmail.jorgegilcavazos.ballislife.features.gopremium.GoPremiumActivity
import com.gmail.jorgegilcavazos.ballislife.features.main.BaseNoActionBarActivity
import com.gmail.jorgegilcavazos.ballislife.features.model.GameStatus
import com.gmail.jorgegilcavazos.ballislife.features.model.NbaGame
import com.gmail.jorgegilcavazos.ballislife.features.model.Team
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
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


class CommentsActivity : BaseNoActionBarActivity(), View.OnClickListener {

  @Inject lateinit var eventLogger: EventLogger
  @Inject lateinit var gameUpdatesInteractor: GameUpdatesInteractor
  @Inject lateinit var disposables: CompositeDisposable
  @Inject lateinit var premiumService: PremiumService
  @Inject lateinit var streamChangesBus: StreamChangesBus

  private lateinit var homeTeam: String
  private lateinit var visitorTeam: String
  private lateinit var gameId: String
  private var date = 0L
  private lateinit var gameStatus: String

  private lateinit var pagerAdapter: PagerAdapter
  private lateinit var rewardedVideoAd: RewardedVideoAd

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

    rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
    rewardedVideoAd.rewardedVideoAdListener = object : RewardedVideoAdListener {
      override fun onRewardedVideoAdClosed() { }
      override fun onRewardedVideoAdLeftApplication() { }
      override fun onRewardedVideoAdLoaded() { }
      override fun onRewardedVideoAdOpened() { }
      override fun onRewardedVideoCompleted() { }
      override fun onRewarded(item: RewardItem?) {
        localRepository.saveGameStreamAsUnlocked(gameId)
        Toast.makeText(this@CommentsActivity, R.string.game_unlocked, Toast.LENGTH_SHORT).show()
      }
      override fun onRewardedVideoStarted() { }
      override fun onRewardedVideoAdFailedToLoad(p0: Int) { }
    }
    rewardedVideoAd.loadAd(getString(R.string.video_reward_id), AdRequest.Builder().build())

    fab.hide()

    pagerAdapter = PagerAdapter(this, supportFragmentManager, bundle)
    pager.adapter = pagerAdapter
    pager.offscreenPageLimit = 2
    tabLayout.setupWithViewPager(pager)
    pager.addOnPageChangeListener(object : OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {}
      override fun onPageScrolled(position: Int, offset: Float, OffsetPixels: Int) {}
      override fun onPageSelected(position: Int) { if (position == BOX_SCORE_TAB) fab.hide() }
    })

    setSelectedTab(intent.getStringExtra(GamesFragment.GAME_STATUS))

    fab.setOnClickListener(this)

    gameSummaryComponent = GameSummaryComponent(
        parent = gameSummary,
        events = gameSummaryEvents,
        homeTeam = Team.fromKey(homeTeam),
        visitorTeam = Team.fromKey(visitorTeam)
    )
    if (localRepository.isGameThreadStreamingEnabled) {
      gameSummaryEvents.accept(Event.StreamingEnabled)
    } else {
      gameSummaryEvents.accept(Event.StreamingDisabled)
    }
  }

  override fun onStart() {
    super.onStart()
    gameSummaryComponent.getUiEvents()
        .subscribe { event ->
          when (event) {
            BackPressed -> finish()
            StreamChecked -> {
              if (premiumService.isPremium() || localRepository.isGameStreamUnlocked(gameId)) {
                eventLogger.logEvent(SwishEvent.STREAM, null)
                localRepository.isGameThreadStreamingEnabled = true
                streamChangesBus.notifyChanged(ON)
              } else {
                openUnlockVsPremiumDialog()
                gameSummaryEvents.accept(Event.StreamingDisabled)
              }
            }
            StreamUnchecked -> {
              // TODO: set comment delay to NONE
              localRepository.isGameThreadStreamingEnabled = false
              streamChangesBus.notifyChanged(OFF)
            }
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
    rewardedVideoAd.resume(this)
  }

  override fun onPause() {
    rewardedVideoAd.pause(this)
    super.onPause()
  }

  override fun onStop() {
    disposables.clear()
    super.onStop()
  }

  override fun onDestroy() {
    rewardedVideoAd.destroy(this)
    super.onDestroy()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.fab -> addComment()
    }
  }

  private fun addComment() {
    val pos = pager.currentItem
    val fragment = pagerAdapter.getItem(pos)

    if (pos == 0 || pos == 2) {
      val gameThreadFragment = fragment as GameThreadFragment
      gameThreadFragment.fabClicked()
    }
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

  private fun setSelectedTab(gameStatus: String) {
    when {
      localRepository.openBoxScoreByDefault -> pager.currentItem = BOX_SCORE_TAB
      gameStatus == GameStatus.POST.code -> pager.currentItem = POST_GAME_TAB
      else -> pager.currentItem = GAME_THREAD_TAB
    }
  }

  private fun openUnlockVsPremiumDialog() {
    MaterialDialog.Builder(this)
        .title(R.string.unlock_game_title)
        .content(R.string.unlock_game_content)
        .positiveText(R.string.go_premium_no_excl)
        .negativeText(R.string.unlock_game_watch_video)
        .onPositive { _, _ ->
          logGoPremiumFromStream()
          purchasePremium()
        }
        .onNegative { _, _ ->
          rewardedVideoAd.show()
        }
        .build()
        .show()
  }

  private fun logGoPremiumFromStream() {
    val params = Bundle()
    params.putString(
        SwishEventParam.GO_PREMIUM_ORIGIN.key,
        GoPremiumOrigin.GAME_THREAD_STREAM.originName
    )
    eventLogger.logEvent(SwishEvent.GO_PREMIUM, params)
  }

  private fun purchasePremium() {
    startActivity(Intent(this, GoPremiumActivity::class.java))
  }

  companion object {
    const val GAME_ID_KEY = "gameId"
    const val HOME_TEAM_KEY = "homeTeamKey"
    const val AWAY_TEAM_KEY = "awayTeamKey"
  }
}
