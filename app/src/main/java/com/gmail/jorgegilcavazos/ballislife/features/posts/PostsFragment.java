package com.gmail.jorgegilcavazos.ballislife.features.posts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.data.repository.posts.PostsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.common.EndlessRecyclerViewScrollListener;
import com.gmail.jorgegilcavazos.ballislife.features.common.OnSubmissionClickListener;
import com.gmail.jorgegilcavazos.ballislife.features.model.NBASubChips;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity;
import com.gmail.jorgegilcavazos.ballislife.features.videoplayer.VideoPlayerActivity;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.ThemeUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PostsFragment extends Fragment implements PostsView,
        SwipeRefreshLayout.OnRefreshListener, OnSubmissionClickListener {

    private static final String TAG = "PostsFragment";
    private static final String SUBREDDIT = "subreddit";
    private static final String LIST_STATE = "listState";

    @Inject LocalRepository localRepository;
    @Inject
    @Named("redditSharedPreferences") SharedPreferences redditSharedPreferences;
    @Inject PostsRepository postsRepository;
    @Inject PostsPresenter presenter;
    @Inject RedditAuthentication redditAuthentication;
    @Inject EventLogger eventLogger;
    @Inject PremiumService premiumService;

    @BindView(R.id.posts_content) View postsContainer;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView_posts) RecyclerView recyclerViewPosts;
    @BindView(R.id.adView) AdView adView;

    Parcelable listState;
    private int viewType;
    private String subreddit;
    private Snackbar snackbar;
    private PostsAdapter postsAdapter;
    private LinearLayoutManager linearLayoutManager;
    private EndlessRecyclerViewScrollListener scrollListener;
    private Unbinder unbinder;
    private Sorting sorting = Sorting.HOT;
    private TimePeriod timePeriod = TimePeriod.ALL;
    private Menu menu;

    public PostsFragment() {
        BallIsLifeApplication.getAppComponent().inject(this);
    }

    public static PostsFragment newInstance(String subreddit) {
        PostsFragment fragment = new PostsFragment();
        Bundle args = new Bundle();
        args.putString(SUBREDDIT, subreddit);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            subreddit = getArguments().getString(SUBREDDIT);
        }

        if (localRepository.getFavoritePostsViewType() != -1) {
            viewType = localRepository.getFavoritePostsViewType();
        } else {
            viewType = Constants.POSTS_VIEW_WIDE_CARD;
        }

        int textColor = ThemeUtils.Companion.getTextColor(getActivity(), localRepository
                .getAppTheme());
        linearLayoutManager = new LinearLayoutManager(getActivity());
        postsAdapter = new PostsAdapter(getActivity(), redditAuthentication, null, viewType,
                this, subreddit, textColor, localRepository.getAppTheme());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerViewPosts.setLayoutManager(linearLayoutManager);
        recyclerViewPosts.setAdapter(postsAdapter);

        // Pass 1 as staticItemCount because of the posts header.
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager,
                1 /* staticItemCount */) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                presenter.loadPosts(false /* reset */);
            }
        };

        recyclerViewPosts.addOnScrollListener(scrollListener);

        presenter.setSubreddit(subreddit);
        presenter.attachView(this);
        presenter.loadSubscriberCount();
        presenter.subscribeToSubmissionShare(postsAdapter.getShareObservable());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (premiumService.isPremium()) {
            adView.setVisibility(View.GONE);
        } else {
            adView.loadAd(new AdRequest.Builder().build());
            adView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        eventLogger.setCurrentScreen(getActivity(), SwishScreen.POSTS);
        // Load cached data if available, or from network if not.
        presenter.loadFirstAvailable(sorting, timePeriod);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save layout manager state to restore scroll position after config changes.
        listState = linearLayoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, listState);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        presenter.detachView();
        presenter.stop();
        dismissSnackbar();
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_posts, menu);
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);

        setViewIcon(viewType);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                presenter.loadSubscriberCount();
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                return true;
            case R.id.action_change_view:
                openViewPickerDialog();
                return true;
            case R.id.action_sort_hot:
                sorting = Sorting.HOT;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("HOT");
                return true;
            case R.id.action_sort_new:
                sorting = Sorting.NEW;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("NEW");
                return true;
            case R.id.action_sort_rising:
                sorting = Sorting.RISING;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("RISING");
                return true;
            case R.id.action_sort_controversial:
                sorting = Sorting.CONTROVERSIAL;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("CONTROVERSIAL");
                return true;
            case R.id.action_sort_top_hour:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.HOUR;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: HOUR");
                return true;
            case R.id.action_sort_top_day:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.DAY;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: DAY");
                return true;
            case R.id.action_sort_top_week:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.WEEK;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: WEEK");
                return true;
            case R.id.action_sort_top_month:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.MONTH;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: MONTH");
                return true;
            case R.id.action_sort_top_year:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.YEAR;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: YEAR");
                return true;
            case R.id.action_sort_top_all:
                sorting = Sorting.TOP;
                timePeriod = TimePeriod.ALL;
                presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle("TOP: ALL");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        presenter.loadSubscriberCount();
        presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showPosts(List<SubmissionWrapper> submissions, boolean clear) {
        if (clear) {
            postsAdapter.setData(submissions);
        } else {
            postsAdapter.addData(submissions);
        }

        // We're coming from a config change, so the state needs to be restored.
        if (listState != null) {
            linearLayoutManager.onRestoreInstanceState(listState);
            listState = null;
        }
    }

    @Override
    public void showPostsLoadingFailedSnackbar(final boolean reset) {
        if (getView() != null) {
            snackbar = Snackbar.make(getView(), R.string.posts_loading_failed,
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.retry, v -> {
                if (reset) {
                    presenter.resetPaginatorThenLoadPosts(sorting, timePeriod);
                } else {
                    presenter.loadPosts(false);
                }
            });
            snackbar.show();
        }
    }

    @Override
    public void dismissSnackbar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    @Override
    public void showNotAuthenticatedToast() {
        Toast.makeText(getActivity(), R.string.not_authenticated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNotLoggedInToast() {
        Toast.makeText(getActivity(), R.string.not_logged_in, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showSubscribers(SubscriberCount subscriberCount) {
        postsAdapter.setSubscriberCount(subscriberCount);
    }

    @Override
    public void openContentTab(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(getActivity(), Uri.parse(url));
    }

    @Override
    public void showNothingToShowToast() {
        Toast.makeText(getActivity(), R.string.nothing_to_show, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openStreamable(String shortcode) {
        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.SHORTCODE, shortcode);
        startActivity(intent);
    }

    @Override
    public void showContentUnavailableToast() {
        Toast.makeText(getActivity(), R.string.content_not_available, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void changeViewType(int viewType) {
        postsAdapter.setContentViewType(viewType);
        setViewIcon(viewType);
    }

    @Override
    public void scrollToTop() {
        recyclerViewPosts.smoothScrollToPosition(0);
    }

    @Override
    public void resetScrollState() {
        scrollListener.resetState();
    }

    @Override
    public void share(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_this_link)));
    }

    @Override
    public void showUnknownErrorToast() {
        Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubmissionClick(String submissionId) {
        Intent intent = new Intent(getActivity(), SubmissionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.THREAD_ID, submissionId);
        bundle.putString(SubmissionActivity.KEY_TITLE, subreddit);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onVoteSubmission(SubmissionWrapper submissionWrapper, VoteDirection voteDirection) {
        presenter.onVote(submissionWrapper.getSubmission(), voteDirection);
    }

    @Override
    public void onSaveSubmission(SubmissionWrapper submissionWrapper, boolean saved) {
        presenter.onSave(submissionWrapper.getSubmission(), saved);
    }

    @Override
    public void onContentClick(String url) {
        presenter.onContentClick(url);
    }

    @Override
    public void onHideSubmission(SubmissionWrapper submission) {
        int index = postsAdapter.getIndexOfSubmission(submission);
        presenter.onHide(submission, index, true);
    }

    @Override
    public void setNbaSubChips(NBASubChips nbaSubChips) {
        postsAdapter.setNBASubChips(nbaSubChips);
    }

    @Override
    public void hideSubmission(SubmissionWrapper submission, int index) {
        postsAdapter.removePost(submission);
    }

    @Override
    public void showHideSubmissionSnackbar(SubmissionWrapper submission, int index) {
        Snackbar snackbar = Snackbar.make(postsContainer,
                R.string.submission_hidden,
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo_string, view -> presenter.onHide(submission, index, false));
        snackbar.show();
    }

    @Override
    public void unHideSubmission(SubmissionWrapper submission, int index) {
        postsAdapter.addSubmissionAtIndex(submission, index);
    }

    private void openViewPickerDialog() {
        final MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.change_view)
                .customView(R.layout.view_picker_layout, false)
                .build();

        View view = materialDialog.getCustomView();
        if (view == null) return;

        View viewTypeCard = view.findViewById(R.id.layout_type_card);
        View viewTypeList = view.findViewById(R.id.layout_type_list);

        viewTypeCard.setOnClickListener(v -> {
            presenter.onViewTypeSelected(Constants.POSTS_VIEW_WIDE_CARD);
            materialDialog.dismiss();
        });

        viewTypeList.setOnClickListener(v -> {
            presenter.onViewTypeSelected(Constants.POSTS_VIEW_LIST);
            materialDialog.dismiss();
        });

        materialDialog.show();
    }

    private void setViewIcon(int viewType) {
        Drawable drawable;
        switch (viewType) {
            case Constants.POSTS_VIEW_WIDE_CARD:
                drawable = VectorDrawableCompat.create(getContext().getResources(),
                                                       R.drawable.ic_image_white_24dp,
                                                       null);
                break;
            case Constants.POSTS_VIEW_LIST:
                drawable = VectorDrawableCompat.create(getContext().getResources(),
                                                       R.drawable.ic_view_list_white_24dp,
                                                       null);
                break;
            default:
                drawable = VectorDrawableCompat.create(getContext().getResources(),
                                                       R.drawable.ic_image_white_24dp,
                                                       null);
                break;
        }
        menu.findItem(R.id.action_change_view).setIcon(drawable);
    }
}
