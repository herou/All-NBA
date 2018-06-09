package com.gmail.jorgegilcavazos.ballislife.features.games


import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker

import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import com.google.android.gms.ads.AdRequest
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_games_home.*
import java.util.*
import javax.inject.Inject


/**
 * Main fragment for the games screen. Contains a view pager for individual pages of day games.
 */
class GamesHomeFragment : Fragment(), DatePickerDialog.OnDateSetListener {

  @Inject lateinit var premiumService: PremiumService
  @Inject lateinit var schedulerProvider: BaseSchedulerProvider
  @Inject lateinit var disposable: CompositeDisposable
  @Inject lateinit var eventLogger: EventLogger

  companion object {
    fun newInstance() = GamesHomeFragment()
  }

  private lateinit var adapter: GamesHomePagerAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    BallIsLifeApplication.getAppComponent().inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_games_home, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setAdVisibility()

    premiumService.isPremiumUpdates()
        .subscribeOn(schedulerProvider.io())
        .observeOn(schedulerProvider.ui())
        .subscribe { setAdVisibility() }
        .addTo(disposable)

    adapter = GamesHomePagerAdapter(requireContext(), childFragmentManager)
    viewPager.adapter = adapter
    viewPager.currentItem = adapter.count / 2

    setNavigatorText()

    viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
      }

      override fun onPageSelected(position: Int) {
        setNavigatorText()
      }
    })

    navigatorLeftBtn.setOnClickListener { viewPager.currentItem = viewPager.currentItem - 1 }
    navigatorRightBtn.setOnClickListener { viewPager.currentItem = viewPager.currentItem + 1 }
    navigatorText.setOnClickListener {
      val today = Calendar.getInstance()
      val datePicker = DatePickerDialog(
          activity,
          this,
          today.get(Calendar.YEAR),
          today.get(Calendar.MONTH),
          today.get(Calendar.DAY_OF_MONTH)
      )
      // First available game in backend.
      datePicker.datePicker.minDate = 1507204822000L
      datePicker.show()
    }
  }

  override fun onResume() {
    super.onResume()
    setNavigatorText()
    eventLogger.setCurrentScreen(requireActivity(), SwishScreen.GAMES)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    disposable.clear()
  }

  private fun setAdVisibility() {
    if (premiumService.isPremium()) {
      adView.visibility = View.GONE
    } else {
      adView.visibility = View.VISIBLE
      adView.loadAd(AdRequest.Builder().build())
    }
  }

  override fun onDateSet(datePicker: DatePicker?, year: Int, month: Int, day: Int) {
    val date = Calendar.getInstance()
    date.set(Calendar.DAY_OF_MONTH, day)
    date.set(Calendar.MONTH, month)
    date.set(Calendar.YEAR, year)

    // Move to selected date.
    viewPager.currentItem = getPositionForDate(date.timeInMillis)
  }

  private fun setNavigatorText() {
    navigatorText.text = DateFormatUtil.formatNavigatorDate(
        Date(getDateForPosition(viewPager.currentItem)))
  }
}
