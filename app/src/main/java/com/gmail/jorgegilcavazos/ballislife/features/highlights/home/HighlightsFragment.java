package com.gmail.jorgegilcavazos.ballislife.features.highlights.home;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.GoPremiumOrigin;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEvent;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishEventParam;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.data.repository.highlights.HighlightsRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.common.EndlessRecyclerViewScrollListener;
import com.gmail.jorgegilcavazos.ballislife.features.gopremium.GoPremiumActivity;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.HighlightAdapterV2;
import com.gmail.jorgegilcavazos.ballislife.features.model.Highlight;
import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard;
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity;
import com.gmail.jorgegilcavazos.ballislife.features.videoplayer.VideoPlayerActivity;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;

public class HighlightsFragment extends Fragment implements HighlightsView,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String LIST_STATE = "listState";

    @Inject LocalRepository localRepository;
    @Inject HighlightsRepository highlightsRepository;
    @Inject BaseSchedulerProvider schedulerProvider;
    @Inject HighlightsPresenter presenter;
    @Inject PremiumService premiumService;
    @Inject EventLogger eventLogger;

    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.recyclerView_highlights) RecyclerView rvHighlights;

    Parcelable listState;
    private HighlightViewType viewType;
    private Unbinder unbinder;
    private HighlightAdapterV2 highlightAdapter;
    private LinearLayoutManager linearLayoutManager;
    private EndlessRecyclerViewScrollListener scrollListener;
    private Menu menu;
    private Snackbar snackbar;
    private Sorting sorting = Sorting.NEW;

    public HighlightsFragment() {
        // Required empty public constructor.
    }

    public static HighlightsFragment newInstance() {
        return new HighlightsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BallIsLifeApplication.getAppComponent().inject(this);

        viewType = localRepository.getFavoriteHighlightViewType();

        linearLayoutManager = new LinearLayoutManager(getActivity());

        // Only of of the three showCard parameters should be true.
        highlightAdapter = new HighlightAdapterV2(
                getActivity(),
                new ArrayList<>(25),
                viewType,
                isPremium(),
                shouldShowSortingCard(),
                false /* showSwishFavoritesCard */,
                false /* showAddFavoritesCard */);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_highlights, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);

        rvHighlights.setLayoutManager(linearLayoutManager);
        rvHighlights.setAdapter(highlightAdapter);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                presenter.loadHighlights(false /* reset */);
            }
        };

        rvHighlights.addOnScrollListener(scrollListener);

        presenter.setItemsToLoad(10);
        presenter.attachView(this);
        presenter.subscribeToHighlightsClick(highlightAdapter.getViewClickObservable());
        presenter.subscribeToHighlightsShare(highlightAdapter.getShareClickObservable());
        presenter.subscribeToFavoriteClick(highlightAdapter.getFavoriteClicks());
        presenter.subscribeToSubmissionClick(highlightAdapter.getSubmissionClickObservable());

        return view;
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
        // Load cached data if available, or from network if not.
        presenter.loadFirstAvailable();
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
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_highlights, menu);
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);

        setViewIcon(viewType);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                presenter.loadHighlights(true);
                return true;
            case R.id.action_change_view:
                openViewPickerDialog();
                return true;
            case R.id.action_sort_new:
                sorting = Sorting.NEW;
                presenter.loadHighlights(true);
                return true;
            case R.id.action_sort_top_day:
                if (!isPremium()) {
                    logGoPremiumFromSorting();
                    openPremiumActivity();
                    return true;
                }
                sorting = Sorting.TOP_DAY;
                presenter.loadHighlights(true);
                return true;
            case R.id.action_sort_top_week:
                if (!isPremium()) {
                    logGoPremiumFromSorting();
                    openPremiumActivity();
                    return true;
                }
                sorting = Sorting.TOP_WEEK;
                presenter.loadHighlights(true);
                return true;
            case R.id.action_sort_top_season:
                if (!isPremium()) {
                    logGoPremiumFromSorting();
                    openPremiumActivity();
                    return true;
                }
                sorting = Sorting.TOP_SEASON;
                presenter.loadHighlights(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        presenter.loadHighlights(true);
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void hideHighlights() {
        rvHighlights.setVisibility(View.GONE);
    }

    @Override
    public void showHighlights(List<Highlight> highlights, boolean clear) {
        rvHighlights.setVisibility(View.VISIBLE);
        if (clear) {
            highlightAdapter.setData(highlights);
        } else {
            highlightAdapter.addData(highlights);
        }

        // We're coming from a config change, so the state needs to be restored.
        if (listState != null) {
            linearLayoutManager.onRestoreInstanceState(listState);
            listState = null;
        }
    }

    @Override
    public void showNoHighlightsAvailable() {
        Toast.makeText(getActivity(), R.string.no_highlights_to_show, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNoNetAvailable() {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(getView(),
                                 R.string.your_device_is_offline,
                                 Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    @Override
    public void showErrorLoadingHighlights(int code) {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(getView(),
                                 getString(R.string.error_loading_highlights, code),
                                 Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void openStreamable(String shortcode) {
        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.SHORTCODE, shortcode);
        startActivity(intent);
    }

    @Override
    public void showErrorOpeningStreamable() {
        Toast.makeText(getActivity(), R.string.error_loading_streamable, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void openYoutubeVideo(String videoId) {
        Intent intent;
        // Verify that the API is available in the device.
        if (localRepository.getOpenYouTubeInApp()
                && YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(getActivity())
                .equals(YouTubeInitializationResult.SUCCESS)) {
            Crashlytics.log(Log.INFO, "HighlightsFragment", "Opening youtube video in " +
                    "app: " + videoId);
            intent = YouTubeStandalonePlayer.createVideoIntent(getActivity(),
                    "AIzaSyA3jvG_4EIhAH_l3criaJx7-E_XWixOe78", /* API KEY */
                    videoId, 0, /* Start millisecond */
                    true /* Autoplay */, true /* Lightbox */);
            startActivity(intent);
        } else {
            Crashlytics.log(Log.INFO, "HighlightsFragment", "Opening youtube video in " +
                    "YouTube: " + videoId);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
            startActivity(intent);
        }
    }

    @Override
    public void showErrorOpeningYoutube() {
        Toast.makeText(getActivity(), R.string.error_loading_youtube, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showUnknownSourceError() {
        Toast.makeText(getActivity(), R.string.unknown_source, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void resetScrollState() {
        scrollListener.resetState();
    }

    @Override
    public void shareHighlight(Highlight highlight) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, highlight.getUrl());
        startActivity(Intent.createChooser(shareIntent,
                getResources().getString(R.string.share_video)));
    }

    @Override
    public void changeViewType(HighlightViewType viewType) {
        highlightAdapter.setContentViewType(viewType);
        setViewIcon(viewType);
    }

    @Override
    public void hideSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    @Override
    public void onSubmissionClick(Highlight highlight) {
        Intent intent = new Intent(getActivity(), SubmissionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.THREAD_ID, highlight.getId());
        bundle.putString(SubmissionActivity.KEY_TITLE, getString(R.string.highlights));
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public Sorting getSorting() {
        return sorting;
    }

    @Override
    public Observable<SwishCard> explorePremiumClicks() {
        return highlightAdapter.getExplorePremiumClicks();
    }

    @Override
    public Observable<SwishCard> gotItClicks() {
        return highlightAdapter.getGotItClicks();
    }

    @Override
    public void openPremiumActivity() {
        Intent intent = new Intent(getActivity(), GoPremiumActivity.class);
        startActivity(intent);
    }

    @Override
    public void dismissSwishCard(SwishCard swishCard) {
        highlightAdapter.removeSwishCard(swishCard);
    }

    @Override
    public void showAddingToFavoritesMsg() {
        Toast.makeText(getActivity(), R.string.add_fav_progress, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showAddedToFavoritesMsg() {
        Toast.makeText(getActivity(), R.string.add_fav_success, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showAddToFavoritesFailed() {
        Toast.makeText(getActivity(), R.string.add_fav_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showMustLogInToFavoriteMsg() {
        Toast.makeText(getActivity(), R.string.favorite_must_log_in, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void logExplorePremiumFromSorting() {
        Bundle params = new Bundle();
        params.putString(SwishEventParam.GO_PREMIUM_ORIGIN.getKey(),
                GoPremiumOrigin.HIGHLIGHTS_SORTING_EXPLORE_CARD.getOriginName());
        eventLogger.logEvent(SwishEvent.GO_PREMIUM, params);
    }

    public void logGoPremiumFromSorting() {
        Bundle params = new Bundle();
        params.putString(SwishEventParam.GO_PREMIUM_ORIGIN.getKey(),
                GoPremiumOrigin.HIGHLIGHTS_SORTING.getOriginName());
        eventLogger.logEvent(SwishEvent.GO_PREMIUM, params);
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
            presenter.onViewTypeSelected(HighlightViewType.LARGE);
            materialDialog.dismiss();
        });

        viewTypeList.setOnClickListener(v -> {
            presenter.onViewTypeSelected(HighlightViewType.SMALL);
            materialDialog.dismiss();
        });

        materialDialog.show();
    }

    private void setViewIcon(HighlightViewType viewType) {
        Drawable drawable;
        switch (viewType) {
            case LARGE:
                drawable = VectorDrawableCompat.create(getContext().getResources(),
                                                       R.drawable.ic_image_white_24dp,
                                                       null);
                break;
            case SMALL:
                drawable = VectorDrawableCompat.create(getContext().getResources(),
                                                       R.drawable.ic_view_list_white_24dp,
                                                       null);
                break;
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
        menu.findItem(R.id.action_change_view).setIcon(drawable);
    }

    private boolean isPremium() {
        return premiumService.isPremium();
    }

    private boolean shouldShowSortingCard() {
        boolean sortingCardSeen = localRepository.swishCardSeen(SwishCard.HIGHLIGHT_SORTING);
        return !sortingCardSeen && !isPremium();
    }
}
