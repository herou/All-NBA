package com.gmail.jorgegilcavazos.ballislife.features.posts

import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication
import com.gmail.jorgegilcavazos.ballislife.data.repository.posts.PostsRepository
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditService
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PostsPresenterTest {

    @Mock
    private lateinit var mockView: PostsView

    @Mock
    private lateinit var mockRedditAuthentication: RedditAuthentication

    @Mock
    private lateinit var mockPostsRepository: PostsRepository

    @Mock
    private lateinit var mockLocalRepository: LocalRepository

    @Mock
    private lateinit var mockRedditService: RedditService

    private lateinit var presenter: PostsPresenter

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)

        `when`(mockRedditAuthentication.authenticate())
                .thenReturn(Completable.complete())
        `when`(mockRedditAuthentication.checkUserLoggedIn())
                .thenReturn(Single.just(true))

        presenter = PostsPresenter(
                mockRedditAuthentication,
                mockLocalRepository,
                mockPostsRepository,
                mockRedditService,
                TrampolineSchedulerProvider()
        )
        presenter.attachView(mockView)
    }

    @Test
    fun onHideSubmissionNotLoggedIn_ShowNotLoggedInToast() {
        val mockSubmissionWrapper = Mockito.mock(SubmissionWrapper::class.java)
        val index = 0
        val hide = true

        `when`(mockRedditAuthentication.isUserLoggedIn)
                .thenReturn(false)

        presenter.onHide(mockSubmissionWrapper, index, hide)

        verify(mockView).showNotLoggedInToast()
    }

    @Test
    fun onUnHideSubmissionNotLoggedIn_ShowNotLoggedInToast() {
        val mockSubmissionWrapper = Mockito.mock(SubmissionWrapper::class.java)
        val index = 0
        val hide = false

        `when`(mockRedditAuthentication.isUserLoggedIn)
                .thenReturn(false)

        presenter.onHide(mockSubmissionWrapper, index, hide)

        verify(mockView).showNotLoggedInToast()
    }

    @Test
    fun onHideSubmission_HideSubmissionAndShowSnack() {
        val mockSubmissionWrapper = Mockito.mock(SubmissionWrapper::class.java)
        val index = 0
        val hide = true

        `when`(mockRedditAuthentication.isUserLoggedIn)
                .thenReturn(true)
        `when`(mockRedditService.hideSubmission(mockRedditAuthentication.redditClient,
                mockSubmissionWrapper.submission,
                hide))
                .thenReturn(Completable.complete())

        presenter.onHide(mockSubmissionWrapper, index, hide)

        verify(mockView).hideSubmission(mockSubmissionWrapper, index)
        verify(mockView).showHideSubmissionSnackbar(mockSubmissionWrapper, index)
    }

    @Test
    fun onUnHideSubmission_UnHideSubmission() {
        val mockSubmissionWrapper = Mockito.mock(SubmissionWrapper::class.java)
        val index = 0
        val hide = false

        `when`(mockRedditAuthentication.isUserLoggedIn)
                .thenReturn(true)
        `when`(mockRedditService.hideSubmission(mockRedditAuthentication.redditClient,
                mockSubmissionWrapper.submission,
                hide))
                .thenReturn(Completable.complete())

        presenter.onHide(mockSubmissionWrapper, index, hide)

        verify(mockView).unHideSubmission(mockSubmissionWrapper, index)
    }
}