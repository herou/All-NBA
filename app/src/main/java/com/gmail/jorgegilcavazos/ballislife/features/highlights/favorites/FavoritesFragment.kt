package com.gmail.jorgegilcavazos.ballislife.features.highlights.favorites


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger
import com.gmail.jorgegilcavazos.ballislife.analytics.GoPremiumOrigin
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEvent
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEventParam
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.gmail.jorgegilcavazos.ballislife.features.common.EndlessRecyclerViewScrollListener
import com.gmail.jorgegilcavazos.ballislife.features.gopremium.GoPremiumActivity
import com.gmail.jorgegilcavazos.ballislife.features.highlights.HighlightAdapterV2
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity
import com.gmail.jorgegilcavazos.ballislife.features.videoplayer.VideoPlayerActivity
import com.gmail.jorgegilcavazos.ballislife.util.Constants
import com.google.android.youtube.player.YouTubeApiServiceUtil
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.crash.FirebaseCrash
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_favorites.*
import javax.inject.Inject


/**
 * Fragment used to show a list of highlights that a user has saved as favorites.
 */
class FavoritesFragment : Fragment(), FavoritesView {

  @Inject lateinit var presenter: FavoritesPresenter
  @Inject lateinit var localRepository: LocalRepository
  @Inject lateinit var premiumService: PremiumService
  @Inject lateinit var eventLogger: EventLogger

  private var listState: Parcelable? = null
  private lateinit var linearLayoutManager: LinearLayoutManager
  private lateinit var highlightAdapter: HighlightAdapterV2
  private lateinit var endlessScroller: EndlessRecyclerViewScrollListener
  private lateinit var viewType: HighlightViewType

  private val favoriteDeletions = PublishRelay.create<Highlight>()

  companion object {
    val LIST_STATE = "listState"

    fun newInstance() = FavoritesFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    BallIsLifeApplication.getAppComponent().inject(this)

    viewType = localRepository.favoriteHighlightViewType
    linearLayoutManager = LinearLayoutManager(activity)

    // Show the favorites feature card to non-premium users.
    val showSwishFavoritesCard = !isPremium()
    // Show the add to favorites card to premium users until they mark it seen.
    val showAddFavoritesCard = isPremium()
        && !localRepository.swishCardSeen(SwishCard.EMPTY_FAVORITE_HIGHLIGHTS)

    // Only of of the three showCard parameters should be true.
    highlightAdapter = HighlightAdapterV2(
        context = requireActivity(),
        highlights = mutableListOf(),
        highlightViewType = viewType,
        isPremium = isPremium(),
        showSwishFavoritesCard = showSwishFavoritesCard,
        showAddFavoritesCard = showAddFavoritesCard
    )
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_favorites, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recyclerView.layoutManager = linearLayoutManager
    recyclerView.adapter = highlightAdapter

    endlessScroller = object : EndlessRecyclerViewScrollListener(linearLayoutManager) {
      override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
        presenter.loadMore()
      }
    }

    recyclerView.addOnScrollListener(endlessScroller)

