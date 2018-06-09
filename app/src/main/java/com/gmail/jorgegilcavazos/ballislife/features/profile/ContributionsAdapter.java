package com.gmail.jorgegilcavazos.ballislife.features.profile;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Lists "contributions" of a reddit user. Contributions can be comments or link posts.
 */
public class ContributionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int LINK_TYPE = 0;
    private static final int COMMENT_TYPE = 1;

    private Context context;
    private List<Contribution> contributions;
    private PublishSubject<PublicContribution> clickSubject = PublishSubject.create();

    public ContributionsAdapter(Context context, List<Contribution> contributions) {
        this.context = context;
        this.contributions = contributions;
    }

    @Override
    public int getItemViewType(int position) {
        Contribution contribution = contributions.get(position);
        if (contribution.getDataNode().get("name").toString().contains("t1")) {
            return COMMENT_TYPE;
        } else {
            return LINK_TYPE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case LINK_TYPE:
                return new LinkViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.contribution_link_layout, parent, false));
            case COMMENT_TYPE:
                return new CommentViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.contribution_comment_layout, parent, false));
            default:
                throw new IllegalStateException("Contribution view type is invalid: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case LINK_TYPE:
                LinkViewHolder linkViewHolder = (LinkViewHolder) holder;
                setLinkViewHolderViews(linkViewHolder, position);
                break;
            case COMMENT_TYPE:
                CommentViewHolder commentViewHolder = (CommentViewHolder) holder;
                setCommentViewHolderViews(commentViewHolder, position);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return null != contributions ? contributions.size() : 0;
    }

    private void setLinkViewHolderViews(LinkViewHolder holder, int position) {
        final Submission post = new Submission(contributions.get(position).getDataNode());

        holder.titleView.setText(post.getTitle());
        holder.scoreView.setText(String.valueOf(post.getScore()));
        holder.authorView.setText(post.getAuthor());
        holder.createdView.setText(DateFormatUtil.formatRedditDate(post.getCreated()));
        holder.numOfCommentsView.setText(String.valueOf(post.getCommentCount()));

        String thumbnailUrl = post.getThumbnail();

        if (post.isSelfPost()) {
            holder.linkView.setText("• self." + post.getSubredditName());
            holder.thumbnailContainer.setVisibility(View.GONE);
        } else {
            String domain = post.getDomain();
            holder.linkView.setText("• " + domain);
            if (thumbnailUrl != null) {
                Glide.with(context).load(thumbnailUrl).into(holder.thumbnail);
                switch (domain) {
                    case Constants.STREAMABLE_DOMAIN:
                        holder.thumbnailType.setImageResource(R.drawable
                                .ic_play_circle_outline_black_24dp);
                        break;
                    case Constants.IMGUR_DOMAIN:
                    case Constants.GIPHY_DOMAIN:
                        holder.thumbnailType.setImageResource(R.drawable.ic_gif_black_24dp);
                        break;
                    default:
                        holder.thumbnailType.setVisibility(View.GONE);
                        break;
                }
            } else {
                holder.thumbnailContainer.setVisibility(View.GONE);
            }
        }
        holder.container.setOnClickListener(v -> clickSubject.onNext(post));
    }

    private void setCommentViewHolderViews(CommentViewHolder holder, int position) {
        final Comment comment = new Comment(contributions.get(position).getDataNode());

        holder.postTitleTextView.setText(comment.getSubmissionTitle());
        holder.authorTextView.setText(comment.getAuthor());
        holder.bodyTextView.setText(RedditUtils.bindSnuDown(comment.data("body_html")));
        holder.timestampTextView.setText(DateFormatUtil.formatRedditDate(comment.getCreated()));
        holder.scoreTextView.setText(context.getString(R.string.points,
                String.valueOf(comment.getScore())));
        holder.container.setOnClickListener(v -> clickSubject.onNext(comment));
    }

    public void setData(List<Contribution> data) {
        contributions.clear();
        contributions.addAll(data);
        notifyDataSetChanged();
    }

    public void addData(List<Contribution> data) {
        contributions.addAll(data);
        notifyDataSetChanged();
    }

    public void clearData() {
        contributions.clear();
        notifyDataSetChanged();
    }

    public Observable<PublicContribution> getClickObservable() {
        return clickSubject;
    }

    public static class LinkViewHolder extends RecyclerView.ViewHolder {
        View container;

        @BindView(R.id.scoreView) TextView scoreView;
        @BindView(R.id.authorView) TextView authorView;
        @BindView(R.id.createdView) TextView createdView;
        @BindView(R.id.titleView) TextView titleView;
        @BindView(R.id.numCommentsView) TextView numOfCommentsView;
        @BindView(R.id.linkView) TextView linkView;
        @BindView(R.id.thumbnail_container) View thumbnailContainer;
        @BindView(R.id.thumbnail) ImageView thumbnail;
        @BindView(R.id.thumbnail_content_type) ImageView thumbnailType;
        public LinkViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            container = view;
        }

    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        View container;

        @BindView(R.id.comment_post_title) TextView postTitleTextView;
        @BindView(R.id.comment_author) TextView authorTextView;
        @BindView(R.id.comment_score) TextView scoreTextView;
        @BindView(R.id.comment_timestamp) TextView timestampTextView;
        @BindView(R.id.comment_body) TextView bodyTextView;
        public CommentViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            container = view;
        }
    }
}
