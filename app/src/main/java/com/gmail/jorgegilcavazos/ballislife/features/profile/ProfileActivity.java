package com.gmail.jorgegilcavazos.ballislife.features.profile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.common.EndlessRecyclerViewScrollListener;
import com.gmail.jorgegilcavazos.ballislife.features.main.BaseNoActionBarActivity;
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProfileActivity extends BaseNoActionBarActivity
        implements ProfileView, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "ProfileActivity";

    private static final String LIST_STATE = "listState";

    @BindView(R.id.profile_coordinator_layout) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.profile_toolbar) Toolbar toolbar;
    @BindView(R.id.profile_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.profile_recycler_view) RecyclerView recyclerView;

    @Inject ProfilePresenter presenter;
    @Inject EventLogger eventLogger;

    private LinearLayoutManager linearLayoutManager;
    private EndlessRecyclerViewScrollListener scrollListener;
    private ContributionsAdapter contributionsAdapter;
    private Snackbar snackbar;
    private Parcelable listState;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        listState = savedInstanceState.getParcelable(LIST_STATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        listState = linearLayoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, listState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh:
                presenter.loadContributions(true /* reset */);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void injectAppComponent() {
        BallIsLifeApplication.getAppComponent().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.profile);

        swipeRefreshLayout.setOnRefreshListener(this);

        linearLayoutManager = new LinearLayoutManager(this);
        contributionsAdapter = new ContributionsAdapter(this, new ArrayList<Contribution>());

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(contributionsAdapter);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                presenter.loadContributions(false /* reset */);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);

        presenter.setLimit(20);
        presenter.setSorting(Sorting.NEW);
        presenter.setTimePeriod(TimePeriod.ALL);
        presenter.attachView(this);
        presenter.observeContributionsClicks(contributionsAdapter.getClickObservable());
    }

    @Override
    protected void onDestroy() {
        presenter.stop();
        presenter.detachView();
        dismissSnackbar();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        eventLogger.setCurrentScreen(this, SwishScreen.PROFILE);
        // Load cached data if available, from network if not.
        presenter.loadFirstAvailable();
    }

    @Override
    public void onRefresh() {
        presenter.loadContributions(true /* reset */);
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showContent(List<Contribution> contributions, boolean clear) {
        if (clear) {
            contributionsAdapter.setData(contributions);
        } else {
            contributionsAdapter.addData(contributions);
        }

        // We're coming from a config change, so the state needs to be restored.
        if (listState != null) {
            linearLayoutManager.onRestoreInstanceState(listState);
            listState = null;
        }
    }

    @Override
    public void hideContent() {
        contributionsAdapter.clearData();
    }

    @Override
    public void openSubmission(String submissionId) {
        Intent intent = new Intent(ProfileActivity.this, SubmissionActivity.class);
        Bundle extras = new Bundle();
        extras.putString(SubmissionActivity.KEY_TITLE, getString(R.string.profile));
        extras.putString(Constants.THREAD_ID, submissionId);
        intent.putExtras(extras);
        startActivity(intent);
    }

    @Override
    public void openSubmissionAndScrollToComment(String submissionId, String commentId) {
        Intent intent = new Intent(ProfileActivity.this, SubmissionActivity.class);
        Bundle extras = new Bundle();
        extras.putString(SubmissionActivity.KEY_TITLE, getString(R.string.profile));
        extras.putString(Constants.THREAD_ID, submissionId);
        extras.putString(SubmissionActivity.KEY_COMMENT_TO_SCROLL_ID, commentId);
        intent.putExtras(extras);
        startActivity(intent);
    }

    @Override
    public void dismissSnackbar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    @Override
    public void scrollToTop() {
        recyclerView.smoothScrollToPosition(0);
    }

    @Override
    public void resetScrollingState() {
        scrollListener.resetState();
    }

    @Override
    public void showNotAuthenticatedToast() {
        Toast.makeText(this, R.string.not_authenticated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showNothingToShowSnackbar() {
        snackbar = Snackbar.make(coordinatorLayout, R.string.nothing_to_show,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    @Override
    public void showContributionsLoadingFailedSnackbar(final boolean reset) {
        snackbar = Snackbar.make(coordinatorLayout, R.string.failed_profile_data,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.loadContributions(reset);
            }
        });
        snackbar.show();
    }

    @Override
    public void showUnknownErrorToast(Throwable e) {
        Crashlytics.logException(e);
        Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
    }
}
