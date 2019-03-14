package com.gmail.jorgegilcavazos.ballislife.features.submission;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.common.ThreadAdapter;
import com.gmail.jorgegilcavazos.ballislife.features.main.BaseNoActionBarActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentItem;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem;
import com.gmail.jorgegilcavazos.ballislife.features.reply.ReplyActivity;
import com.gmail.jorgegilcavazos.ballislife.features.videoplayer.VideoPlayerActivity;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.ThemeUtils;
import com.jakewharton.rxbinding2.view.RxView;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Submission;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;

public class SubmissionActivity extends BaseNoActionBarActivity implements
        SubmissionView,
        SwipeRefreshLayout.OnRefreshListener {
    public static final String KEY_TITLE = "Title";
    public static final String KEY_COMMENT_TO_SCROLL_ID = "CommentToScroll";

    @Inject SubmissionPresenter presenter;
    @Inject EventLogger eventLogger;
    @Inject PremiumService premiumService;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView_submission) RecyclerView submissionRecyclerView;

    private String threadId;
    private String title;

    private ThreadAdapter threadAdapter;
    private CommentSort sorting = CommentSort.TOP;

    @Override
    public void injectAppComponent() {
        BallIsLifeApplication.getAppComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setSubtitle("TOP");
        }
        setToolbarPopupTheme(toolbar);

        Bundle extras = getIntent().getExtras();
        if (getIntent() != null && getIntent().getExtras() != null) {
            threadId = extras.getString(Constants.THREAD_ID);
            title = extras.getString(KEY_TITLE);
        }

        setTitle(title);

        swipeRefreshLayout.setOnRefreshListener(this);

        int textColor = ThemeUtils.Companion.getTextColor(this, localRepository.getAppTheme());
        threadAdapter = new ThreadAdapter(this, premiumService, localRepository,
                new ArrayList<>(), true, textColor);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        submissionRecyclerView.setLayoutManager(linearLayoutManager);
        submissionRecyclerView.setAdapter(threadAdapter);
        submissionRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fab.hide();
                } else if (dy < 0) {
                    fab.show();
                }
            }
        });

        presenter.attachView(this);
        presenter.loadComments(threadId, sorting, false /* forceReload */);
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventLogger.setCurrentScreen(this, SwishScreen.SUBMISSION);
    }

    @Override
    protected void onDestroy() {
        presenter.detachView();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_submission, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_sort_hot:
                sorting = CommentSort.HOT;
                presenter.loadComments(threadId, sorting, true /* forceReload */);
                getSupportActionBar().setSubtitle("HOT");
                return true;
            case R.id.action_sort_new:
                sorting = CommentSort.NEW;
                presenter.loadComments(threadId, sorting, true /* forceReload */);
                getSupportActionBar().setSubtitle("NEW");
                return true;
            case R.id.action_sort_old:
                sorting = CommentSort.OLD;
                presenter.loadComments(threadId, sorting, true /* forceReload */);
                getSupportActionBar().setSubtitle("OLD");
                return true;
            case R.id.action_sort_controversial:
                sorting = CommentSort.CONTROVERSIAL;
                presenter.loadComments(threadId, sorting, true /* forceReload */);
                getSupportActionBar().setSubtitle("CONTROVERSIAL");
                return true;
            case R.id.action_sort_top:
                sorting = CommentSort.TOP;
                presenter.loadComments(threadId, sorting, true /* forceReload */);
                getSupportActionBar().setSubtitle("TOP");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ReplyActivity.POST_COMMENT_REPLY_REQUEST && resultCode ==
                RESULT_OK) {
            String parentId = data.getStringExtra(ReplyActivity.KEY_COMMENT_ID);
            String response = data.getStringExtra(ReplyActivity.KEY_POSTED_COMMENT);
            presenter.replyToComment(parentId, response);
        } else if (requestCode == ReplyActivity.POST_SUBMISSION_REPLY_REQUEST && resultCode ==
                RESULT_OK) {
            String response = data.getStringExtra(ReplyActivity.KEY_POSTED_COMMENT);
            String submissionId = data.getStringExtra(ReplyActivity.KEY_SUBMISSION_ID);
            presenter.replyToSubmission(submissionId, response);
        }
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentSaves() {
        return threadAdapter.getCommentSaves();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentUnsaves() {
        return threadAdapter.getCommentUnsaves();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentUpvotes() {
        return threadAdapter.getUpvotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentDownvotes() {
        return threadAdapter.getDownvotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentNovotes() {
        return threadAdapter.getNovotes();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionShares() {
        return threadAdapter.getSubmissionShares();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionSaves() {
        return threadAdapter.getSubmissionSaves();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionUnsaves() {
        return threadAdapter.getSubmissionUnsaves();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionUpvotes() {
        return threadAdapter.getSubmissionUpvotes();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionDownvotes() {
        return threadAdapter.getSubmissionDownvotes();
    }

    @NotNull
    @Override
    public Observable<Submission> submissionNovotes() {
        return threadAdapter.getSubmissionNovotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> commentReplies() {
        return threadAdapter.getReplies();
    }

    @NotNull
    @Override
    public Observable<Object> submissionReplies() {
        return RxView.clicks(fab);
    }

    @NotNull
    @Override
    public Observable<String> submissionContentClicks() {
        return threadAdapter.getSubmissionContentClicks();
    }

    @NotNull
    @Override
    public Observable<String> commentCollapses() {
        return threadAdapter.getCommentCollapses();
    }

    @NotNull
    @Override
    public Observable<String> commentUnCollapses() {
        return threadAdapter.getCommentUnCollapses();
    }

    @NotNull
    @Override
    public Observable<CommentItem> loadMoreComments() {
        return threadAdapter.getLoadMoreComments();
    }


    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showComments(@NonNull List<ThreadItem> commentNodes, @NonNull Submission
            submission) {
        threadAdapter.setSubmission(submission);
        threadAdapter.setData(commentNodes);
    }

    @Override
    public void addCommentItem(@NotNull CommentItem commentItem, @NotNull String parentId) {
        threadAdapter.addCommentItem(commentItem, parentId);
    }

    @Override
    public void addCommentItem(@NotNull CommentItem commentItem) {
        threadAdapter.addCommentItem(commentItem);
        submissionRecyclerView.smoothScrollToPosition(0);
    }

    @Override
    public void showSubmittingCommentToast() {
        Toast.makeText(this, R.string.submitting_comment, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showErrorAddingComment() {
        Toast.makeText(this, R.string.saving_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNotLoggedInError() {
        Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSavedCommentToast() {
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUnsavedCommentToast() {
        Toast.makeText(this, R.string.unsaved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openReplyToCommentActivity(@NonNull final Comment parentComment) {
        Intent intent = new Intent(SubmissionActivity.this, ReplyActivity.class);
        Bundle extras = new Bundle();
        extras.putString(ReplyActivity.KEY_COMMENT_ID, parentComment.getId());
        extras.putCharSequence(ReplyActivity.KEY_COMMENT, RedditUtils.bindSnuDown(parentComment
                .data("body_html")));
        intent.putExtras(extras);
        startActivityForResult(intent, ReplyActivity.POST_COMMENT_REPLY_REQUEST);
    }

    @Override
    public void openReplyToSubmissionActivity(@NonNull String submissionId) {
        Intent intent = new Intent(SubmissionActivity.this, ReplyActivity.class);
        Bundle extras = new Bundle();
        extras.putString(ReplyActivity.KEY_SUBMISSION_ID, submissionId);
        intent.putExtras(extras);
        startActivityForResult(intent, ReplyActivity.POST_SUBMISSION_REPLY_REQUEST);
    }

    @Override
    public void openContentTab(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }

    @Override
    public void openStreamable(String shortcode) {
        Intent intent = new Intent(SubmissionActivity.this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.SHORTCODE, shortcode);
        startActivity(intent);
    }

    @Override
    public void showContentUnavailableToast() {
        Toast.makeText(this, R.string.content_not_available, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void scrollToComment(int index) {
        // Comment i is actually at index i + 1 because of the post card view.
        submissionRecyclerView.scrollToPosition(index + 1);
    }

    @Override
    public void hideFab() {
        fab.hide();
    }

    @Override
    public void showFab() {
        fab.show();
    }

    @Override
    public void collapseComments(@NotNull String id) {
        threadAdapter.collapseComments(id);
    }

    @Override
    public void uncollapseComments(@NotNull String id) {
        threadAdapter.unCollapseComments(id);
    }

    @Override
    public void onRefresh() {
        presenter.loadComments(threadId, sorting, true /* forceReload */);
    }

    @Override
    public void insertItemsBelowParent(@NotNull List<ThreadItem> threadItems, @NotNull
            CommentNode parentNode) {
        threadAdapter.insertItemsBelowParent(threadItems, parentNode);
    }

    @Override
    public void showErrorLoadingMoreComments() {
        Toast.makeText(this, "Error loading more comments", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNoNetAvailable() {
        Toast.makeText(this, R.string.your_device_is_offline, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void share(@NonNull String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_this_link)));
    }
}
