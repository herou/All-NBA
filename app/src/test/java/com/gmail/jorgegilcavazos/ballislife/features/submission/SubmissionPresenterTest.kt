package com.gmail.jorgegilcavazos.ballislife.features.submission

import com.gmail.jorgegilcavazos.ballislife.data.actions.RedditActions
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.SaveUIModel
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.VoteUIModel
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.submissions.SubmissionRepository
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditService
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentItem
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper
import com.gmail.jorgegilcavazos.ballislife.util.Constants
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
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
class SubmissionPresenterTest {

  @Mock private lateinit var mockView: SubmissionView
  @Mock private lateinit var mockRedditAuthentication: RedditAuthentication
  @Mock private lateinit var mockSubmissionRepository: SubmissionRepository
  @Mock private lateinit var mockCompositeDisposable: CompositeDisposable
  @Mock private lateinit var mockRedditService: RedditService
  @Mock private lateinit var mockRedditActions: RedditActions
  @Mock private lateinit var mockContributionRepository: ContributionRepository
  @Mock private lateinit var mockNetworkUtils: NetworkUtils
  @Mock private lateinit var mockErrorHandler: ErrorHandler

  private val commentSaves = PublishSubject.create<CommentWrapper>()
  private val commentUnsaves = PublishSubject.create<CommentWrapper>()
  private val commentUpvotes = PublishSubject.create<CommentWrapper>()
  private val commentDownvotes = PublishSubject.create<CommentWrapper>()
  private val commentNovotes = PublishSubject.create<CommentWrapper>()
  private val submissionShares = PublishSubject.create<Submission>()
  private val submissionSaves = PublishSubject.create<Submission>()
  private val submissionUnsaves = PublishSubject.create<Submission>()
  private val submissionUpvotes = PublishSubject.create<Submission>()
  private val submissionDownvotes = PublishSubject.create<Submission>()
  private val submissionNovotes = PublishSubject.create<Submission>()
  private val commentReplies = PublishSubject.create<CommentWrapper>()
  private val submissionReplies = PublishSubject.create<Any>()
  private val submissionContentClicks = PublishSubject.create<String>()
  private val commentCollapses = PublishSubject.create<String>()
  private val commentUncollapses = PublishSubject.create<String>()
  private val loadMoreComments = PublishSubject.create<CommentItem>()

