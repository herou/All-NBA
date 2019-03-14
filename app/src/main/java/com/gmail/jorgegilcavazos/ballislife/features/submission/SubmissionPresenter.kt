package com.gmail.jorgegilcavazos.ballislife.features.submission

import android.support.annotation.VisibleForTesting
import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter
import com.gmail.jorgegilcavazos.ballislife.data.actions.RedditActions
import com.gmail.jorgegilcavazos.ballislife.data.actions.models.SaveUIModel
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication
import com.gmail.jorgegilcavazos.ballislife.data.repository.comments.ContributionRepository
import com.gmail.jorgegilcavazos.ballislife.data.repository.submissions.SubmissionRepository
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditService
import com.gmail.jorgegilcavazos.ballislife.util.*
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import net.dean.jraw.models.*
import javax.inject.Inject

class SubmissionPresenter @Inject constructor(
		private val redditAuthentication: RedditAuthentication,
		private val submissionRepository: SubmissionRepository,
		private val schedulerProvider: BaseSchedulerProvider,
		private val disposables: CompositeDisposable,
		private val redditService: RedditService,
		private val redditActions: RedditActions,
		private val contributionRepository: ContributionRepository,
		private val networkUtils: NetworkUtils,
		private val errorHandler: ErrorHandler) : BasePresenter<SubmissionView>() {

	private var currentSubmission: Submission? = null

	override fun attachView(view: SubmissionView) {
		super.attachView(view)

		view.commentSaves()
				.subscribe {
					// TODO: allow operations on null comments (own replies).
					// Empty comment in wrapper means we don't have the actual [Comment], usually because this
					// comment was created after a reply (where we don't have the [Comment] object).
					if (it.comment != null) {
						saveComment(it.comment)
					}
				}.addTo(disposables)

		view.commentUnsaves()
				.subscribe {
					if (it.comment != null) {
						unsaveComment(it.comment)
					}
				}
				.addTo(disposables)

		view.commentUpvotes()
				.subscribe {
					if (it.comment != null) {
						redditActions.voteComment(it.comment, VoteDirection.UPVOTE)
								.subscribe {
									if (it.notLoggedIn) {
										view.showNotLoggedInError()
									}
								}.addTo(disposables)
					}
				}.addTo(disposables)

		view.commentDownvotes()
				.subscribe {
					if (it.comment != null) {
						redditActions.voteComment(it.comment, VoteDirection.DOWNVOTE)
								.subscribe {
									if (it.notLoggedIn) {
										view.showNotLoggedInError()
									}
								}.addTo(disposables)
					}
				}.addTo(disposables)

		view.commentNovotes()
				.subscribe {
					if (it.comment != null) {
						redditActions.voteComment(it.comment, VoteDirection.NO_VOTE)
								.subscribe {
									if (it.notLoggedIn) {
										view.showNotLoggedInError()
									}
								}.addTo(disposables)
					}
				}.addTo(disposables)

        view.submissionShares().subscribe {
            if (it.isSelfPost) {
                view.share(Constants.HTTPS + Constants.REDDIT_DOMAIN +
                        it.permalink)
            } else {
                view.share(it.url)
            }
        }.addTo(disposables)

		view.submissionSaves()
				.subscribe {
					redditActions.savePublicContribution(it)
							.subscribe {
								if (it.notLoggedIn) {
									view.showNotLoggedInError()
								}
							}
				}
				.addTo(disposables)

		view.submissionUnsaves()
				.subscribe {
					redditActions.unsavePublicContribution(it)
							.subscribe {
								if (it.notLoggedIn) {
									view.showNotLoggedInError()
								}
							}.addTo(disposables)
				}.addTo(disposables)

		view.submissionUpvotes()
				.subscribe {
					redditActions.voteSubmission(it, VoteDirection.UPVOTE)
							.subscribe {
								if (it.notLoggedIn) {
									view.showNotLoggedInError()
								}
							}.addTo(disposables)
				}.addTo(disposables)

		view.submissionDownvotes()
				.subscribe {
					redditActions.voteSubmission(it, VoteDirection.DOWNVOTE)
							.subscribe {
								if (it.notLoggedIn) {
									view.showNotLoggedInError()
								}
							}.addTo(disposables)
				}.addTo(disposables)

		view.submissionNovotes()
				.subscribe {
					redditActions.voteSubmission(it, VoteDirection.NO_VOTE)
							.subscribe {
								if (it.notLoggedIn) {
									view.showNotLoggedInError()
								}
							}.addTo(disposables)
				}.addTo(disposables)

		view.commentReplies()
				.subscribe {
					if (redditAuthentication.isUserLoggedIn) {
						if (it.comment != null) {
							contributionRepository.saveComment(it.comment)
							view.openReplyToCommentActivity(it.comment)
						}
					} else {
						view.showNotLoggedInError()
					}
				}.addTo(disposables)

		view.submissionReplies()
				.subscribe {
					if (redditAuthentication.isUserLoggedIn) {
						contributionRepository.saveSubmission(
								currentSubmission
										?: throw IllegalStateException("Current submission should not be null"))
						view.openReplyToSubmissionActivity(
								currentSubmission?.id
										?: throw IllegalStateException("Current submission should not be null"))
					} else {
						view.showNotLoggedInError()
					}
				}.addTo(disposables)

		view.submissionContentClicks()
				.subscribe { onContentClick(it) }
				.addTo(disposables)

		view.commentCollapses()
				.subscribe { view.collapseComments(it) }
				.addTo(disposables)

		view.commentUnCollapses()
				.subscribe { view.uncollapseComments(it) }
				.addTo(disposables)

		view.loadMoreComments()
				.subscribe {
					it.commentNode?.let {
						loadChildren(it)
					}
				}
	}

	override fun detachView() {
		disposables.clear()
		super.detachView()
	}

	fun loadComments(
			threadId: String,
			sorting: CommentSort,
			forceReload: Boolean) {
		view.hideFab()
		view.setLoadingIndicator(true)

		redditAuthentication.authenticate()
				.andThen(
						submissionRepository.getSubmission(
								threadId,
								sorting,
								forceReload))
				.subscribeOn(schedulerProvider.io())
				.observeOn(schedulerProvider.ui())
				.subscribe(
						{ submissionWrapper ->
							currentSubmission = submissionWrapper.submission!!
							val items = CommentsTraverser
									.flattenCommentTree(submissionWrapper.submission.comments.children)

							view.showComments(items, submissionWrapper.submission)
							view.setLoadingIndicator(false)
							view.showFab()
						},
						{ e ->
							if (!networkUtils.isNetworkAvailable()) {
								view.showNoNetAvailable()
							} else {
								errorHandler.handleError(e)
							}
							view.setLoadingIndicator(false)
						}
				)
				.addTo(disposables)
	}

	fun replyToComment(parentId: String, response: String) {
		redditActions.replyToComment(parentId, response)
				.subscribe(
						{ uiModel ->
							if (uiModel.inProgress) {
								view.showSubmittingCommentToast()
							}
							if (uiModel.success) {
								val parentComment = contributionRepository.getComment(parentId)
								if (uiModel.commentItem != null && parentComment != null) {
									view.addCommentItem(uiModel.commentItem, parentId)
								}
							}
							if (uiModel.error != null) {
								if (!networkUtils.isNetworkAvailable()) {
									view.showNoNetAvailable()
								} else {
									errorHandler.handleError(uiModel.error)
									view.showErrorAddingComment()
								}
							}
						}).addTo(disposables)
	}

	fun replyToSubmission(submissionId: String, response: String) {
		redditActions.replyToSubmission(submissionId, response)
				.subscribe(
						{ uiModel ->
							if (uiModel.inProgress) {
								view.showSubmittingCommentToast()
							}
							if (uiModel.success) {
								if (uiModel.commentItem != null) {
									view.addCommentItem(uiModel.commentItem)
								}
							}
							if (uiModel.error != null) {
								if (!networkUtils.isNetworkAvailable()) {
									view.showNoNetAvailable()
								} else {
									errorHandler.handleError(uiModel.error)
									view.showErrorAddingComment()
								}
							}
						}).addTo(disposables)
	}

	private fun saveComment(comment: Comment) {
		redditActions.savePublicContribution(comment)
				.subscribe(
						{ uiModel: SaveUIModel ->
							if (uiModel.notLoggedIn) {
								view.showNotLoggedInError()
							}

							if (uiModel.success) {
								view.showSavedCommentToast()
							}

							if (uiModel.error != null) {
								if (!networkUtils.isNetworkAvailable()) {
									view.showNoNetAvailable()
								} else {
									errorHandler.handleError(uiModel.error)
								}
							}
						}
				)
				.addTo(disposables)
	}

	private fun unsaveComment(comment: Comment) {
		redditActions.unsavePublicContribution(comment)
				.subscribe(
						{
							if (it.notLoggedIn) {
								view.showNotLoggedInError()
							}

							if (it.success) {
								view.showUnsavedCommentToast()
							}

							if (it.error != null) {
								if (!networkUtils.isNetworkAvailable()) {
									view.showNoNetAvailable()
								} else {
									errorHandler.handleError(it.error)
								}
							}
						}
				)
				.addTo(disposables)
	}

	private fun onContentClick(url: String?) {
		if (url != null) {
			if (url.contains(Constants.STREAMABLE_DOMAIN)) {
				val shortCode = Utilities.getStreamableShortcodeFromUrl(url)
				if (shortCode != null) {
					view.openStreamable(shortCode)
				} else {
					view.openContentTab(url)
				}
			} else {
				view.openContentTab(url)
			}
		} else {
			view.showContentUnavailableToast()
		}
	}

	private fun loadChildren(commentNode: CommentNode) {
		redditService.loadMoreComments(redditAuthentication.redditClient, commentNode)
				.subscribeOn(schedulerProvider.io())
				.observeOn(schedulerProvider.ui())
				.subscribe(
						{ nodes ->
							val items = CommentsTraverser.flattenCommentTree(nodes)
							view.insertItemsBelowParent(items, commentNode)
						},
						{
							view.showErrorLoadingMoreComments()
						}
				)
				.addTo(disposables)
	}

	@VisibleForTesting
	fun setCurrentSubmission(submission: Submission) {
		currentSubmission = submission
	}

}
