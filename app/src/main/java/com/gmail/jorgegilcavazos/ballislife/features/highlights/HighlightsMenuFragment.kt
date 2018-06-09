package com.gmail.jorgegilcavazos.ballislife.features.highlights


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.fragment_highlights_menu.*
import javax.inject.Inject

/**
 * Highlights fragment that contains a viewpager with the regular highlights feed and the favorites
 * section.
 */
class HighlightsMenuFragment : Fragment() {

  companion object {
    fun newInstance() = HighlightsMenuFragment()
  }

  @Inject lateinit var eventLogger: EventLogger
  @Inject lateinit var premiumService: PremiumService

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    BallIsLifeApplication.getAppComponent().inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_highlights_menu, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (premiumService.isPremium()) {
      adView.visibility = View.INVISIBLE
      val params = adView.layoutParams
      params.height = 0
    } else {
      adView.loadAd(AdRequest.Builder().build())
      adView.visibility = View.VISIBLE
    }

    viewPager.adapter = HighlightsPagerAdapter(requireContext(), childFragmentManager)
    tabLayout.setupWithViewPager(viewPager)
  }

  override fun onResume() {
    super.onResume()
    eventLogger.setCurrentScreen(requireActivity(), SwishScreen.HIGHLIGHTS)
  }

}
