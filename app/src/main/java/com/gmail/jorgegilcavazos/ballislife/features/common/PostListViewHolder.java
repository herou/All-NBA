package com.gmail.jorgegilcavazos.ballislife.features.common;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;

import net.dean.jraw.models.VoteDirection;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PostListViewHolder extends RecyclerView.ViewHolder {

    public @BindView(R.id.content_container) View contentContainer;
    public @BindView(R.id.text_title) TextView tvTitle;
    public @BindView(R.id.text_author) TextView tvAuthor;
    public @BindView(R.id.text_timestamp) TextView tvTimestamp;
    public @BindView(R.id.text_domain) TextView tvDomain;
    public @BindView(R.id.thumbnail_container) View thumbnailContainer;
    public @BindView(R.id.image_thumbnail) ImageView ivThumbnail;
    public @BindView(R.id.image_thumbnail_type) ImageView ivThumbnailType;
    public @BindView(R.id.button_upvote) ImageButton btnUpvote;
    public @BindView(R.id.text_points) TextView tvPoints;
    public @BindView(R.id.button_downvote) ImageButton btnDownvote;
    public @BindView(R.id.text_comments) TextView tvComments;

    private int textColor;

    public PostListViewHolder(View itemView, int textColor) {
        super(itemView);
        ButterKnife.bind(this, itemView);
        this.textColor = textColor;
    }

    public void bindData(final Context context,
                         final RedditAuthentication redditAuthentication, final SubmissionWrapper
                                 submissionWrapper,
                         boolean isDisplayedInList,
                         final OnSubmissionClickListener submissionClickListener) {
        String title = submissionWrapper.getTitle();
        String author = submissionWrapper.getAuthor();
        long timestamp = submissionWrapper.getCreated();
        int commentCount = submissionWrapper.getCommentCount();
        String score = String.valueOf(submissionWrapper.getScore());
        String selfTextHtml = submissionWrapper.getSelfTextHtml();
        String domain = submissionWrapper.getDomain();
        String thumbnail = submissionWrapper.getThumbnail();
        final String url = submissionWrapper.getUrl();
        boolean isSelf = submissionWrapper.isSelfPost();
        boolean isStickied = submissionWrapper.isStickied();
        boolean isSaved = submissionWrapper.isSaved();
        VoteDirection vote = submissionWrapper.getVoteDirection();
        String highResThumbnail = submissionWrapper.getHighResThumbnail();

        // Show low res thumbnail over lower res version.
        String thumbnailToShow = thumbnail;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvTitle.setText(Html.fromHtml(title));
        }

        tvAuthor.setText(author);
        tvTimestamp.setText(DateFormatUtil.formatRedditDate(new Date(timestamp)));
        tvComments.setText(context.getString(R.string.num_comments, commentCount));
        tvPoints.setText(score);

        if (isSelf) {
            tvDomain.setText("self");
            ivThumbnail.setVisibility(View.GONE);
            ivThumbnailType.setVisibility(View.GONE);
        } else {
            tvDomain.setText(domain);
            if (thumbnailToShow != null) {
                ivThumbnail.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(thumbnailToShow)
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setVisibility(View.VISIBLE);
            }

            ivThumbnailType.setVisibility(View.GONE);
            if (domain.equals(Constants.STREAMABLE_DOMAIN)
                    || domain.equals(Constants.YOUTUBE_DOMAIN)) {
                ivThumbnailType.setVisibility(View.VISIBLE);
            }
        }

        // Set title font to green and bold if is stickied post.
        if (isStickied) {
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.stickiedColor));
            tvTitle.setTypeface(null, Typeface.BOLD);
        } else {
            tvTitle.setTextColor(textColor);
            tvTitle.setTypeface(null, Typeface.NORMAL);
        }

        // Set vote buttons colors if there submission has been voted on.
        if (vote == VoteDirection.UPVOTE) {
            setUpvotedColors(itemView.getContext());
        } else if (vote == VoteDirection.DOWNVOTE) {
            setDownvotedColors(itemView.getContext());
        } else {
            setNoVoteColors();
        }

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
                    setUpvotedColors(itemView.getContext());
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.UPVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.UPVOTE);
                    setUpvotedColors(itemView.getContext());
                }
            }
        });

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
                    setDownvotedColors(itemView.getContext());
                }
            } else {
                submissionClickListener.onVoteSubmission(submissionWrapper, VoteDirection.DOWNVOTE);
                if (redditAuthentication.isUserLoggedIn()) {
                    submissionWrapper.setVoteDirection(VoteDirection.DOWNVOTE);
                    setDownvotedColors(itemView.getContext());
                }
            }
        });

        thumbnailContainer.setOnClickListener(v -> submissionClickListener.onContentClick(url));

        if (isDisplayedInList) {
            // Enable buttons to navigate to SubmissionActivity.
            contentContainer.setOnClickListener(v ->
                    submissionClickListener.onSubmissionClick(submissionWrapper.getId()));
        }
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
}
