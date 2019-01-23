package com.gmail.jorgegilcavazos.ballislife.features.boxscore

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter
import com.gmail.jorgegilcavazos.ballislife.data.repository.boxscore.BoxScoreRepository
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject

class BoxScorePresenter @Inject constructor(
		private val boxScoreRepository: BoxScoreRepository,
		private val disposable: CompositeDisposable,
		private val errorHandler: ErrorHandler): BasePresenter<BoxScoreView>() {

	override fun detachView() {
		disposable.clear()
		super.detachView()
	}

	fun loadBoxScore(gameId: String, selectedTeam: BoxScoreSelectedTeam, forceNetwork: Boolean) {
		disposable.clear()
		boxScoreRepository.boxScore(gameId, forceNetwork)
				.subscribe({ boxModel: BoxScoreUIModel ->
					if (boxModel.inProgress) {
						view.hideBoxScore()
						view.setLoadingIndicator(forceNetwork)
						view.showBoxScoreNotAvailableMessage(false)
					}

					if (boxModel.notAvailable) {
						view.setLoadingIndicator(false)
						view.showBoxScoreNotAvailableMessage(true)
						view.hideBoxScore()
					}

					if (boxModel.success) {
						when (selectedTeam) {
							BoxScoreSelectedTeam.HOME -> view.showHomeBoxScore(boxModel.boxScore!!.game)
							BoxScoreSelectedTeam.VISITOR -> view.showVisitorBoxScore(boxModel.boxScore!!.game)
						}

						view.setLoadingIndicator(false)
						view.showBoxScoreNotAvailableMessage(false)
					}
				}, { e: Throwable ->
					view.showUnknownErrorToast(errorHandler.handleError(e))
				})
				.addTo(disposable)
	}
}
