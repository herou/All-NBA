package com.gmail.jorgegilcavazos.ballislife.features.posts;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.features.common.OnSubmissionClickListener;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;
import com.gmail.jorgegilcavazos.ballislife.util.Pair;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities.ThumbnailType;
import com.google.common.base.Optional;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.subjects.PublishSubject;

/**
 * Holder that binds Reddit {@link SubmissionWrapper} data to it's view.
 * TODO: Refactor to user PublishSubject instead of listeners for all click actions.
 */
public class WideCardViewHolder extends RecyclerView.ViewHolder {

    public @BindView(R.id.layout_container) View layoutContainer;
    public @BindView(R.id.image_thumbnail) ImageView ivThumbnail;
    public @BindView(R.id.text_title) TextView tvTitle;
    public @BindView(R.id.layout_small_image_title) View layoutSmallImageTitle;
    public @BindView(R.id.image_thumbnail_small) ImageView ivThumbnailSmall;
    public @BindView(R.id.text_title_small) TextView tvTitleSmall;
    public @BindView(R.id.text_body) TextView tvBody;
    public @BindView(R.id.button_upvote) ImageButton btnUpvote;
    public @BindView(R.id.button_downvote) ImageButton btnDownvote;
    public @BindView(R.id.text_points) TextView tvPoints;
    public @BindView(R.id.text_comments) TextView tvComments;
    public @BindView(R.id.button_more) ImageButton btnMore;
    public @BindView(R.id.button_save) ImageButton btnSave;
    public @BindView(R.id.button_share) ImageButton btnShare;
    public @BindView(R.id.text_posted_details) TextView tvPostedDetails;

    private int textColor;

