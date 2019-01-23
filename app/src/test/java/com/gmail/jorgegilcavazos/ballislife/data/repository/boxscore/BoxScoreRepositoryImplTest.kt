package com.gmail.jorgegilcavazos.ballislife.data.repository.boxscore

import com.gmail.jorgegilcavazos.ballislife.data.firebase.remoteconfig.RemoteConfig
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaGamesService
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaService
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreResponse
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreTeam
import com.gmail.jorgegilcavazos.ballislife.features.model.BoxScoreValues
import com.gmail.jorgegilcavazos.ballislife.util.Constants
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BoxScoreRepositoryImplTest {

	@Mock private lateinit var mockNbaGamesService: NbaGamesService
	@Mock private lateinit var mockNbaService: NbaService
  @Mock private lateinit var mockRemoteConfig: RemoteConfig

	private lateinit var repository: BoxScoreRepositoryImpl

	@Before
	fun setup() {
		MockitoAnnotations.initMocks(this)

    `when`(mockRemoteConfig.getBoolean(Constants.USE_SWISH_BACKEND_BOX_SCORE)).thenReturn(true)

		repository = BoxScoreRepositoryImpl(mockNbaGamesService, mockNbaService,
        TrampolineSchedulerProvider(), mockRemoteConfig)
	}

	@Test
	fun loadBoxScoreCacheAvailableDontForceNetwork() {
		`when`(mockNbaGamesService.boxScore("GAME_ID"))
				.thenReturn(Single.error(Exception()))
		val boxScore = createBoxScoreResponse()
		repository.saveBoxScoreInCache("GAME_ID", boxScore)

		val testObserver = repository.boxScore("GAME_ID", false).test()

		testObserver.assertValueCount(2)
		testObserver.assertValueAt(0, { it.inProgress })
		testObserver.assertValueAt(1, { it.success && it.boxScore == boxScore })
	}

	@Test
	fun loadBoxScoreCacheNotAvailableDontForceNetwork() {
		val boxScore = createBoxScoreResponse()
		`when`(mockNbaGamesService.boxScore("GAME_ID")).thenReturn(Single.just(boxScore))

		val testObserver = repository.boxScore("GAME_ID", false).test()

		testObserver.assertValueCount(3)
		testObserver.assertValueAt(0, { it.inProgress })
		testObserver.assertValueAt(1, { it.inProgress })
		testObserver.assertValueAt(2, { it.success && it.boxScore == boxScore })
	}

	@Test
	fun loadBoxScoreForceNetworkSuccess() {
		val boxScore = createBoxScoreResponse()
		`when`(mockNbaGamesService.boxScore("GAME_ID")).thenReturn(Single.just(boxScore))

		val testObserver = repository.boxScore("GAME_ID", true).test()

		testObserver.assertValueCount(2)
		testObserver.assertValueAt(0, { it.inProgress })
		testObserver.assertValueAt(1, { it.success && it.boxScore == boxScore })
	}

	@Test
	fun loadBoxScoreForceNetworkNotAvailable() {
		`when`(mockNbaGamesService.boxScore("GAME_ID")).thenReturn(Single.error(Exception()))

		val testObserver = repository.boxScore("GAME_ID", true).test()

		testObserver.assertValueCount(2)
		testObserver.assertValueAt(0, { it.inProgress })
		testObserver.assertValueAt(1, { it.notAvailable })
	}

	private fun createBoxScoreResponse() : BoxScoreResponse {
		return BoxScoreResponse(BoxScoreValues(
				BoxScoreTeam(listOf(), 0, 5, 5, 5, 5, 3, 3, 3, 3),
				BoxScoreTeam(listOf(), 0, 8, 8, 8, 8, 2, 2, 2, 2),
				4
		))
	}
}