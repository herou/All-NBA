package com.gmail.jorgegilcavazos.ballislife.features.gamethread

import android.support.annotation.VisibleForTesting
import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter
import com.gmail.jorgegilcavazos.ballislife.data.actions.RedditActions
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.ReplyUIModel
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.SaveUIModel
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.gamethreads.GameThreadsRepository
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.StreamChangesBus.StreamMode.OFF
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.StreamChangesBus.StreamMode.ON
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentDelay.NONE
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentItem
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper
import com.gmail.jorgegilcavazos.ballislife.features.model.GameThreadType
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItemType.COMMENT
import com.gmail.jorgegilcavazos.ballislife.util.ErrorHandler
import com.gmail.jorgegilcavazos.ballislife.util.NetworkUtils
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GameThreadPresenterV2 @Inject constructor(
    private val gameThreadsRepository: GameThreadsRepository,
    private val redditActions: RedditActions,
    private val contributionRepository: ContributionRepository,
    private val schedulerProvider: BaseSchedulerProvider,
    private val threadsDisposable: CompositeDisposable,
    private val localRepository: LocalRepository,
    private val disposable: CompositeDisposable,
    private val networkUtils: NetworkUtils,
    private val errorHandler: ErrorHandler,
    private val streamChangesBus: StreamChangesBus
) : BasePresenter<GameThreadView>() {

  private lateinit var type: GameThreadType
  private lateinit var home: String
  private lateinit var visitor: String
  private var gameTimeUtc: Long = 0
  private var currentSubmission: Submission? = null

  override fun attachView(view: GameThreadView) {
    super.attachView(view)
    type = view.getThreadType()
    home = view.getHome()
    visitor = view.getVisitor()
    gameTimeUtc = view.getGameTimeUtc()

    view.commentSaves()
        .subscribe { saveComment(it.comment!!) }
        .addTo(disposable)

    view.commentUnsaves()
        .subscribe { unsaveComment(it.comment!!) }
        .addTo(disposable)

    view.upvotes()
        .subscribe {
          redditActions.voteComment(it.comment!!, VoteDirection.UPVOTE)
              .subscribe({
                if (it.notLoggedIn) {
                  view.showNotLoggedInToast()
                }
              })
              .addTo(disposable)
        }.addTo(disposable)

    view.downvotes()
        .subscribe {
          redditActions.voteComment(it.comment!!, VoteDirection.DOWNVOTE)
              .subscribe({
                if (it.notLoggedIn) {
                  view.showNotLoggedInToast()
                }
              })
              .addTo(disposable)
        }.addTo(disposable)

    view.novotes()
        .subscribe { redditActions.voteComment(it.comment!!, VoteDirection.NO_VOTE)
            .subscribe({
              if (it.notLoggedIn) {
                view.showNotLoggedInToast()
              }
            })
            .addTo(disposable)
        }.addTo(disposable)

    view.replies()
        .subscribe {
          if (!localRepository.username.isNullOrEmpty()) {
            contributionRepository.saveComment(it.comment!!)
            view.openReplyToCommentActivity(it.comment)
          } else {
            view.showNotLoggedInToast()
          }
        }.addTo(disposable)

    view.submissionReplies()
        .subscribe {
          if (!localRepository.username.isNullOrEmpty()) {
            contributionRepository.saveSubmission(
                currentSubmission
                    ?: throw IllegalStateException("Current submission should not be null"))
            view.openReplyToSubmissionActivity(
                currentSubmission?.id
                    ?: throw IllegalStateException("Current submission should not be null"))
          } else {
            view.showNotLoggedInToast()
          }
        }.addTo(disposable)

    streamChangesBus.getChanges()
        .subscribe { streamMode ->
          when (streamMode) {
            ON -> loadGameThread(repeating = true)
            OFF -> {
              loadGameThread(repeating = false)
              view.setCommentDelay(NONE)
            }
          }
        }.addTo(disposable)

    view.commentCollapses()
        .subscribe { view.collapseComments(it) }
        .addTo(disposable)

    view.commentUnCollapses()
        .subscribe { view.uncollapseComments(it) }
        .addTo(disposable)
  }

  override fun detachView() {
    disposable.clear()
    threadsDisposable.clear()
    super.detachView()
  }

  fun onVisible() {
    if (null != currentSubmission && view != null) {
      view.showFab()
    }
  }

  fun loadGameThread(repeating: Boolean) {
    val gameThreadsObs = if (repeating) {
      gameThreadsRepository.gameThreads(home, visitor, gameTimeUtc, type)
          .repeatWhen { obs -> obs.delay(10, TimeUnit.SECONDS) }
    } else {
      gameThreadsRepository.gameThreads(home, visitor, gameTimeUtc, type)
    }

    threadsDisposable.clear()
    gameThreadsObs
        .observeOn(schedulerProvider.ui(), true)
        .subscribe(
            { uiModel ->
              if (uiModel.inProgress && !localRepository.isGameThreadStreamingEnabled) {
                view.setLoadingIndicator(true)
                view.hideFab()
              } else {
                view.setLoadingIndicator(false)
              }

              view.hideErrorLoadingText()
              view.hideNoCommentsText()

              if (uiModel.found) {
                val submission = uiModel.submission!!
                currentSubmission = submission
                val iterator = submission.comments?.walkTree()

                val threadItems = mutableListOf<ThreadItem>()

                val delay = view.getCommentDelay()

                iterator?.filter {
                  // Filter out comments that are older than now minus delay.
                  System.currentTimeMillis() - delay.seconds * 1000 > it!!.comment.created.time
                }?.forEach {
                  threadItems.add(
                      ThreadItem(
                          COMMENT,
                          createCommentItem(it),
                          it.depth))
                }

                if (threadItems.isEmpty()) {
                  view.showNoCommentsText()
                  view.hideComments()
                } else {
                  view.showComments(threadItems)
                }
                view.showFab()
              }

              if (uiModel.notFound) {
                view.hideFab()
              }

              if (uiModel.notFound) {
                view.showNoThreadText()
              } else {
                view.hideNoThreadText()
              }
            },
            { e ->
              if (!networkUtils.isNetworkAvailable()) {
                view.showNoNetAvailableText()
              } else {
                view.showErrorLoadingText(errorHandler.handleError(e))
              }
              view.setLoadingIndicator(false)
            }
        )
        .addTo(threadsDisposable)
  }

  fun replyToComment(parentId: String, response: String) {
    redditActions.replyToComment(parentId, response)
        .subscribe(
            { uiModel: ReplyUIModel ->
              if (uiModel.inProgress) {
                view.showSubmittingCommentToast()
              }

              if (uiModel.notLoggedIn) {
                view.showNotLoggedInToast()
              }

              if (uiModel.parentNotFound) {
                view.showMissingParentToast()
              }

              if (uiModel.success) {
                view.showSubmittedCommentToast()
              }

              if (uiModel.error != null) {
                if (!networkUtils.isNetworkAvailable()) {
                  view.showNoNetAvailableToast()
                } else {
                  view.showErrorSavingCommentToast(errorHandler.handleError(uiModel.error))
                }
              }
            }).addTo(disposable)
  }

  fun replyToSubmission(submissionId: String, response: String) {
    redditActions.replyToSubmission(submissionId, response)
        .subscribe(
            { uiModel ->
              if (uiModel.inProgress) {
                view.showSubmittingCommentToast()
              }

              if (uiModel.notLoggedIn) {
                view.showNotLoggedInToast()
              }

              if (uiModel.parentNotFound) {
                view.showMissingSubmissionToast()
              }

              if (uiModel.success) {
                view.showSubmittedCommentToast()
              }

              if (uiModel.error != null) {
                if (!networkUtils.isNetworkAvailable()) {
                  view.showNoNetAvailableToast()
                } else {
                  view.showErrorSavingCommentToast(errorHandler.handleError(uiModel.error))
                }
              }
            }).addTo(disposable)
  }

  private fun saveComment(comment: Comment) {
    redditActions.savePublicContribution(comment)
        .subscribe(
            { uiModel: SaveUIModel ->
              if (uiModel.notLoggedIn) {
                view.showNotLoggedInToast()
              }

              if (uiModel.success) {
                view.showSavedToast()
              }

              if (uiModel.error != null) {
                if (!networkUtils.isNetworkAvailable()) {
                  view.showNoNetAvailableToast()
                } else {
                  errorHandler.handleError(uiModel.error)
                }
              }
            }
        )
        .addTo(disposable)
  }

  private fun unsaveComment(comment: Comment) {
    redditActions.unsavePublicContribution(comment)
        .subscribe(
            {
              if (it.notLoggedIn) {
                view.showNotLoggedInToast()
              }

              if (it.success) {
                view.showUnsavedToast()
              }

              if (it.error != null) {
                if (!networkUtils.isNetworkAvailable()) {
                  view.showNoNetAvailableToast()
                } else {
                  errorHandler.handleError(it.error)
                }
              }
            }
        )
        .addTo(disposable)
  }

  @VisibleForTesting
  fun setCurrentSubmission(submission: Submission) {
    currentSubmission = submission
  }

  private fun createCommentItem(root: CommentNode): CommentItem {
    val comment = root.comment
    return CommentItem(
        commentWrapper = CommentWrapper(
            comment = comment,
            id = comment.id,
            saved = comment.isSaved,
            author = comment.author,
            score = comment.score,
            created = comment.created,
            body = comment.body,
            bodyHtml = comment.data("body_html"),
            authorFlair = comment.authorFlair,
            vote = comment.vote,
            edited = comment.hasBeenEdited()),
        depth = root.depth)
  }
}