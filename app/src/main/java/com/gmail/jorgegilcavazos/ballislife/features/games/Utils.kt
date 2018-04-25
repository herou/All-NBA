package com.gmail.jorgegilcavazos.ballislife.features.games

import java.util.Calendar

/**
 * Returns the date of a given page of the adapter. E.g. if the position is the center page
 * (NUM_PAGES / 2) then the date returned is today, if the position is the center page + 1 then
 * the date returned is tomorrow.
 */
fun getDateForPosition(position: Int): Long {
  val todayPage = GamesHomePagerAdapter.NUM_PAGES / 2
  val date = Calendar.getInstance()

  return if (position == todayPage) {
    date.timeInMillis
  } else {
    date.add(Calendar.DAY_OF_YEAR, -1 * (todayPage - position))
    date.timeInMillis
  }
}

/**
 * Returns the position in the adapter that a given page corresponds to. E.g. if the date is
 * today then the position should be the page in the center (NUM_PAGES / 2), if the date is
 * yesterday then the position is the center most page - 1.
 */
fun getPositionForDate(date: Long): Int {
  val selectedDate = Calendar.getInstance()
  selectedDate.timeInMillis = date
  val today = Calendar.getInstance()

  val dayDiff = getDayDifferenceBetweenDates(today, selectedDate)

  return GamesHomePagerAdapter.NUM_PAGES / 2 - dayDiff
}

private fun getDayDifferenceBetweenDates(today: Calendar, date: Calendar): Int {
  today.set(Calendar.HOUR_OF_DAY, 0)
  today.set(Calendar.MINUTE, 0)
  today.set(Calendar.SECOND, 0)

  date.set(Calendar.HOUR_OF_DAY, 0)
  date.set(Calendar.MINUTE, 0)
  date.set(Calendar.SECOND, 0)

  val millisDiff = today.timeInMillis - date.timeInMillis
  return (millisDiff / (1000 * 60 * 60 * 24)).toInt()
}