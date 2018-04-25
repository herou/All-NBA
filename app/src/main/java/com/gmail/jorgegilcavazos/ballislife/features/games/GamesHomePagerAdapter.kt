package com.gmail.jorgegilcavazos.ballislife.features.games

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

/**
 * Pager Adapter for the Games home screen. Paginates between days of games.
 */
class GamesHomePagerAdapter(
    val context: Context,
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {

  companion object {
    // Number of pages available for pagination. The center page corresponds to today so there
    // would be 250 pages (days) to the left and 250 pages (days) to the right.
    const val NUM_PAGES = 501
  }

  override fun getItem(position: Int): Fragment {
    return GamesFragment.newInstance(position)
  }

  override fun getCount() = NUM_PAGES
}