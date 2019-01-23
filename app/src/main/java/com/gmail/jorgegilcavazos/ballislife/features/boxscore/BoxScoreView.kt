package com.gmail.jorgegilcavazos.ballislife.features.boxscore

import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreValues

interface BoxScoreView {

	fun showVisitorBoxScore(values: BoxScoreValues)

	fun showHomeBoxScore(values: BoxScoreValues)

	fun showQuarterByQuarterTable(stats: QuarterByQuarterStats)

	fun setLoadingIndicator(active: Boolean)

	fun hideBoxScore()

	fun showBoxScoreNotAvailableMessage(active: Boolean)

	fun showUnknownErrorToast(code: Int)
}
