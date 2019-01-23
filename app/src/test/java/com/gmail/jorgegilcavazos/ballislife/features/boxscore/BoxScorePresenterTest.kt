package com.gmail.jorgegilcavazos.ballislife.features.boxscore

import com.gmail.jorgegilcavazos.ballislife.data.repository.boxscore.BoxScoreRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreResponse
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreTeam
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreValues
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BoxScorePresenterTest {

	@Mock private lateinit var mockView: BoxScoreView
	@Mock private lateinit var mockBoxScoreRepository: BoxScoreRepository
	@Mock private lateinit var mockDisposables: CompositeDisposable
	@Mock private lateinit var mockErrorHandler: ErrorHandler

	private lateinit var presenter: BoxScorePresenter

	@Before
	fun setup() {
		MockitoAnnotations.initMocks(this)

		presenter = BoxScorePresenter(mockBoxScoreRepository, mockDisposables, mockErrorHandler)
		presenter.attachView(mockView)
	}

	@Test
	fun clearSubscriptionsOnDetach() {
		presenter.detachView()

		verify(mockDisposables).clear()
	}

	@Test
	fun loadHomeBoxScoreSuccess() {
		val boxScore = createBoxScoreResponse()
		`when`(mockBoxScoreRepository.boxScore("GAME_ID", true))
				.thenReturn(Observable.just(
						BoxScoreUIModel.inProgress(),
						BoxScoreUIModel.success(boxScore)))

		presenter.loadBoxScore("GAME_ID", BoxScoreSelectedTeam.HOME, true)

		verify(mockView).hideBoxScore()
		verify(mockView).setLoadingIndicator(true)
		verify(mockView, times(2)).showBoxScoreNotAvailableMessage(false)
		verify(mockView).showHomeBoxScore(boxScore.game)
		verify(mockView).setLoadingIndicator(false)
	}

	@Test
	fun loadVisitorBoxScoreSuccess() {
		val boxScore = createBoxScoreResponse()
		`when`(mockBoxScoreRepository.boxScore("GAME_ID", true))
				.thenReturn(Observable.just(
						BoxScoreUIModel.inProgress(),
						BoxScoreUIModel.success(boxScore)))

		presenter.loadBoxScore("GAME_ID", BoxScoreSelectedTeam.VISITOR, true)

		verify(mockView).hideBoxScore()
		verify(mockView).setLoadingIndicator(true)
		verify(mockView, times(2)).showBoxScoreNotAvailableMessage(false)
		verify(mockView).showVisitorBoxScore(boxScore.game)
		verify(mockView).setLoadingIndicator(false)
	}

	@Test
	fun loadBoxScoreNotAvailable() {
		`when`(mockBoxScoreRepository.boxScore("GAME_ID", true))
				.thenReturn(Observable.just(
						BoxScoreUIModel.inProgress(),
						BoxScoreUIModel.notAvailable()))

		presenter.loadBoxScore("GAME_ID", BoxScoreSelectedTeam.HOME, true)

		verify(mockView, times(2)).hideBoxScore()
		verify(mockView).setLoadingIndicator(true)
		verify(mockView).showBoxScoreNotAvailableMessage(false)
		verify(mockView).showBoxScoreNotAvailableMessage(true)
		verify(mockView).setLoadingIndicator(false)
	}

	@Test
	fun showUnknownErrorToast() {
		val error = Exception()
		`when`(mockBoxScoreRepository.boxScore("GAME_ID", true))
				.thenReturn(Observable.error(error))
		`when`(mockErrorHandler.handleError(error)).thenReturn(-1)

		presenter.loadBoxScore("GAME_ID", BoxScoreSelectedTeam.HOME, true)

		verify(mockView).showUnknownErrorToast(-1)
	}

	private fun createBoxScoreResponse() : BoxScoreResponse {
		return BoxScoreResponse(BoxScoreValues(
				BoxScoreTeam(listOf(), 0, 5, 5, 5, 5, 3, 3, 3, 3),
				BoxScoreTeam(listOf(), 0, 8, 8, 8, 8, 2, 2, 2, 2),
				4
		))
	}
}