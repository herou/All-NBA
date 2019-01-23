package com.gmail.jorgegilcavazos.ballislife.features.standings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.analytics.EventLogger;
import com.gmail.jorgegilcavazos.ballislife.analytics.SwishScreen;
import com.gmail.jorgegilcavazos.ballislife.data.premium.PremiumService;
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaStandingsService;
import com.gmail.jorgegilcavazos.ballislife.features.application.BallIsLifeApplication;
import com.gmail.jorgegilcavazos.ballislife.features.model.Standings;
import com.gmail.jorgegilcavazos.ballislife.util.TeamUtils;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import retrofit2.Retrofit;

public class StandingsFragment extends Fragment implements StandingsView,
        SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "StandingsFragment";

    private static final int EAST = 0;
    private static final int WEST = 1;

    @Inject
    @Named("SwishBackend") Retrofit retrofit;
    @Inject BaseSchedulerProvider schedulerProvider;
    @Inject EventLogger eventLogger;
    @Inject PremiumService premiumService;

    @BindView(R.id.standings_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.layout_content) LinearLayout layoutContent;
    @BindView(R.id.adView) AdView adView;

    private Snackbar snackbar;
    private Unbinder unbinder;

    private StandingsPresenter presenter;
    private CompositeDisposable disposables;

    public StandingsFragment() {
        // Required empty public constructor.
    }

    public static StandingsFragment newInstance() {
        return new StandingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        BallIsLifeApplication.getAppComponent().inject(this);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_standings, container, false);
        unbinder = ButterKnife.bind(this, view);

        swipeRefreshLayout.setOnRefreshListener(this);
        layoutContent.setVisibility(View.GONE);

        NbaStandingsService service = retrofit.create(NbaStandingsService.class);

        disposables = new CompositeDisposable();

        presenter = new StandingsPresenter(service, schedulerProvider, disposables);
        presenter.attachView(this);
        presenter.loadStandings();

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
    public void onResume() {
        super.onResume();
        eventLogger.setCurrentScreen(getActivity(), SwishScreen.STANDINGS);
    }

    @Override
    public void onPause() {
        presenter.dismissSnackbar();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (disposables != null) {
            disposables.clear();
        }
        unbinder.unbind();
        presenter.detachView();
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                presenter.loadStandings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        presenter.loadStandings();
    }

    @Override
    public void setLoadingIndicator(boolean active) {
        swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showStandings(Standings standings) {
        layoutContent.setVisibility(View.VISIBLE);
        layoutContent.removeAllViews();

        addConferenceHeader(EAST);
        addTeamRows(standings.getEast());

        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.divider_layout, layoutContent, false);
        layoutContent.addView(view);

        addConferenceHeader(WEST);
        addTeamRows(standings.getWest());
    }

    @Override
    public void hideStandings() {
        layoutContent.removeAllViews();
        layoutContent.setVisibility(View.GONE);
    }

    @Override
    public void showSnackbar(boolean canReload) {
        snackbar = Snackbar.make(getView(), R.string.failed_standings_data,
                Snackbar.LENGTH_INDEFINITE);
        if (canReload) {
            snackbar.setAction(R.string.retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.loadStandings();
                }
            });
        }
        snackbar.show();
    }

    @Override
    public void dismissSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void addConferenceHeader(int conference) {
        View eastHeader = LayoutInflater.from(getActivity())
                .inflate(R.layout.standings_conference_header, layoutContent, false);
        TextView tvEastConf = eastHeader.findViewById(R.id.text_conference);

        if (conference == EAST) {
            tvEastConf.setText(getResources().getString(R.string.eastern_conference));
        } else if (conference == WEST) {
            tvEastConf.setText(getResources().getString(R.string.western_conference));
        } else {
            throw new IllegalStateException("Conference was neither east nor west");
        }

        layoutContent.addView(eastHeader);
    }

    private void addTeamRows(List<Standings.TeamStanding> teamStandings) {
        for (Standings.TeamStanding teamStanding : teamStandings) {
            View teamRow = LayoutInflater.from(getActivity())
                    .inflate(R.layout.standings_team_item, layoutContent, false);
            TextView tvId = teamRow.findViewById(R.id.text_seed);
            TextView tvTeam = teamRow.findViewById(R.id.text_team);
            TextView tvWins = teamRow.findViewById(R.id.text_wins);
            TextView tvLosses = teamRow.findViewById(R.id.text_losses);
            TextView tvPct = teamRow.findViewById(R.id.text_pct);
            TextView tvGB = teamRow.findViewById(R.id.text_gb);
            TextView tvStreak = teamRow.findViewById(R.id.textStreak);
            ImageView logo = teamRow.findViewById(R.id.logo);

            String wins = "", losses = "", pct = "", gb = "", streak = "";
            for (Standings.StandingStat stat : teamStanding.getStats()) {
                if (stat.getName().equals("W")) wins = stat.getValue();
                if (stat.getName().equals("L")) losses = stat.getValue();
                if (stat.getName().equals("PCT")) pct = stat.getValue();
                if (stat.getName().equals("GB")) gb = stat.getValue();
                if (stat.getName().equals("STRK")) streak = stat.getValue();
            }

            tvId.setText(String.valueOf(teamStanding.getSeed()));
            tvTeam.setText(teamStanding.getName());
            tvWins.setText(wins);
            tvLosses.setText(losses);
            tvPct.setText(pct);
            tvGB.setText(gb);
            tvStreak.setText(streak);
            logo.setImageResource(TeamUtils.Companion.getTeamLogo(teamStanding.getAbbreviation()));

            layoutContent.addView(teamRow);
        }
    }
}
