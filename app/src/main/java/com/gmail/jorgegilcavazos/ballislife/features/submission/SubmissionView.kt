package com.gmail.jorgegilcavazos.ballislife.features.submission

import com.gmail.jorgegilcavazos.ballislife.features.model.CommentItem
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem
import io.reactivex.Observable
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentNode
import net.dean.jraw.models.Submission

interface SubmissionView {

  fun commentSaves(): Observable<CommentWrapper>

  fun commentUnsaves(): Observable<CommentWrapper>

  fun commentUpvotes(): Observable<CommentWrapper>

  fun commentDownvotes(): Observable<CommentWrapper>

  fun commentNovotes(): Observable<CommentWrapper>

  fun submissionShares(): Observable<Submission>

  fun submissionSaves(): Observable<Submission>

  fun submissionUnsaves(): Observable<Submission>

  fun submissionUpvotes(): Observable<Submission>

  fun submissionDownvotes(): Observable<Submission>

  fun submissionNovotes(): Observable<Submission>

  fun commentReplies(): Observable<CommentWrapper>

  fun submissionReplies(): Observable<Any>

  fun submissionContentClicks(): Observable<String>

  fun commentCollapses(): Observable<String>

  fun commentUnCollapses(): Observable<String>

	fun loadMoreComments(): Observable<CommentItem>

  fun setLoadingIndicator(active: Boolean)

  fun showComments(commentNodes: List<ThreadItem>, submission: Submission)

  fun addCommentItem(commentItem: CommentItem, parentId: String)

  fun addCommentItem(commentItem: CommentItem)

  fun showSubmittingCommentToast()

  fun showErrorAddingComment()

  fun showNotLoggedInError()

  fun showSavedCommentToast()

  fun showUnsavedCommentToast()

  fun showNoNetAvailable()

  fun openReplyToCommentActivity(parentComment: Comment)

  fun openReplyToSubmissionActivity(submissionId: String)

  fun openContentTab(url: String)

  fun openStreamable(shortcode: String)

  fun showContentUnavailableToast()

  fun scrollToComment(index: Int)

  fun hideFab()

  fun showFab()

  fun collapseComments(id: String)

  fun uncollapseComments(id: String)

  fun insertItemsBelowParent(threadItems: List<ThreadItem>, parentNode: CommentNode)

  fun showErrorLoadingMoreComments()

  fun share(url: String?)
}
