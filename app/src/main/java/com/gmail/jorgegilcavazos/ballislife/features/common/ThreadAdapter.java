package com.gmail.jorgegilcavazos.ballislife.features.common;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.LoadMoreCommentsViewHolder;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentItem;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem;
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItemType;
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity;
import com.gmail.jorgegilcavazos.ballislife.util.DateFormatUtil;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.StringUtils;
import com.gmail.jorgegilcavazos.ballislife.util.UnitUtils;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItemType.COMMENT;
import static com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItemType.LOAD_MORE_COMMENTS;
import static com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItemType.SUBMISSION_HEADER;

/**
 * Adapter used to hold all of the comments from a threadId. It also supports loading a header view
 * as the first element of the recycler view.
 * The (optional) header is loaded based on the hasHeader field of the constructor and when true
 * loads a {@link FullCardViewHolder} with information about the submission.
 * <p>
 * Currently used to display comments only in the
 * {@link com.gmail.jorgegilcavazos.ballislife.features.gamethread.GameThreadFragment} and to
 * display comments AND a the card for a full submission in the
 * {@link SubmissionActivity}.
 */
public class ThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private LocalRepository localRepository;
    private PremiumService premiumService;
    private Context context;
    private List<ThreadItem> commentsList;
    private Map<String, List<ThreadItem>> collapsedItems = new HashMap<>();
    private boolean hasHeader;
    private SubmissionWrapper submissionWrapper;
    private int textColor;

    private PublishSubject<CommentWrapper> commentSaves = PublishSubject.create();
    private PublishSubject<CommentWrapper> commentUnsaves = PublishSubject.create();
    private PublishSubject<CommentWrapper> upvotes = PublishSubject.create();
    private PublishSubject<CommentWrapper> downvotes = PublishSubject.create();
    private PublishSubject<CommentWrapper> novotes = PublishSubject.create();
    private PublishSubject<CommentWrapper> replies = PublishSubject.create();
    private PublishSubject<Submission> submissionShares = PublishSubject.create();
    private PublishSubject<Submission> submissionSaves = PublishSubject.create();
    private PublishSubject<Submission> submissionUnsaves = PublishSubject.create();
    private PublishSubject<Submission> submissionUpvotes = PublishSubject.create();
    private PublishSubject<Submission> submissionDownvotes = PublishSubject.create();
    private PublishSubject<Submission> submissionNovotes = PublishSubject.create();
    private PublishSubject<String> submissionContentClicks = PublishSubject.create();
    private PublishSubject<String> commentCollapses = PublishSubject.create();
    private PublishSubject<String> commentUnCollapses = PublishSubject.create();
    private PublishSubject<CommentItem> loadMoreComments = PublishSubject.create();

    public ThreadAdapter(Context context,
                         PremiumService premiumService,
                         LocalRepository localRepository,
                         List<ThreadItem> commentsList,
                         boolean hasHeader,
                         int textColor) {
        this.context = context;
        this.premiumService = premiumService;
        this.localRepository = localRepository;
        this.commentsList = commentsList;
        this.hasHeader = hasHeader;
        this.textColor = textColor;
    }

    public void setSubmissionWrapper(SubmissionWrapper submissionWrapper) {
        this.submissionWrapper = submissionWrapper;
    }

    public void setSubmission(Submission submission) {
        submissionWrapper = new SubmissionWrapper(submission);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View view;

        if (viewType == SUBMISSION_HEADER.getValue()) {
            view = inflater.inflate(R.layout.post_layout_card, parent, false);
            return new FullCardViewHolder(view, textColor, localRepository.getAppTheme(),
                    premiumService);
        } else if (viewType == COMMENT.getValue()) {
            view = inflater.inflate(R.layout.comment_layout, parent, false);
            return new CommentViewHolder(view, textColor, localRepository.getAppTheme());
        } else if (viewType == LOAD_MORE_COMMENTS.getValue()) {
            view = inflater.inflate(R.layout.layout_load_more_comments, parent, false);
            return new LoadMoreCommentsViewHolder(view, localRepository.getAppTheme());
        } else {
            throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof FullCardViewHolder) {
            ((FullCardViewHolder) holder).bindData(context, localRepository,
                    submissionWrapper, submissionShares, submissionSaves, submissionUnsaves,
                    submissionUpvotes, submissionDownvotes, submissionNovotes,
                    submissionContentClicks);
        } else if (holder instanceof CommentViewHolder) {
            final CommentViewHolder commentHolder = (CommentViewHolder) holder;

            final ThreadItem item;
            if (hasHeader && submissionWrapper != null) {
                item = commentsList.get(position - 1);
            } else {
                item = commentsList.get(position);
            }
            String submissionAuthor = null;
            if (submissionWrapper != null) {
                submissionAuthor = submissionWrapper.getAuthor();
            }
            commentHolder.bindData(context, item, localRepository, commentSaves,
                    commentUnsaves, upvotes, downvotes, novotes, replies, commentCollapses,
                    commentUnCollapses, submissionAuthor);
        } else if (holder instanceof LoadMoreCommentsViewHolder) {
            if (hasHeader) {
                ((LoadMoreCommentsViewHolder) holder).bindData(commentsList.get(position - 1),
                        loadMoreComments);
            } else {
                ((LoadMoreCommentsViewHolder) holder).bindData(commentsList.get(position),
                        loadMoreComments);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        // Submission data guaranteed to be not null at this point.
        if (hasHeader) {
            if (position == 0) {
                return SUBMISSION_HEADER.getValue();
            } else {
                return commentsList.get(position - 1).getType().getValue();
            }
        }
        return commentsList.get(position).getType().getValue();
    }

    @Override
    public int getItemCount() {
        if (!hasHeader) {
            return null != commentsList ? commentsList.size() : 0;
        } else {
            if (submissionWrapper == null) {
                // Don't show anything until we have a submission to show.
                return 0;
            } else {
                return null != commentsList ? commentsList.size() + 1 : 1;
            }
        }
    }

    public void setData(List<ThreadItem> data) {
        commentsList.clear();
        commentsList.addAll(data);
        notifyDataSetChanged();
    }

    public void addCommentItem(CommentItem commentItem, String parentId) {
        for (int i = 0; i < commentsList.size(); i++) {
            ThreadItem item = commentsList.get(i);
            if (item.getType() == COMMENT) {
                if (item.getCommentItem() != null && item.getCommentItem()
                        .getCommentWrapper()
                        .getId()
                        .equals(parentId)) {
                    commentItem.setDepth(item.getCommentItem().getDepth() + 1);
                    commentsList.add(i + 1, new ThreadItem(COMMENT, commentItem, commentItem
                            .getDepth(), false));
                    int adapterPosInserted;
                    if (hasHeader) {
                        adapterPosInserted = i + 2;
                    } else {
                        adapterPosInserted = i + 1;
                    }
                    notifyItemInserted(adapterPosInserted);
                    break;
                }
            }
        }
    }

    public void addCommentItem(CommentItem commentItem) {
        commentsList.add(0, new ThreadItem(COMMENT, commentItem, 0, false));
        int adapterPosInserted;
        if (hasHeader) {
            adapterPosInserted = 1;
        } else {
            adapterPosInserted = 0;
        }
        notifyItemInserted(adapterPosInserted);
    }

    public void collapseComments(String commentId) {
        List<ThreadItem> itemsToCollapse = new ArrayList<>();
        boolean collapse = false;
        int depth = Integer.MAX_VALUE;

        int firstCollapse = -1;
        int lastCollapse = -1;
        for (int i = 0; i < commentsList.size(); i++) {
            ThreadItem item = commentsList.get(i);
            ThreadItemType itemType = item.getType();
            String itemId = item.getCommentItem().getCommentWrapper().getId();

            if (itemType == COMMENT && itemId.equals(commentId)) {
                collapse = true;
                depth = item.getCommentItem().getDepth();
            } else {
                int nextDepth;
                if (itemType == COMMENT) {
                    nextDepth = item.getCommentItem().getDepth();
                } else {
                    nextDepth = item.getDepth();
                }

                if (collapse) {
                    if (nextDepth > depth) {
                        itemsToCollapse.add(item);
                        if (firstCollapse == -1) {
                            firstCollapse = i;
                        }
                    } else {
                        lastCollapse = i - 1;
                        break;
                    }
                }
            }
        }

        commentsList.removeAll(itemsToCollapse);

        collapsedItems.put(commentId, itemsToCollapse);
        if (firstCollapse != -1 && lastCollapse != -1) {
            if (hasHeader) {
                notifyItemRangeRemoved(firstCollapse + 1, itemsToCollapse.size());
            } else {
                notifyItemRangeRemoved(firstCollapse, itemsToCollapse.size());
            }
        }
    }

    public void unCollapseComments(String commentId) {
        List<ThreadItem> itemsToUnCollapse = collapsedItems.get(commentId);
        if (itemsToUnCollapse != null && !itemsToUnCollapse.isEmpty()) {
            for (int i = 0; i < commentsList.size(); i++) {
                ThreadItem item = commentsList.get(i);
                if (item.getType() == COMMENT && item.getCommentItem()
                        .getCommentWrapper()
                        .getId()
                        .equals(commentId)) {
                    commentsList.addAll(i + 1, itemsToUnCollapse);
                    if (hasHeader) {
                        notifyItemRangeInserted(i + 2, itemsToUnCollapse.size());
                    } else {
                        notifyItemRangeInserted(i + 1, itemsToUnCollapse.size());
                    }
                    collapsedItems.remove(commentId);
                    break;
                }
            }
        }
    }

    public void insertItemsBelowParent(List<ThreadItem> items, CommentNode parent) {
        for (int i = 0; i < commentsList.size(); i++) {
            ThreadItem item = commentsList.get(i);
            if (item.getType() == LOAD_MORE_COMMENTS && item.getCommentItem()
                    .getCommentWrapper()
                    .getId()
                    .equals(parent.getComment().getId())) {
                commentsList.remove(i);
                if (hasHeader) {
                    notifyItemRemoved(i + 1);
                } else {
                    notifyItemRemoved(i);
                }
                commentsList.addAll(i, items);
                if (hasHeader) {
                    notifyItemRangeInserted(i + 1, items.size());
                } else {
                    notifyItemRangeInserted(i, items.size());
                }
                break;
            }
        }
    }

    public Observable<CommentWrapper> getCommentSaves() {
        return commentSaves;
    }

    public Observable<CommentWrapper> getCommentUnsaves() {
        return commentUnsaves;
    }

    public Observable<CommentWrapper> getUpvotes() {
        return upvotes;
    }

    public Observable<CommentWrapper> getDownvotes() {
        return downvotes;
    }

    public Observable<CommentWrapper> getNovotes() {
        return novotes;
    }

    public Observable<CommentWrapper> getReplies() {
        return replies;
    }

    public PublishSubject<Submission> getSubmissionShares() {
        return submissionShares;
    }

    public Observable<Submission> getSubmissionSaves() {
        return submissionSaves;
    }

    public Observable<Submission> getSubmissionUnsaves() {
        return submissionUnsaves;
    }

    public Observable<Submission> getSubmissionUpvotes() {
        return submissionUpvotes;
    }

    public Observable<Submission> getSubmissionDownvotes() {
        return submissionDownvotes;
    }

    public Observable<Submission> getSubmissionNovotes() {
        return submissionNovotes;
    }

    public Observable<String> getSubmissionContentClicks() {
        return submissionContentClicks;
    }

    public Observable<String> getCommentCollapses() {
        return commentCollapses;
    }

    public Observable<String> getCommentUnCollapses() {
        return commentUnCollapses;
    }

    public Observable<CommentItem> getLoadMoreComments() {
        return loadMoreComments;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.layout_comment_content) View commentContentLayout;
        @BindView(R.id.layout_comment_inner_content) View commentInnerContentLayout;
        @BindView(R.id.comment_author) TextView authorTextView;
        @BindView(R.id.comment_score) TextView scoreTextView;
        @BindView(R.id.comment_timestamp) TextView timestampTextView;
        @BindView(R.id.bodyTextContainer) LinearLayout bodyTextContainer;
        @BindView(R.id.comment_flair) TextView flairTextView;
        @BindView(R.id.image_flair) ImageView ivFlair;
        @BindView(R.id.layout_comment_actions) LinearLayout rlCommentActions;
        @BindView(R.id.button_comment_upvote) ImageButton btnUpvote;
        @BindView(R.id.button_comment_downvote) ImageButton btnDownvote;
        @BindView(R.id.button_comment_save) ImageButton btnSave;
        @BindView(R.id.button_comment_reply) ImageButton btnReply;
        @BindView(R.id.collapsedIndicator) TextView collapseIndicator;

        private int textColor;
        private SwishTheme swishTheme;

        public CommentViewHolder(View view, int textColor, SwishTheme swishTheme) {
            super(view);
            ButterKnife.bind(this, view);
            this.textColor = textColor;
            this.swishTheme = swishTheme;
        }

        public void bindData(final Context context,
                             final ThreadItem threadItem,
                             final LocalRepository localRepository,
                             PublishSubject<CommentWrapper> commentSaves,
                             PublishSubject<CommentWrapper> commentUnsaves,
                             PublishSubject<CommentWrapper> upvotes,
                             PublishSubject<CommentWrapper> downvotes,
                             PublishSubject<CommentWrapper> novotes,
                             PublishSubject<CommentWrapper> replies,
                             PublishSubject<String> commentCollapses,
                             PublishSubject<String> commentUncollapses,
                             String submissionAuthor) {
            final CommentItem commentItem = threadItem.getCommentItem();
            final CommentWrapper comment = commentItem.getCommentWrapper();
            int depth = commentItem.getDepth();
            String author = comment.getAuthor();
            String timestamp = DateFormatUtil.formatRedditDate(comment.getCreated());
            String score = String.valueOf(comment.getScore());
            String flair = RedditUtils.parseNbaFlair(String.valueOf(comment.getAuthorFlair()));
            String cssClass = RedditUtils.parseCssClassFromFlair(String.valueOf(comment
                    .getAuthorFlair()));
            int flairRes = RedditUtils.getFlairFromCss(cssClass);

            authorTextView.setText(author);
            if (comment.getAuthor().equals(submissionAuthor)) {
                authorTextView.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.redditAuthor));
            } else if (comment.getAuthor().equals(localRepository.getUsername())) {
                authorTextView.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.redditMe));
            } else {
                authorTextView.setTextColor(textColor);
            }

            RedditUtils.renderBody(itemView.getContext(), swishTheme, bodyTextContainer,
                    comment.getBodyHtml(), RedditUtils.BodyType.COMMENT);
            timestampTextView.setText(timestamp);
            scoreTextView.setText(context.getString(R.string.points, score));
            if (flairRes != -1) {
                ivFlair.setImageResource(flairRes);
                ivFlair.setVisibility(View.VISIBLE);
                flairTextView.setVisibility(View.GONE);
            } else {
                flairTextView.setText(flair);
                ivFlair.setVisibility(View.GONE);
                flairTextView.setVisibility(View.VISIBLE);
            }
            rlCommentActions.setVisibility(View.GONE);

            setBackgroundAndPadding(context, depth, this, false /* dark */,
                    localRepository.getAppTheme());

            if (commentItem.getChildrenCollapsed()) {
                collapseIndicator.setVisibility(View.VISIBLE);
            } else {
                collapseIndicator.setVisibility(View.GONE);
            }

            final CommentViewHolder commentHolder = this;

            // On comment click hide/show actions (upvote, downvote, save, etc...).
            commentContentLayout.setOnClickListener(v -> {
                if (rlCommentActions.getVisibility() == View.VISIBLE) {
                    hideActions(context, commentHolder, depth, localRepository.getAppTheme());
                } else {
                    showActions(context, commentHolder, depth, localRepository.getAppTheme());
                }
            });
            commentContentLayout.setOnLongClickListener(v -> {
                if (commentItem.getChildrenCollapsed()) {
                    commentUncollapses.onNext(comment.getId());
                    commentItem.setChildrenCollapsed(false);
                    collapseIndicator.setVisibility(View.GONE);
                } else {
                    commentCollapses.onNext(comment.getId());
                    commentItem.setChildrenCollapsed(true);
                    collapseIndicator.setVisibility(View.VISIBLE);
                }
                return true;
            });

            final int colorUpvoted = ContextCompat.getColor(context, R.color.commentUpvoted);
            final int colorDownvoted = ContextCompat.getColor(context, R.color.commentDownvoted);

            // Set score color base on vote.
            if (comment.getVote() == VoteDirection.UPVOTE) {
                scoreTextView.setTextColor(colorUpvoted);
            } else if (comment.getVote() == VoteDirection.DOWNVOTE) {
                scoreTextView.setTextColor(colorDownvoted);
            } else {
                scoreTextView.setTextColor(textColor);
            }

            // Add a "*" to indicate that a comment has been edited.
            if (comment.getEdited()) {
                timestampTextView.setText(timestamp + "*");
            }

            btnUpvote.setOnClickListener(v -> {
                if (scoreTextView.getCurrentTextColor() == colorUpvoted) {
                    if (!StringUtils.Companion.isNullOrEmpty(localRepository.getUsername())) {
                        scoreTextView.setTextColor(textColor);
                    }
                    novotes.onNext(comment);
                } else {
                    if (!StringUtils.Companion.isNullOrEmpty(localRepository.getUsername())) {
                        scoreTextView.setTextColor(colorUpvoted);
                    }
                    upvotes.onNext(comment);
                }
                hideActions(context, commentHolder, depth, localRepository.getAppTheme());
            });
            btnDownvote.setOnClickListener(v -> {
                if (scoreTextView.getCurrentTextColor() == colorDownvoted) {
                    if (!StringUtils.Companion.isNullOrEmpty(localRepository.getUsername())) {
                        scoreTextView.setTextColor(textColor);
                    }
                    novotes.onNext(comment);
                } else {
                    if (!StringUtils.Companion.isNullOrEmpty(localRepository.getUsername())) {
                        scoreTextView.setTextColor(colorDownvoted);
                    }
                    downvotes.onNext(comment);
                }
                hideActions(context, commentHolder, depth, localRepository.getAppTheme());
            });
            btnSave.setOnClickListener(v -> {
                if (comment.getSaved()) {
                    commentUnsaves.onNext(comment);
                    comment.setSaved(false);
                } else {
                    commentSaves.onNext(comment);
                    comment.setSaved(true);
                }
                hideActions(context, commentHolder, depth, localRepository.getAppTheme());
            });
            btnReply.setOnClickListener(v -> {
                replies.onNext(comment);
                hideActions(context, commentHolder, depth, localRepository.getAppTheme());
            });
        }

        private void hideActions(Context context, CommentViewHolder holder, int depth,
                                 SwishTheme theme) {
            holder.commentInnerContentLayout.setBackgroundColor(ContextCompat.getColor(context, R
                    .color.white));
            holder.rlCommentActions.setVisibility(View.GONE);
            setBackgroundAndPadding(context, depth, holder, false /* dark */, theme);
        }

        private void showActions(Context context, CommentViewHolder holder, int depth,
                                 SwishTheme theme) {
            holder.rlCommentActions.setVisibility(View.VISIBLE);
            setBackgroundAndPadding(context, depth, holder, true /* dark */, theme);
        }

        private void setBackgroundAndPadding(Context context, int depth,
                                             CommentViewHolder holder, boolean selected,
                                             SwishTheme theme) {
            int padding_in_dp = 5;
            final float scale = context.getResources().getDisplayMetrics().density;
            int padding_in_px = (int) (padding_in_dp * scale + 0.5F);

            // Add color if it is not a top-level comment.
            if (depth > 1) {
                int depthFromZero = depth - 2;
                int res = (depthFromZero) % 5;
                switch (res) {
                    case 0:
                        if (selected) {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_blue_selected_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_blue_selected_light);
                            }
                        } else {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_blue_normal_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_blue_normal_light);
                            }
                        }
                        break;
                    case 1:
                        if (selected) {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_green_selected_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_green_selected_light);
                            }
                        } else {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_green_normal_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_green_normal_light);
                            }
                        }
                        break;
                    case 2:
                        if (selected) {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_brown_selected_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_brown_selected_light);
                            }
                        } else {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_brown_normal_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_brown_normal_light);
                            }
                        }
                        break;
                    case 3:
                        if (selected) {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_orange_selected_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_orange_selected_light);
                            }
                        } else {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_orange_normal_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_orange_normal_light);
                            }
                        }
                        break;
                    case 4:
                        if (selected) {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_red_selected_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_red_selected_light);
                            }
                        } else {
                            if (theme == SwishTheme.DARK) {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_red_normal_dark);
                            } else {
                                holder.commentInnerContentLayout.setBackgroundResource(
                                        R.drawable.comment_border_red_normal_light);
                            }
                        }
                        break;
                }
            } else {
                if (selected) {
                    if (theme == SwishTheme.DARK) {
                        holder.commentInnerContentLayout.setBackgroundResource(
                                R.drawable.comment_no_border_selected_dark);
                    } else {
                        holder.commentInnerContentLayout.setBackgroundResource(
                                R.drawable.comment_no_border_selected_light);
                    }
                } else {
                    if (theme == SwishTheme.DARK) {
                        holder.commentInnerContentLayout.setBackgroundResource(
                                R.drawable.comment_no_border_normal_dark);
                    } else {
                        holder.commentInnerContentLayout.setBackgroundResource(
                                R.drawable.comment_no_border_normal_light);
                    }
                }
            }
            // Add padding depending on level.
            holder.commentContentLayout.setPadding(padding_in_px * (depth - 2), 0, 0, 0);

            int innerPadding = (int) UnitUtils.convertDpToPixel(10, itemView.getContext());
            holder.commentInnerContentLayout.setPadding(innerPadding, innerPadding, innerPadding,
                    innerPadding);
        }
    }
}