    presenter.attachView(this)
    presenter.loadMore()
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    listState = savedInstanceState?.getParcelable(LIST_STATE)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    listState = linearLayoutManager.onSaveInstanceState()
    outState.putParcelable(LIST_STATE, listState)
  }

  override fun onDestroy() {
    presenter.detachView()
    super.onDestroy()
  }

  override fun addHighlight(highlight: Highlight, addToTop: Boolean) {
    if (addToTop) {
      highlightAdapter.addHighlightToTop(highlight)
      recyclerView.scrollToPosition(0)
    } else {
      highlightAdapter.addHighlight(highlight)
    }
  }

  override fun removeHighlight(highlight: Highlight) {
    highlightAdapter.removeHighlight(highlight)
  }

  override fun favoriteClicks(): Observable<Highlight> = highlightAdapter.getFavoriteClicks()

  override fun showRemoveFromFavoritesConfirmation(highlight: Highlight) {
    MaterialDialog.Builder(requireActivity())
        .title(getString(R.string.fav_deletion_title))
        .content(getString(R.string.fav_deletion_content))
        .positiveText(getString(R.string.fav_deletion_positive))
        .negativeText(getString(R.string.fav_deletion_negative))
        .onPositive { _, _ ->  favoriteDeletions.accept(highlight)}
        .show()
  }

  override fun favoriteDeletions(): Observable<Highlight> = favoriteDeletions

  override fun isPremium(): Boolean {
    return premiumService.isPremium() || localRepository.isUserWhitelisted
  }

  override fun openHighlightEvents(): Observable<Highlight> {
    return highlightAdapter.getViewClickObservable()
  }

  override fun shareHighlightEvents(): Observable<Highlight> {
    return highlightAdapter.getShareClickObservable()
  }

  override fun openSubmissionEvents(): Observable<Highlight> {
   return highlightAdapter.getSubmissionClickObservable()
  }

  override fun openStreamable(shortCode: String) {
    val intent = Intent(activity, VideoPlayerActivity::class.java)
    intent.putExtra(VideoPlayerActivity.SHORTCODE, shortCode)
    startActivity(intent)
  }

  override fun showErrorOpeningStreamable() {
    Toast.makeText(activity, R.string.error_loading_streamable, Toast.LENGTH_SHORT).show()
  }

  override fun openYoutubeVideo(videoId: String) {
    // Verify that the API is available in the device.
    val intent = if (localRepository.openYouTubeInApp
        && YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(activity)
        == YouTubeInitializationResult.SUCCESS) {
      FirebaseCrash.logcat(Log.INFO, "FavoritesFrag", "Opening youtube video in app: " + videoId)
      YouTubeStandalonePlayer.createVideoIntent(
          activity,
          "AIzaSyA3jvG_4EIhAH_l3criaJx7-E_XWixOe78", /* API KEY */
          videoId,
          0, /* Start millisecond */
          true /* Autoplay */,
          true /* Lightbox */
      )
    } else {
      FirebaseCrash.logcat(Log.INFO, "HighlightsFrag", "Opening native youtube video" + videoId)
      Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId))
    }
    startActivity(intent)
  }

  override fun showErrorOpeningYoutube() {
    Toast.makeText(activity, R.string.error_loading_youtube, Toast.LENGTH_SHORT).show()
  }

  override fun showUnknownSourceError() {
    Toast.makeText(activity, R.string.unknown_source, Toast.LENGTH_SHORT).show()
  }

  override fun showSubmission(highlight: Highlight) {
    val intent = Intent(activity, SubmissionActivity::class.java)
    val bundle = Bundle()
    bundle.putString(Constants.THREAD_ID, highlight.id)
    bundle.putString(SubmissionActivity.KEY_TITLE, getString(R.string.highlights))
    intent.putExtras(bundle)
    startActivity(intent)
  }

  override fun showShareDialog(highlight: Highlight) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, highlight.url)
    startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.share_video)))
  }

  override fun swishCardExploreClicks(): Observable<SwishCard> {
    return highlightAdapter.getExplorePremiumClicks()
  }

  override fun swishCardGotItClicks(): Observable<SwishCard> {
    return highlightAdapter.getGotItClicks()
  }

  override fun dismissSwishCard(swishCard: SwishCard) {
    highlightAdapter.removeSwishCard(swishCard)
  }

  override fun openPremiumActivity() {
    val params = Bundle()
    params.putString(SwishEventParam.GO_PREMIUM_ORIGIN.key,
        GoPremiumOrigin.HIGHLIGHTS_FAVORITES_EXPLORE_CARD.originName)
    eventLogger.logEvent(SwishEvent.GO_PREMIUM, params)
    val intent = Intent(activity, GoPremiumActivity::class.java)
    startActivity(intent)
  }
}
