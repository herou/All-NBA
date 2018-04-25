package com.gmail.jorgegilcavazos.ballislife.features.games;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.LoadGamesEvent;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.OpenGameEvent;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesUiEvent.RefreshGamesEvent;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.model.GameV2;
import com.gmail.jorgegilcavazos.ballislife.features.model.NbaGame;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.jakewharton.rxrelay2.PublishRelay;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;

/**
 * Displays a list of {@link NbaGame}s for the selected date.
 */
public class GamesFragment extends Fragment implements GamesView,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String KEY_DATE = "Date";
    private static final String SELECTED_TIME = "SelectedTime";
    private static final String LIST_STATE = "ListState";
    public final static String TAG = "GamesFragment";
    public final static String GAME_THREAD_HOME = "GAME_THREAD_HOME";
    public final static String GAME_THREAD_AWAY = "GAME_THREAD_AWAY";
    public final static String GAME_ID = "GAME_ID";
    public final static String GAME_DATE = "GAME_DATE";
    public final static String GAME_STATUS = "GAME_STATUS";

    @Inject GamesPresenter presenter;
    @Inject LocalRepository localRepository;

    @BindView(R.id.no_games_text) TextView tvNoGames;
    @BindView(R.id.games_rv) RecyclerView rvGames;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    private Parcelable listState;
    private RecyclerView.LayoutManager layoutManager;
    private GameAdapter gameAdapter;
    private Snackbar snackbar;
    private Unbinder unbinder;

    /**
     * The position from 0-500 that this fragment is positioned inside the [GamesHomePagerAdapter],
     * with 250 being the middle page (i.e., today).
     */
    private int position;

    private PublishRelay<LoadGamesEvent> loadGamesEvents = PublishRelay.create();
    private PublishRelay<RefreshGamesEvent> refreshGamesEvents = PublishRelay.create();

    public GamesFragment() {
        // Required empty public constructor.
    }

    public static GamesFragment newInstance(int position) {
        GamesFragment fragment = new GamesFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_DATE, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        BallIsLifeApplication.getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        position = getArguments().getInt(KEY_DATE);

        layoutManager = new LinearLayoutManager(getActivity());
        gameAdapter = new GameAdapter(new ArrayList<>(0), localRepository);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);

        rvGames.setLayoutManager(layoutManager);
        rvGames.setAdapter(gameAdapter);

        presenter.attachView(this);

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
        loadGamesEvents.accept(new LoadGamesEvent(dateToShow()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        listState = layoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE, listState);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        presenter.detachView();
        super.onDestroyView();
    }

    @Override
    public void onRefresh() {
        refreshGamesEvents.accept(new RefreshGamesEvent(dateToShow()));
    }

    @NotNull
    @Override
    public Observable<OpenGameEvent> openGameEvents() {
        return gameAdapter.getGameClicks().map(OpenGameEvent::new);
    }

    @NotNull
    @Override
    public Observable<LoadGamesEvent> loadGamesEvents() {
        return loadGamesEvents;
    }

    @NotNull
    @Override
    public Observable<RefreshGamesEvent> refreshGamesEvents() {
        return refreshGamesEvents;
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void hideGames() {
        rvGames.setVisibility(View.GONE);
    }

    @Override
    public void showGames(List<GameV2> games) {
        gameAdapter.swap(games);
        rvGames.setVisibility(View.VISIBLE);
    }

    @Override
    public void showGameDetails(@NonNull GameV2 game) {
        Intent intent = new Intent(getActivity(), CommentsActivity.class);
        intent.putExtra(GAME_THREAD_HOME, game.getHomeTeamAbbr());
        intent.putExtra(GAME_THREAD_AWAY, game.getAwayTeamAbbr());
        intent.putExtra(GAME_ID, game.getId());
        intent.putExtra(GAME_DATE, game.getTimeUtc());
        intent.putExtra(GAME_STATUS, game.getGameStatus());
        startActivity(intent);
    }

    @Override
    public void setNoGamesIndicator(boolean active) {
        if (active) {
            tvNoGames.setVisibility(View.VISIBLE);
        } else {
            tvNoGames.setVisibility(View.GONE);
        }
    }

    @Override
    public void showNoNetSnackbar() {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(
                getView(),
                R.string.your_device_is_offline,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, v -> {
                    refreshGamesEvents.accept(new RefreshGamesEvent(dateToShow()));
                });
        snackbar.show();
    }

    @Override
    public void showErrorSnackbar(int code) {
        if (getView() == null) {
            return;
        }

        snackbar = Snackbar.make(
                getView(),
                getString(R.string.something_went_wrong, code),
                Snackbar.LENGTH_SHORT)
                .setAction(R.string.retry, v -> {
                    refreshGamesEvents.accept(new RefreshGamesEvent(dateToShow()));
                });
        snackbar.show();
    }

    @Override
    public void dismissSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    /**
     * Returns the date that must be used to load games for the current fragment considering the
     * current date.
     * The date returned is based on the position of this fragment, where 250 will always be today.
     */
    private Calendar dateToShow() {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(UtilsKt.getDateForPosition(position));
        return date;
    }
}
