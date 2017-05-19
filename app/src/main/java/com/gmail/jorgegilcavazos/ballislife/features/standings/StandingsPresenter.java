package com.gmail.jorgegilcavazos.ballislife.features.standings;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.API.NbaStandingsService;
import com.gmail.jorgegilcavazos.ballislife.features.model.Standings;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

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
        disposables.add(nbaStandingsService.getStandings("22016")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
}
