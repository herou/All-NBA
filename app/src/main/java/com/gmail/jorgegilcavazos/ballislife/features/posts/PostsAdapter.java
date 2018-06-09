package com.gmail.jorgegilcavazos.ballislife.features.posts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.features.common.OnSubmissionClickListener;
import com.gmail.jorgegilcavazos.ballislife.features.common.PostListViewHolder;
import com.gmail.jorgegilcavazos.ballislife.features.model.NBASubChips;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.util.Pair;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.google.common.base.Optional;
import com.google.firebase.crash.FirebaseCrash;

import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.gmail.jorgegilcavazos.ballislife.util.Constants.POSTS_VIEW_LIST;
import static com.gmail.jorgegilcavazos.ballislife.util.Constants.POSTS_VIEW_WIDE_CARD;
import static com.gmail.jorgegilcavazos.ballislife.util.Constants.VIEW_HEADER;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private RedditAuthentication redditAuthentication;
    private List<SubmissionWrapper> postsList;
    private NBASubChips nbaSubChips;
    private int contentViewType;
    private OnSubmissionClickListener submissionClickListener;
    private SubscriberCount subscriberCount;
    private String subreddit;
    private int textColor;
    private SwishTheme theme;

    private PublishSubject<Submission> sharePublishSubject = PublishSubject.create();

    public PostsAdapter(Context context,
                        RedditAuthentication redditAuthentication,
                        List<SubmissionWrapper> postsList,
                        int contentViewType,
                        OnSubmissionClickListener submissionClickListener,
                        String subreddit,
                        int textColor,
                        SwishTheme theme) {
        this.context = context;
        this.redditAuthentication = redditAuthentication;
        this.postsList = postsList;
        this.contentViewType = contentViewType;
        this.submissionClickListener = submissionClickListener;
        this.subreddit = subreddit;
        this.textColor = textColor;
        this.theme = theme;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view;
        if (viewType == VIEW_HEADER) {
            view = inflater.inflate(R.layout.rnba_header_layout, parent, false);
            return new HeaderViewHolder(view, theme);
        }

        switch (viewType) {
            case POSTS_VIEW_LIST:
                view = inflater.inflate(R.layout.post_layout_list, parent, false);
                return new PostListViewHolder(view, textColor);
            case POSTS_VIEW_WIDE_CARD:
                view = inflater.inflate(R.layout.post_layout_card_wide, parent, false);
                return new WideCardViewHolder(view, textColor);
            default:
                throw new IllegalStateException("Posts view type is not valid: " + contentViewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bindData(subscriberCount, subreddit, nbaSubChips,
                    submissionClickListener);
        } else {
            SubmissionWrapper submissionWrapper = postsList.get(position - 1);
            switch (contentViewType) {
                case POSTS_VIEW_LIST:
                    ((PostListViewHolder) holder).bindData(
                            context, redditAuthentication, submissionWrapper,
                            true,
                            submissionClickListener);
                    break;
                case POSTS_VIEW_WIDE_CARD:
                    ((WideCardViewHolder) holder).bindData(
                            context, redditAuthentication, submissionWrapper,
                            submissionClickListener,
                            sharePublishSubject);
                    break;
                default:
                    throw new IllegalStateException("Invalid view type in bind view holder: "
                            + contentViewType);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_HEADER;
        }
        if (contentViewType == 0) {
            FirebaseCrash.report(new IllegalArgumentException("contentViewType should not be 0"));
            return POSTS_VIEW_WIDE_CARD;
        }
        return contentViewType;

    }

    @Override
    public int getItemCount() {
        // Add 1 for header.
        return null != postsList ? postsList.size() + 1 : 1;
    }

    public void setData(List<SubmissionWrapper> submissions) {
        if (postsList != null) {
            postsList.clear();
        } else {
            postsList = new ArrayList<>();
        }

        postsList.addAll(submissions);
        preFetchImages(submissions);
        notifyDataSetChanged();
    }

    public void addData(List<SubmissionWrapper> submissions) {
        if (postsList == null) {
            postsList = new ArrayList<>();
        }

        postsList.addAll(submissions);
        preFetchImages(submissions);
        notifyDataSetChanged();
    }

    public void setSubscriberCount(SubscriberCount subscriberCount) {
        this.subscriberCount = subscriberCount;
        notifyDataSetChanged();
    }

    public void setContentViewType(int viewType) {
        contentViewType = viewType;
        notifyDataSetChanged();
    }

    public void setNBASubChips(NBASubChips nbaSubChips) {
        this.nbaSubChips = nbaSubChips;
        notifyItemChanged(0);
    }

    public Observable<Submission> getShareObservable() {
        return sharePublishSubject;
    }

    private void preFetchImages(List<SubmissionWrapper> submissions) {
        for (SubmissionWrapper submission : submissions) {
            Optional<Pair<Utilities.ThumbnailType, String>> thumbnailTypeUrl =
                    Utilities.getThumbnailToShowFromCustomSubmission(submission);
            if (thumbnailTypeUrl.isPresent()) {
                Glide.with(context)
                        .load(thumbnailTypeUrl.get().second)
                        .preload();
            }
        }
    }
}
