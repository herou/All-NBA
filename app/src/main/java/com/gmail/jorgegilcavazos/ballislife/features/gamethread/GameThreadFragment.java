package com.gmail.jorgegilcavazos.ballislife.features.gamethread;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.common.ThreadAdapter;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentDelay;
import com.gmail.jorgegilcavazos.ballislife.features.model.CommentWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameThreadType;
import com.gmail.jorgegilcavazos.ballislife.features.model.ThreadItem;
import com.gmail.jorgegilcavazos.ballislife.features.reply.ReplyActivity;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.ThemeUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.dean.jraw.models.Comment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static android.app.Activity.RESULT_OK;

public class GameThreadFragment extends Fragment implements GameThreadView,
        SwipeRefreshLayout.OnRefreshListener {

    public static final String THREAD_TYPE_KEY = "THREAD_TYPE";
    public static final String GAME_DATE_KEY = "GAME_DATE";

    @Inject GameThreadPresenterV2 presenter;
    @Inject LocalRepository localRepository;
    @Inject PremiumService premiumService;
    @Inject EventLogger eventLogger;

    @BindView(R.id.game_thread_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.comment_thread_rv) RecyclerView rvComments;
    @BindView(R.id.noThreadText) TextView noThreadText;
    @BindView(R.id.noCommentsText) TextView noCommentsText;
    @BindView(R.id.errorLoadingText) TextView errorLoadingText;
    @BindView(R.id.adView) AdView adView;

    private PublishSubject<Object> fabClicks = PublishSubject.create();

    private RecyclerView.LayoutManager lmComments;
    private ThreadAdapter threadAdapter;
    private Unbinder unbinder;
    private String homeTeam, awayTeam;
    private GameThreadType threadType;
    private String gameId;
    private long gameDate;

    public GameThreadFragment() {
        // Required empty public constructor.
    }

    public static GameThreadFragment newInstance() {
        return new GameThreadFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ReplyActivity.POST_COMMENT_REPLY_REQUEST && resultCode == RESULT_OK) {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        BallIsLifeApplication.getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            homeTeam = getArguments().getString(CommentsActivity.HOME_TEAM_KEY);
            awayTeam = getArguments().getString(CommentsActivity.AWAY_TEAM_KEY);
            threadType = (GameThreadType) getArguments().getSerializable(THREAD_TYPE_KEY);
            gameId = getArguments().getString(CommentsActivity.GAME_ID_KEY);
            gameDate = getArguments().getLong(GAME_DATE_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_thread, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);

        int textColor = ThemeUtils.Companion.getTextColor(getActivity(), localRepository
                .getAppTheme());
        threadAdapter = new ThreadAdapter(getActivity(), premiumService, localRepository,
                new ArrayList<>(), false, textColor);

        lmComments = new LinearLayoutManager(getActivity());
        rvComments.setLayoutManager(lmComments);
        rvComments.setAdapter(threadAdapter);
        rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    ((CommentsActivity) getActivity()).getFab().hide();
                } else if (dy < 0) {
                    ((CommentsActivity) getActivity()).getFab().show();
                }
            }
        });

        presenter.attachView(this);
        presenter.loadGameThread(localRepository.isGameThreadStreamingEnabled());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (premiumService.isPremium() || localRepository.isGameStreamUnlocked(gameId)) {
            adView.setVisibility(View.GONE);
        } else {
            adView.loadAd(new AdRequest.Builder().build());
            adView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if the user is premium or if the game is unlocked and hide ads if either is
        // true, but don't load the ad again if false. This is necessary for when the user
        // watches a rewarded video or purchases premium in a different activity and returns to
        // this screen.
        if (premiumService.isPremium() || localRepository.isGameStreamUnlocked(gameId)) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && null != presenter) {
            presenter.onVisible();
        }
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        presenter.detachView();
        super.onDestroyView();
    }

    @Override
    public void onRefresh() {
        presenter.loadGameThread(localRepository.isGameThreadStreamingEnabled());
    }

    @NonNull
    @Override
    public GameThreadType getThreadType() {
        return threadType;
    }

    @NonNull
    @Override
    public String getHome() {
        return homeTeam;
    }

    @NonNull
    @Override
    public String getVisitor() {
        return awayTeam;
    }

    @Override
    public long getGameTimeUtc() {
        return gameDate;
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showComments(List<ThreadItem> comments) {
        threadAdapter.setData(comments);
        rvComments.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideComments() {
        rvComments.setVisibility(View.GONE);
    }

    @Override
    public void showNoThreadText() {
        noThreadText.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideNoThreadText() {
        noThreadText.setVisibility(View.GONE);
    }

    @Override
    public void showNoCommentsText() {
        noCommentsText.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideNoCommentsText() {
        noCommentsText.setVisibility(View.GONE);
    }

    @Override
    public void showErrorLoadingText(int code) {
        errorLoadingText.setText(getString(R.string.error_loading_comments, code));
        errorLoadingText.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideErrorLoadingText() {
        errorLoadingText.setVisibility(View.GONE);
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
    public Observable<CommentWrapper> upvotes() {
        return threadAdapter.getUpvotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> downvotes() {
        return threadAdapter.getDownvotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> novotes() {
        return threadAdapter.getNovotes();
    }

    @NotNull
    @Override
    public Observable<CommentWrapper> replies() {
        return threadAdapter.getReplies();
    }

    @NotNull
    @Override
    public Observable<Object> submissionReplies() {
        return fabClicks;
    }

    @Override
    public void openReplyToCommentActivity(@NonNull Comment parentComment) {
        Intent intent = new Intent(getActivity(), ReplyActivity.class);
        Bundle extras = new Bundle();
        extras.putString(ReplyActivity.KEY_COMMENT_ID, parentComment.getId());
        extras.putCharSequence(ReplyActivity.KEY_COMMENT, RedditUtils.bindSnuDown(parentComment
                .data("body_html")));
        intent.putExtras(extras);
        startActivityForResult(intent, ReplyActivity.POST_COMMENT_REPLY_REQUEST);
    }

    @Override
    public void openReplyToSubmissionActivity(@NonNull String submissionId) {
        Intent intent = new Intent(getActivity(), ReplyActivity.class);
        Bundle extras = new Bundle();
        extras.putString(ReplyActivity.KEY_SUBMISSION_ID, submissionId);
        intent.putExtras(extras);
        startActivityForResult(intent, ReplyActivity.POST_SUBMISSION_REPLY_REQUEST);
    }

    @Override
    public void showSavingToast() {
        Toast.makeText(getActivity(), R.string.saving, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSavedToast() {
        Toast.makeText(getActivity(), R.string.saved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUnsavingToast() {
        Toast.makeText(getActivity(), R.string.unsaving, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUnsavedToast() {
        Toast.makeText(getActivity(), R.string.unsaved, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSubmittingCommentToast() {
        Toast.makeText(getActivity(), R.string.submitting_comment, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSubmittedCommentToast() {
        Toast.makeText(getActivity(), R.string.submitted_comment, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showMissingParentToast() {
        Toast.makeText(getActivity(), R.string.could_not_save_comment_missing_parent, Toast
                .LENGTH_SHORT).show();
    }

    @Override
    public void showMissingSubmissionToast() {
        Toast.makeText(getActivity(), R.string.could_not_save_comment_missing_submmission, Toast
                .LENGTH_SHORT).show();
    }

    @Override
    public void showErrorSavingCommentToast(int code) {
        Toast.makeText(getActivity(), getString(R.string.something_went_wrong, code), Toast
                .LENGTH_SHORT).show();
    }

    @Override
    public void showNotLoggedInToast() {
        Toast.makeText(getActivity(), R.string.not_logged_in, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNoNetAvailableText() {
        errorLoadingText.setText(R.string.your_device_is_offline);
        errorLoadingText.setVisibility(View.VISIBLE);
    }

    @Override
    public void showNoNetAvailableToast() {
        Toast.makeText(getActivity(), R.string.your_device_is_offline, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showFab() {
        ((CommentsActivity) getActivity()).showFab();
    }

    @Override
    public void hideFab() {
        ((CommentsActivity) getActivity()).hideFab();
    }


    public void fabClicked() {
        fabClicks.onNext(new Object());
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

    @Override
    public void collapseComments(@NotNull String id) {
        threadAdapter.collapseComments(id);
    }

    @Override
    public void uncollapseComments(@NotNull String id) {
        threadAdapter.unCollapseComments(id);
    }

    @NotNull
    @Override
    public CommentDelay getCommentDelay() {
        return ((CommentsActivity) getActivity()).getCommentDelay();
    }
}
