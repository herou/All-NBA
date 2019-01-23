package com.gmail.jorgegilcavazos.ballislife.features.gamethread

import com.gmail.jorgegilcavazos.ballislife.data.actions.RedditActions
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.ReplyUIModel
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.SaveUIModel
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.VoteUIModel
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.gamethreads.GameThreadsRepository
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentDelay
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper
import com.gmail.jorgegilcavazos.ballislife.features.model.GameThreadType
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.TrampolineSchedulerProvider
import com.google.common.collect.FluentIterable
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.Calendar

class GameThreadPresenterV2Test {

  companion object {
    val THREAD_TYPE = GameThreadType.LIVE
    val HOME = "SAS"
    val VISITOR = "MIL"
    val GAME_TIME_UTC = 10000L
    val SUBMISSION_ID = "82fh"
    val PARENT_FULLNAME = "9n1uid3"
    val RESPONSE = "This is a reply!"

    private val commentSaves: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val commentUnsaves: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val upvotes: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val downvotes: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val novotes: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val replies: PublishSubject<CommentWrapper> = PublishSubject.create()
    private val submissionReplies: PublishSubject<Any> = PublishSubject.create()
    private val streamChanges: PublishSubject<Boolean> = PublishSubject.create()
    private val commentCollapses = PublishSubject.create<String>()
    private val commentUncollapses = PublishSubject.create<String>()
  }

  @Mock private lateinit var mockView: GameThreadView
  @Mock private lateinit var mockGameThreadsRepository: GameThreadsRepository
  @Mock private lateinit var mockRedditActions: RedditActions
  @Mock private lateinit var mockContributionsRepository: ContributionRepository
  @Mock private lateinit var threadsDisposable: CompositeDisposable
  @Mock private lateinit var localRepository: LocalRepository
  @Mock private lateinit var disposable: CompositeDisposable
  @Mock private lateinit var mockNetworkUtils: NetworkUtils
  @Mock private lateinit var mockErrorHandler: ErrorHandler
  private val streamChangesBus: StreamChangesBus = mock()

  private lateinit var presenter: GameThreadPresenterV2

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    `when`(mockView.getThreadType()).thenReturn(THREAD_TYPE)
    `when`(mockView.getHome()).thenReturn(HOME)
    `when`(mockView.getVisitor()).thenReturn(VISITOR)
    `when`(mockView.getGameTimeUtc()).thenReturn(GAME_TIME_UTC)
    `when`(mockView.commentSaves()).thenReturn(commentSaves)
    `when`(mockView.commentUnsaves()).thenReturn(commentUnsaves)
    `when`(mockView.upvotes()).thenReturn(upvotes)
    `when`(mockView.downvotes()).thenReturn(downvotes)
    `when`(mockView.novotes()).thenReturn(novotes)
    `when`(mockView.replies()).thenReturn(replies)
    `when`(mockView.submissionReplies()).thenReturn(submissionReplies)
    `when`(mockView.commentCollapses()).thenReturn(commentCollapses)
    `when`(mockView.commentUnCollapses()).thenReturn(commentUncollapses)
    whenever(streamChangesBus.getChanges()).thenReturn(Observable.empty())

