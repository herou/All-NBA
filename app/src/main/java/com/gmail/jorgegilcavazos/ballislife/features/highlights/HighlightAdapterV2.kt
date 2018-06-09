package com.gmail.jorgegilcavazos.ballislife.features.highlights

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.gmail.jorgegilcavazos.ballislife.R
import com.gmail.jorgegilcavazos.ballislife.features.common.SwishCardViewHolder
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard
import com.gmail.jorgegilcavazos.ballislife.util.StringUtils
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * RecyclerView adapter for a [Highlight]. Only one card should be enabled simultaneously.
 */
class HighlightAdapterV2(
    private val context: Context,
    private val highlights: MutableList<Highlight>,
    private var highlightViewType: HighlightViewType,
    private val isPremium: Boolean,
    private var showSwishSortingCard: Boolean = false,
    private var showSwishFavoritesCard: Boolean = false,
    private var showAddFavoritesCard: Boolean = false

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private val viewClickSubject = PublishSubject.create<Highlight>()
  private val shareClickSubject = PublishSubject.create<Highlight>()
  private val favoriteClicks = PublishRelay.create<Highlight>()
  private val submissionClickSubject = PublishSubject.create<Highlight>()
  private val exploreClicks = PublishSubject.create<SwishCard>()
  private val gotItClicks = PublishSubject.create<SwishCard>()

  fun ViewGroup.inflate(layoutResId: Int): View =
      LayoutInflater.from(this.context).inflate(layoutResId, this, false)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return when (viewType) {
      R.layout.row_highlight -> HighlightHolder(parent.inflate(R.layout.row_highlight))
      R.layout.row_highlight_small -> HighlightHolder(parent.inflate(R.layout.row_highlight_small))
      R.layout.swish_edu_card -> SwishCardViewHolder(parent.inflate(R.layout.swish_edu_card))
      else -> throw IllegalStateException("No matching viewholder for viewType: " + viewType)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is HighlightHolder -> {
        val highlight = if (showSwishSortingCard || showSwishFavoritesCard ||
            showAddFavoritesCard) {
          // If a card is being shown then the adapter position given as the parameter represents the
          // highlight at position - 1.
          highlights[position - 1]
        } else {
          highlights[position]
        }

        holder.bindData(
            isPremium = isPremium,
            contentViewType = highlightViewType,
            highlight = highlight,
            viewClickSubject = viewClickSubject,
            shareClickSubject = shareClickSubject,
            favoriteClicks = favoriteClicks,
            submissionClickSubject = submissionClickSubject
        )
      }
      is SwishCardViewHolder -> {
        val swishCard = when {
          showSwishSortingCard -> SwishCard.HIGHLIGHT_SORTING
          showSwishFavoritesCard -> SwishCard.HIGHLIGHT_FAVORITES
          showAddFavoritesCard -> SwishCard.EMPTY_FAVORITE_HIGHLIGHTS
          else -> throw IllegalStateException("Non valid flags for swish card")
        }
        holder.bindData(swishCard, exploreClicks, gotItClicks)
      }
    }
  }

  override fun getItemCount(): Int = when {
    showSwishSortingCard || showSwishFavoritesCard || showAddFavoritesCard -> highlights.size + 1
    else -> highlights.size
  }

  override fun getItemViewType(position: Int): Int = when {
    showSwishSortingCard && position == 0 -> R.layout.swish_edu_card
    showSwishFavoritesCard && position == 0 -> R.layout.swish_edu_card
    showAddFavoritesCard && position == 0 -> R.layout.swish_edu_card
    highlightViewType == HighlightViewType.LARGE -> R.layout.row_highlight
    highlightViewType == HighlightViewType.SMALL -> R.layout.row_highlight_small
    else -> throw IllegalStateException("No matching layout for pos: " + position)
  }

  fun removeSwishCard(swishCard: SwishCard) {
    when (swishCard) {
      SwishCard.HIGHLIGHT_SORTING -> showSwishSortingCard = false
      SwishCard.HIGHLIGHT_FAVORITES -> showSwishFavoritesCard = false
      SwishCard.EMPTY_FAVORITE_HIGHLIGHTS -> showAddFavoritesCard = false
    }
    notifyItemRemoved(0)
  }

  fun setData(highlights: List<Highlight>) {
    this.highlights.clear()
    this.highlights.addAll(highlights)
    preFetchImages(highlights)
    notifyDataSetChanged()
  }

  fun addData(highlights: List<Highlight>) {
    this.highlights.addAll(highlights)
    preFetchImages(highlights)
    notifyDataSetChanged()
  }

  fun addHighlight(highlight: Highlight) {
    highlights.add(highlight)
    notifyDataSetChanged()
  }

  fun addHighlightToTop(highlight: Highlight) {
    highlights.add(0, highlight)
    notifyItemInserted(0)
  }

  fun removeHighlight(highlight: Highlight) {
    for (i in highlights.indices) {
      if (highlights[i].id == highlight.id) {
        highlights.removeAt(i)
        notifyItemRemoved(i)
        break
      }
    }
  }

  fun setContentViewType(viewType: HighlightViewType) {
    this.highlightViewType = viewType
    notifyDataSetChanged()
  }

  fun getViewClickObservable(): Observable<Highlight> = viewClickSubject

  fun getShareClickObservable(): Observable<Highlight> = shareClickSubject

  fun getFavoriteClicks(): Observable<Highlight> = favoriteClicks

  fun getSubmissionClickObservable(): Observable<Highlight> = submissionClickSubject

  fun getExplorePremiumClicks(): Observable<SwishCard> = exploreClicks

  fun getGotItClicks(): Observable<SwishCard> = gotItClicks

  private fun preFetchImages(highlights: List<Highlight>) {
    for ((_, _, thumbnail, hdThumbnail) in highlights) {
      if (StringUtils.isNullOrEmpty(hdThumbnail)) {
        if (!StringUtils.isNullOrEmpty(thumbnail)) {
          Glide.with(context).load(thumbnail).preload()
        }
      } else {
        Glide.with(context).load(hdThumbnail).preload()
      }
    }
  }
}