    public WideCardViewHolder(View itemView, int textColor) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.textColor = textColor;
    }

    private static String getUsableRedditDate(Context context, Date timestamp) {
        Pair<Integer, Optional<Long>> usableDate = DateFormatUtil.formatRedditDateLong(timestamp);
        switch (usableDate.first) {
            case DateFormatUtil.TIME_UNIT_JUST_NOW:
                return context.getString(R.string.just_now);
            case DateFormatUtil.TIME_UNIT_MINUTES:
                return context.getString(R.string.minutes_ago, usableDate.second.get());
            case DateFormatUtil.TIME_UNIT_HOURS:
                return context.getString(R.string.hours_ago, usableDate.second.get());
            case DateFormatUtil.TIME_UNIT_DAYS:
                return context.getString(R.string.days_ago, usableDate.second.get());
            default:
                throw new IllegalStateException("Invalid TIME UNIT: " + usableDate.first);
        }
    }

    public void bindData(final Context context,
                         RedditAuthentication redditAuthentication, final SubmissionWrapper
                                 submissionWrapper,
                         final OnSubmissionClickListener submissionClickListener,
                         final PublishSubject<Submission> shareSubject) {
        String title = submissionWrapper.getTitle();
        String author = submissionWrapper.getAuthor();
        long timestamp = submissionWrapper.getCreated();
        int commentCount = submissionWrapper.getCommentCount();
        int score = submissionWrapper.getScore();
        String selfTextHtml = submissionWrapper.getSelfTextHtml();
        String domain = submissionWrapper.getDomain();
        String url = submissionWrapper.getUrl();
        boolean isSelf = submissionWrapper.isSelfPost();
        boolean isStickied = submissionWrapper.isStickied();
        boolean isSaved = submissionWrapper.isSaved();
        VoteDirection vote = submissionWrapper.getVoteDirection();

        Optional<Pair<ThumbnailType, String>> thumbnailTypeUrl = Utilities
                .getThumbnailToShowFromCustomSubmission(submissionWrapper);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
            tvTitleSmall.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvTitle.setText(Html.fromHtml(title));
            tvTitleSmall.setText(Html.fromHtml(title));
        }

        // Show / hide thumbnails and titles depending on which thumbnail is available.
        ivThumbnail.setVisibility(View.GONE);
        ivThumbnailSmall.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);
        tvTitleSmall.setVisibility(View.GONE);
        layoutSmallImageTitle.setVisibility(View.GONE);
        boolean shouldShowThumbnail = !isSelf && thumbnailTypeUrl.isPresent();
        if (shouldShowThumbnail) {
            ThumbnailType thumbnailType = thumbnailTypeUrl.get().first;
            String thumbnailUrl = thumbnailTypeUrl.get().second;
            switch (thumbnailType) {
                case LOW_RES:
                    Glide.with(context).load(thumbnailUrl).into(ivThumbnailSmall);
                    ivThumbnailSmall.setVisibility(View.VISIBLE);
                    layoutSmallImageTitle.setVisibility(View.VISIBLE);
                    tvTitleSmall.setVisibility(View.VISIBLE);
                    break;
                case HIGH_RES:
                    Glide.with(context).load(thumbnailUrl).into(ivThumbnail);
                    ivThumbnail.setVisibility(View.VISIBLE);
                    tvTitle.setVisibility(View.VISIBLE);
                    break;
                default:
                    throw new IllegalStateException("Invalid thumbnail type: " + thumbnailType);
            }
        } else {
            tvTitle.setVisibility(View.VISIBLE);
        }

        tvPoints.setText(context.getString(R.string.num_points, score));
        tvComments.setText(context.getString(R.string.num_comments, commentCount));

        tvPostedDetails.setText(context.getString(R.string.posted_details,
                getUsableRedditDate(context, new Date(timestamp)), author, domain));

        // Set title font to green and bold if is stickied post.
        if (isStickied) {
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.stickiedColor));
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitleSmall.setTextColor(ContextCompat.getColor(context, R.color.stickiedColor));
            tvTitleSmall.setTypeface(null, Typeface.BOLD);
        } else {
            tvTitle.setTextColor(textColor);
            tvTitle.setTypeface(null, Typeface.NORMAL);
            tvTitleSmall.setTextColor(textColor);
            tvTitleSmall.setTypeface(null, Typeface.NORMAL);
        }

        if (isSelf && selfTextHtml != null && !selfTextHtml.isEmpty()) {
            tvBody.setVisibility(View.VISIBLE);
            tvBody.setText(RedditUtils.bindSnuDown(selfTextHtml));
        } else {
            tvBody.setVisibility(View.GONE);
        }

        // Set vote buttons colors if the submission has been voted on.
        if (vote == VoteDirection.UPVOTE) {
            setUpvotedColors(context);
        } else if (vote == VoteDirection.DOWNVOTE) {
            setDownvotedColors(context);
        } else {
            setNoVoteColors();
        }

        // Set saved button color depending on whether the submission has been saved.
        if (isSaved) {
            setSavedIcon();
        } else {
            setUnsavedIcon();
        }

        initSaveBtnListener(redditAuthentication, submissionWrapper, submissionClickListener);
        initShareBtnListener(submissionWrapper.getSubmission(), shareSubject);
        initUpvoteBtnListener(context, redditAuthentication, submissionWrapper,
                submissionClickListener);
        initDownvoteBtnListener(context, redditAuthentication, submissionWrapper,
                submissionClickListener);
        initThumbnailListener(url, submissionClickListener);
        initContainerListener(submissionWrapper, submissionClickListener);
        initMoreBtnListener(context, submissionWrapper, submissionClickListener);
    }

    private void setUpvotedColors(Context context) {
        setUpvoteIcon(true);
        setDownvoteIcon(false);
        tvPoints.setTextColor(ContextCompat.getColor(context, R.color.commentUpvoted));
    }

    private void setDownvotedColors(Context context) {
        setUpvoteIcon(false);
        setDownvoteIcon(true);
        tvPoints.setTextColor(ContextCompat.getColor(context, R.color.commentDownvoted));
    }

    private void setNoVoteColors() {
        setUpvoteIcon(false);
        setDownvoteIcon(false);
        tvPoints.setTextColor(textColor);
    }

    private void setUpvoteIcon(boolean active) {
        if (active) {
            btnUpvote.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color
                    .commentUpvoted));
        } else {
            btnUpvote.setColorFilter(textColor);
        }
    }

    private void setDownvoteIcon(boolean active) {
        if (active) {
            btnDownvote.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color
                    .commentDownvoted));
        } else {
            btnDownvote.setColorFilter(textColor);
        }
    }

    private void setSavedIcon() {
        btnSave.setImageResource(R.drawable.ic_bookmark_black_18dp);
        btnSave.setColorFilter(textColor);
    }

    private void setUnsavedIcon() {
        btnSave.setImageResource(R.drawable.ic_bookmark_border_black_18dp);
        btnSave.setColorFilter(textColor);
    }

    private void initSaveBtnListener(final RedditAuthentication redditAuthentication, final
    SubmissionWrapper submissionWrapper,
                                     final OnSubmissionClickListener submissionClickListener) {
        btnSave.setOnClickListener(v -> {
            if (submissionWrapper.isSaved()) {
                submissionClickListener.onSaveSubmission(submissionWrapper, false);
                if (redditAuthentication.isUserLoggedIn()) {
                    setUnsavedIcon();
                    submissionWrapper.setSaved(false);
                }
            } else {
                submissionClickListener.onSaveSubmission(submissionWrapper, true);
                if (redditAuthentication.isUserLoggedIn()) {
                    setSavedIcon();
                    submissionWrapper.setSaved(true);
                }
            }
        });
    }

    private void initUpvoteBtnListener(final Context context, final RedditAuthentication redditAuthentication, final SubmissionWrapper submissionWrapper,
                                       final OnSubmissionClickListener submissionClickListener) {
        btnUpvote.setOnClickListener(v -> {
            if (submissionWrapper.getVoteDirection() == VoteDirection.UPVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.NO_VOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.NO_VOTE);
                    setNoVoteColors();
                }
            } else if (submissionWrapper.getVoteDirection() == VoteDirection.DOWNVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.UPVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.UPVOTE);
                    setUpvotedColors(context);
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.UPVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.UPVOTE);
                    setUpvotedColors(context);
                }
            }
        });
    }

    private void initDownvoteBtnListener(final Context context, final RedditAuthentication
            redditAuthentication, final SubmissionWrapper submissionWrapper,
                                         final OnSubmissionClickListener submissionClickListener) {
        btnDownvote.setOnClickListener(v -> {
            if (submissionWrapper.getVoteDirection() == VoteDirection.DOWNVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.NO_VOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.NO_VOTE);
                    setNoVoteColors();
                }
            } else if (submissionWrapper.getVoteDirection() == VoteDirection.UPVOTE) {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.DOWNVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.DOWNVOTE);
                    setDownvotedColors(context);
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.DOWNVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.DOWNVOTE);
                    setDownvotedColors(context);
                }
            }
        });
    }

    private void initShareBtnListener(final Submission submission,
                                      final PublishSubject<Submission> shareSubject) {
        btnShare.setOnClickListener(v -> shareSubject.onNext(submission));
    }

    private void initThumbnailListener(final String url,
                                       final OnSubmissionClickListener submissionClickListener) {
        ivThumbnail.setOnClickListener(v -> submissionClickListener.onContentClick(url));
        ivThumbnailSmall.setOnClickListener(v -> submissionClickListener.onContentClick(url));
    }

    private void initContainerListener(final SubmissionWrapper submissionWrapper,
                                       final OnSubmissionClickListener submissionClickListener) {
        layoutContainer.setOnClickListener(v -> submissionClickListener.onSubmissionClick
                (submissionWrapper.getId()));
    }

    private void initMoreBtnListener(Context context,
                                     SubmissionWrapper submissionWrapper,
                                     OnSubmissionClickListener submissionClickListener) {
        btnMore.setOnClickListener(btn -> createPopUpMenu(context,
                btn,
                submissionWrapper,
                submissionClickListener));
    }

    private void createPopUpMenu(Context context,
                                 View btn,
                                 SubmissionWrapper submissionWrapper,
                                 OnSubmissionClickListener submissionClickListener) {
        PopupMenu popupMenu = new PopupMenu(context, btn);
        popupMenu.getMenuInflater().inflate(R.menu.menu_post_overflow, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.action_hide:
                    submissionClickListener.onHideSubmission(submissionWrapper);
                    return true;
            }
            return false;
        });
        popupMenu.show();
    }
}