  private lateinit var presenter: SubmissionPresenter

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)

    `when`(mockView.commentSaves()).thenReturn(commentSaves)
    `when`(mockView.commentUnsaves()).thenReturn(commentUnsaves)
    `when`(mockView.commentUpvotes()).thenReturn(commentUpvotes)
    `when`(mockView.commentDownvotes()).thenReturn(commentDownvotes)
    `when`(mockView.commentNovotes()).thenReturn(commentNovotes)
    `when`(mockView.submissionShares()).thenReturn(submissionShares)
    `when`(mockView.submissionSaves()).thenReturn(submissionSaves)
    `when`(mockView.submissionUnsaves()).thenReturn(submissionUnsaves)
    `when`(mockView.submissionUpvotes()).thenReturn(submissionUpvotes)
    `when`(mockView.submissionDownvotes()).thenReturn(submissionDownvotes)
    `when`(mockView.submissionNovotes()).thenReturn(submissionNovotes)
    `when`(mockView.commentReplies()).thenReturn(commentReplies)
    `when`(mockView.submissionReplies()).thenReturn(submissionReplies)
    `when`(mockView.submissionContentClicks()).thenReturn(submissionContentClicks)
    `when`(mockView.commentCollapses()).thenReturn(commentCollapses)
    `when`(mockView.commentUnCollapses()).thenReturn(commentUncollapses)
    `when`(mockView.loadMoreComments()).thenReturn(loadMoreComments)

    presenter = SubmissionPresenter(
        mockRedditAuthentication,
        mockSubmissionRepository,
        TrampolineSchedulerProvider(),
        mockCompositeDisposable,
        mockRedditService,
        mockRedditActions,
        mockContributionRepository,
        mockNetworkUtils,
        mockErrorHandler)

    presenter.attachView(mockView)
  }

  @Test
  fun saveCommentNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.savePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    commentSaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).savePublicContribution(mockComment)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun unsaveCommentNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.unsavePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    commentUnsaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).unsavePublicContribution(mockComment)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun commentUpvoteNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.UPVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    commentUpvotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.UPVOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun commentDownvoteNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.DOWNVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    commentDownvotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.DOWNVOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun commentNovoteNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.NO_VOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    commentNovotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.NO_VOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun shareSubmissionShareSelfPost() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.isSelfPost).thenReturn(true)
    `when`(mockSubmission.permalink).thenReturn("permalink")

    submissionShares.onNext(mockSubmission)

    verify(mockView).share(Constants.HTTPS +
            Constants.REDDIT_DOMAIN + "permalink")
  }

  @Test
  fun shareSubmissionShareNotSelfPost() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.isSelfPost).thenReturn(false)
    `when`(mockSubmission.url).thenReturn("SelfPostUrl")

    submissionShares.onNext(mockSubmission)

    verify(mockView).share("SelfPostUrl")
  }

  @Test
  fun saveSubmissionNotLoggedIn() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockRedditActions.savePublicContribution(mockSubmission))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    submissionSaves.onNext(mockSubmission)

    verify(mockRedditActions).savePublicContribution(mockSubmission)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun unsaveSubmissionNotLoggedIn() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockRedditActions.unsavePublicContribution(mockSubmission))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    submissionUnsaves.onNext(mockSubmission)

    verify(mockRedditActions).unsavePublicContribution(mockSubmission)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun submissionUpvoteNotLoggedIn() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockRedditActions.voteSubmission(mockSubmission, VoteDirection.UPVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    submissionUpvotes.onNext(mockSubmission)

    verify(mockRedditActions).voteSubmission(mockSubmission, VoteDirection.UPVOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun submissionDownvoteNotLoggedIn() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockRedditActions.voteSubmission(mockSubmission, VoteDirection.DOWNVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    submissionDownvotes.onNext(mockSubmission)

    verify(mockRedditActions).voteSubmission(mockSubmission, VoteDirection.DOWNVOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun submissionNovoteNotLoggedIn() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockRedditActions.voteSubmission(mockSubmission, VoteDirection.NO_VOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    submissionNovotes.onNext(mockSubmission)

    verify(mockRedditActions).voteSubmission(mockSubmission, VoteDirection.NO_VOTE)
    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun commentReplies() {
    `when`(mockRedditAuthentication.isUserLoggedIn).thenReturn(true)
    val mockComment = Mockito.mock(Comment::class.java)

    commentReplies.onNext(CommentWrapper(mockComment))

    verify(mockContributionRepository).saveComment(mockComment)
    verify(mockView).openReplyToCommentActivity(mockComment)
  }

  @Test
  fun commentRepliesNotLoggedIn() {
    `when`(mockRedditAuthentication.isUserLoggedIn).thenReturn(false)
    val mockComment = Mockito.mock(Comment::class.java)

    commentReplies.onNext(CommentWrapper(mockComment))

    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun submissionReplies() {
    `when`(mockRedditAuthentication.isUserLoggedIn).thenReturn(true)
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.id).thenReturn("ABC")
    presenter.setCurrentSubmission(mockSubmission)

    submissionReplies.onNext(Any())

    verify(mockContributionRepository).saveSubmission(mockSubmission)
    verify(mockView).openReplyToSubmissionActivity("ABC")
  }

  @Test
  fun submissionRepliesNotLoggedIn() {
    `when`(mockRedditAuthentication.isUserLoggedIn).thenReturn(false)
    val mockSubmission = Mockito.mock(Submission::class.java)

    submissionReplies.onNext(mockSubmission)

    verify(mockView).showNotLoggedInError()
  }

  @Test
  fun disposeOnDetach() {
    presenter.detachView()

    verify(mockCompositeDisposable).clear()
  }

  @Test
  fun collapseComments() {
    commentCollapses.onNext("COMMENT_ID")

    verify(mockView).collapseComments("COMMENT_ID")
  }

  @Test
  fun uncollapseComments() {
    commentUncollapses.onNext("COMMENT_ID")

    verify(mockView).uncollapseComments("COMMENT_ID")
  }
}