    presenter = GameThreadPresenterV2(
        mockGameThreadsRepository,
        mockRedditActions,
        mockContributionsRepository,
        TrampolineSchedulerProvider(),
        threadsDisposable,
        localRepository,
        disposable,
        mockNetworkUtils,
        mockErrorHandler,
        streamChangesBus)
    presenter.attachView(mockView)
  }

  @Test
  fun clearSubscriptionOnDetach() {
    presenter.detachView()

    verify(disposable).clear()
    verify(threadsDisposable).clear()
  }

  @Test
  fun saveCommentSuccess() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.savePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.success()))

    commentSaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).savePublicContribution(mockComment)
    verify(mockView).showSavedToast()
    verify(mockView, times(0)).showNotLoggedInToast()
    verify(mockView, times(0)).showSavingToast()
  }

  @Test
  fun saveCommentNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.savePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    commentSaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).savePublicContribution(mockComment)
    verify(mockView).showNotLoggedInToast()
    verify(mockView, times(0)).showSavingToast()
    verify(mockView, times(0)).showSavedToast()
  }

  @Test
  fun unsaveCommentSuccess() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.unsavePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.success()))

    commentUnsaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).unsavePublicContribution(mockComment)
    verify(mockView).showUnsavedToast()
    verify(mockView, times(0)).showNotLoggedInToast()
    verify(mockView, times(0)).showUnsavingToast()
  }

  @Test
  fun unsaveCommentNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.unsavePublicContribution(mockComment))
        .thenReturn(Observable.just(SaveUIModel.notLoggedIn()))

    commentUnsaves.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).unsavePublicContribution(mockComment)
    verify(mockView).showNotLoggedInToast()
    verify(mockView, times(0)).showUnsavingToast()
    verify(mockView, times(0)).showUnsavedToast()
  }

  @Test
  fun upvote() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.UPVOTE))
        .thenReturn(Observable.just(VoteUIModel.success()))

    upvotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.UPVOTE)
  }

  @Test
  fun downvote() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.DOWNVOTE))
        .thenReturn(Observable.just(VoteUIModel.success()))

    downvotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.DOWNVOTE)
  }

  @Test
  fun novote() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.NO_VOTE))
        .thenReturn(Observable.just(VoteUIModel.success()))

    novotes.onNext(CommentWrapper(mockComment))

    verify(mockRedditActions).voteComment(mockComment, VoteDirection.NO_VOTE)
  }

  @Test
  fun doNotUpvoteIfNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.UPVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    upvotes.onNext(CommentWrapper(mockComment))

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun doNotDownvoteIfNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.DOWNVOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    downvotes.onNext(CommentWrapper(mockComment))

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun doNotNovoteIfNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(mockRedditActions.voteComment(mockComment, VoteDirection.NO_VOTE))
        .thenReturn(Observable.just(VoteUIModel.notLoggedIn()))

    novotes.onNext(CommentWrapper(mockComment))

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun openReplyActivityOnCommentReply() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(localRepository.username).thenReturn("username")

    replies.onNext(CommentWrapper(mockComment))

    verify(mockContributionsRepository).saveComment(mockComment)
    verify(mockView).openReplyToCommentActivity(mockComment)
  }

  @Test
  fun dontOpenReplyActivityOnCommentReplyIfNotLoggedIn() {
    val mockComment = Mockito.mock(Comment::class.java)
    `when`(localRepository.username).thenReturn(null)

    replies.onNext(CommentWrapper(mockComment))

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun openReplyActivityOnSubmissionReply() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.id).thenReturn(SUBMISSION_ID)
    `when`(localRepository.username).thenReturn("username")

    presenter.setCurrentSubmission(mockSubmission)
    submissionReplies.onNext(Any())

    verify(mockContributionsRepository).saveSubmission(mockSubmission)
    verify(mockView).openReplyToSubmissionActivity(SUBMISSION_ID)
  }

  @Test
  fun dontOpenReplyActivityOnSubmissionReply() {
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.id).thenReturn(SUBMISSION_ID)
    `when`(localRepository.username).thenReturn("")

    presenter.setCurrentSubmission(mockSubmission)
    submissionReplies.onNext(Any())

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun loadGameThreadInProgress() {
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.just(GameThreadsUIModel.inProgress()))

    presenter.loadGameThread(repeating = false)

    verify(mockGameThreadsRepository).gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE)
    verify(mockView).setLoadingIndicator(true)
    verify(mockView).hideErrorLoadingText()
    verify(mockView).hideNoCommentsText()
    verify(mockView).hideFab()
    verify(mockView).hideNoThreadText()
  }

  @Test
  fun loadGameThreadNotFound() {
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.just(GameThreadsUIModel.notFound()))

    presenter.loadGameThread(repeating = false)

    verify(mockView).setLoadingIndicator(false)
    verify(mockView).hideErrorLoadingText()
    verify(mockView).hideNoCommentsText()
    verify(mockView).hideFab()
    verify(mockView).showNoThreadText()
  }

  @Test
  fun loadGameThreadFoundWithComments() {
    `when`(mockView.getCommentDelay()).thenReturn(CommentDelay.NONE)
    val mockCommentNode1 = Mockito.mock(CommentNode::class.java)
    setupMocksForNode(mockCommentNode1)
    `when`(mockCommentNode1.depth).thenReturn(0)
    val mockCommentNode2 = Mockito.mock(CommentNode::class.java)
    setupMocksForNode(mockCommentNode2)
    `when`(mockCommentNode2.depth).thenReturn(1)
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.comments).thenReturn(mockCommentNode1)
    `when`(mockCommentNode1.walkTree())
        .thenReturn(FluentIterable.of(mockCommentNode1, mockCommentNode2))
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.just(GameThreadsUIModel.found(mockSubmission)))

    presenter.loadGameThread(repeating = false)

    verify(mockView).setLoadingIndicator(false)
    verify(mockView).hideErrorLoadingText()
    verify(mockView).hideNoCommentsText()
    verify(mockView).showFab()
    verify(mockView).showComments(ArgumentMatchers.anyList())
    verify(mockView).hideNoThreadText()
  }

  @Test
  fun loadGameThreadFoundWithNoComments() {
    val mockCommentNode1 = Mockito.mock(CommentNode::class.java)
    val mockSubmission = Mockito.mock(Submission::class.java)
    `when`(mockSubmission.comments).thenReturn(mockCommentNode1)
    `when`(mockCommentNode1.walkTree()).thenReturn(FluentIterable.of())
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.just(GameThreadsUIModel.found(mockSubmission)))

    presenter.loadGameThread(repeating = false)

    verify(mockView).setLoadingIndicator(false)
    verify(mockView).hideErrorLoadingText()
    verify(mockView).hideNoCommentsText()
    verify(mockView).showFab()
    verify(mockView).showNoCommentsText()
    verify(mockView).hideComments()
    verify(mockView).hideNoThreadText()
  }

  @Test
  fun loadGameThreadErrorNetAvailable() {
    val e = Exception()
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.error(e))
    `when`(mockNetworkUtils.isNetworkAvailable()).thenReturn(true)
    `when`(mockErrorHandler.handleError(e)).thenReturn(404)

    presenter.loadGameThread(repeating = false)

    verify(mockView).setLoadingIndicator(false)
    verify(mockView).showErrorLoadingText(404)
  }

  @Test
  fun loadGameThreadErrorNoNetAvailable() {
    val e = Exception()
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.error(e))
    `when`(mockNetworkUtils.isNetworkAvailable()).thenReturn(false)

    presenter.loadGameThread(repeating = false)

    verify(mockView).setLoadingIndicator(false)
    verify(mockView).showNoNetAvailableText()
  }

  @Test
  fun loadGameThreadInProgressWhileStreaming() {
    `when`(mockGameThreadsRepository.gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE))
        .thenReturn(Observable.just(GameThreadsUIModel.inProgress()))

    presenter.loadGameThread(repeating = true)

    verify(mockGameThreadsRepository).gameThreads(HOME, VISITOR, GAME_TIME_UTC, THREAD_TYPE)
    verify(mockView).setLoadingIndicator(false)
    verify(mockView).hideErrorLoadingText()
    verify(mockView).hideNoCommentsText()
    verify(mockView, times(0)).hideFab()
    verify(mockView).hideNoThreadText()
  }

  @Test
  fun replyToCommentInProgress() {
    `when`(mockRedditActions.replyToComment(PARENT_FULLNAME, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.inProgress()))

    presenter.replyToComment(PARENT_FULLNAME, RESPONSE)

    verify(mockRedditActions).replyToComment(PARENT_FULLNAME, RESPONSE)
    verify(mockView).showSubmittingCommentToast()
  }

  @Test
  fun replyToCommentParentNotFound() {
    `when`(mockRedditActions.replyToComment(PARENT_FULLNAME, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.parentNotFound()))

    presenter.replyToComment(PARENT_FULLNAME, RESPONSE)

    verify(mockView).showMissingParentToast()
  }

  @Test
  fun replyToCommentNotLoggedIn() {
    `when`(mockRedditActions.replyToComment(PARENT_FULLNAME, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.notLoggedIn()))

    presenter.replyToComment(PARENT_FULLNAME, RESPONSE)

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun replyToCommentSuccess() {
    `when`(mockRedditActions.replyToComment(PARENT_FULLNAME, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.success()))

    presenter.replyToComment(PARENT_FULLNAME, RESPONSE)

    verify(mockView).showSubmittedCommentToast()
  }

  @Test
  fun replyToSubmissionInProgress() {
    `when`(mockRedditActions.replyToSubmission(SUBMISSION_ID, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.inProgress()))

    presenter.replyToSubmission(SUBMISSION_ID, RESPONSE)

    verify(mockRedditActions).replyToSubmission(SUBMISSION_ID, RESPONSE)
    verify(mockView).showSubmittingCommentToast()
  }

  @Test
  fun replyToSubmissionParentNotFound() {
    `when`(mockRedditActions.replyToSubmission(SUBMISSION_ID, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.parentNotFound()))

    presenter.replyToSubmission(SUBMISSION_ID, RESPONSE)

    verify(mockView).showMissingSubmissionToast()
  }

  @Test
  fun replyToSubmissionNotLoggedIn() {
    `when`(mockRedditActions.replyToSubmission(SUBMISSION_ID, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.notLoggedIn()))

    presenter.replyToSubmission(SUBMISSION_ID, RESPONSE)

    verify(mockView).showNotLoggedInToast()
  }

  @Test
  fun replyToSubmissionSuccess() {
    `when`(mockRedditActions.replyToSubmission(SUBMISSION_ID, RESPONSE))
        .thenReturn(Observable.just(ReplyUIModel.success()))

    presenter.replyToSubmission(SUBMISSION_ID, RESPONSE)

    verify(mockView).showSubmittedCommentToast()
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

  private fun setupMocksForNode(commentNode: CommentNode) {
    val mockComment1 = Mockito.mock(Comment::class.java)
    `when`(mockComment1.id).thenReturn("id")
    `when`(mockComment1.isSaved).thenReturn(false)
    `when`(mockComment1.author).thenReturn("")
    `when`(mockComment1.score).thenReturn(1)
    `when`(mockComment1.created).thenReturn(Calendar.getInstance().time)
    `when`(mockComment1.body).thenReturn("")
    `when`(mockComment1.data("body_html")).thenReturn("")
    `when`(mockComment1.authorFlair).thenReturn(null)
    `when`(mockComment1.vote).thenReturn(VoteDirection.UPVOTE)
    `when`(mockComment1.hasBeenEdited()).thenReturn(false)
    `when`(commentNode.comment).thenReturn(mockComment1)
  }
}