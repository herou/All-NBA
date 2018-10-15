package com.gmail.jorgegilcavazos.ballislife.features.standings;

import android.util.Log;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.service.NbaStandingsService;
import com.gmail.jorgegilcavazos.ballislife.features.model.Standings;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;

public class StandingsPresenter extends BasePresenter<StandingsView> {

    private NbaStandingsService nbaStandingsService;
    private BaseSchedulerProvider schedulerProvider;
    private CompositeDisposable disposables;

    public StandingsPresenter(NbaStandingsService nbaStandingsService,
                              BaseSchedulerProvider schedulerProvider,
                              CompositeDisposable disposables) {
        this.nbaStandingsService = nbaStandingsService;
        this.schedulerProvider = schedulerProvider;
        this.disposables = disposables;
    }

    public void loadStandings() {
        view.setLoadingIndicator(true);
        view.dismissSnackbar();
        view.hideStandings();

        disposables.clear();
        disposables.add(nbaStandingsService.getStandings("22018")
                .subscribeOn(schedulerProvider.io())
                .map(this::sortTeams)
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<Standings>() {
                    @Override
                    public void onSuccess(Standings standings) {
                        view.setLoadingIndicator(false);
                        view.showStandings(standings);
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.setLoadingIndicator(false);
                        view.showSnackbar(true);
                    }
                })
        );
    }

    public void dismissSnackbar() {
        view.dismissSnackbar();
    }

    private Standings sortTeams(Standings standings) {
        return new Standings(sortTeams(standings.getEast()), sortTeams(standings.getWest()));
    }

    private List<Standings.TeamStanding> sortTeams(List<Standings.TeamStanding> unsortedTeams) {
        Collections.sort(unsortedTeams, (team1, team2) -> {
            int team1Seed = Integer.MAX_VALUE;
            int team2Seed = Integer.MAX_VALUE;
            try {
                team1Seed = Integer.parseInt(team1.getSeed());
            } catch (NumberFormatException e) {
                Log.d(StandingsPresenter.class.getSimpleName(), "Seed is not an integer");
            }
            try {
                team2Seed = Integer.parseInt(team2.getSeed());
            } catch (NumberFormatException e) {
                Log.d(StandingsPresenter.class.getSimpleName(), "Seed is not an integer");
            }
            if(team1Seed < team2Seed)
                return -1;
            else if(team1Seed == team2Seed)
                return 0;
            return 1;
        });
        return unsortedTeams;
    }
}
