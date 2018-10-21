package com.gmail.jorgegilcavazos.ballislife.features.model

import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection

import java.io.Serializable

/**
 * Wraps a [Submission] to allow mutation.
 */
data class SubmissionWrapper(val id: String,
                             val submission: Submission?,
                             val title: String,
                             val author: String) : Serializable {
  var created: Long = 0
  var domain: String? = null
  var isSelfPost: Boolean = false
  var isStickied: Boolean = false
  var isHidden: Boolean = false
  var score: Int = 0
  var commentCount: Int = 0
  var thumbnail: String? = null
  var highResThumbnail: String? = null
  var voteDirection: VoteDirection? = null
  var isSaved: Boolean = false
  var selfTextHtml: String? = null
  var url: String? = null
  var sort: CommentSort? = null

  init {
    created = submission?.created?.time ?: 0
    domain = submission?.domain
    isSelfPost = submission?.isSelfPost == true
    isStickied = submission?.isStickied == true
    isHidden = submission?.isHidden == true
    score = submission?.score ?: 0
    commentCount = submission?.commentCount ?: 0
    thumbnail = submission?.thumbnail

    highResThumbnail = try {
      submission?.oEmbedMedia?.thumbnail?.url?.toString() ?: ""
    } catch (e: NullPointerException) {
      // Method getOEmbedMedia() and getThumbnail() methods in JRAW make incorrected assumptions
      // about nullability in of some fields and can throw NPEs.
      // See: https://github.com/mattbdean/JRAW/issues/198
      // If we catch a NPE just set the highResThumbnail to empty.
      // TODO(jorge): See if this has been fixed in JRAW 1.0+
      ""
    }

    voteDirection = submission?.vote
    isSaved = submission?.isSaved == true
    selfTextHtml = submission?.data("selftext_html")
    url = submission?.url
  }

  constructor(submission: Submission) :
      this(submission.id, submission, submission.title, submission.author)
}
