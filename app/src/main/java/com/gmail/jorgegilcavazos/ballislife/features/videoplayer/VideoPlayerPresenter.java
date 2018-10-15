package com.gmail.jorgegilcavazos.ballislife.features.videoplayer;

import com.crashlytics.android.Crashlytics;
import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.service.StreamableService;
import com.gmail.jorgegilcavazos.ballislife.features.model.Streamable;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;

public class VideoPlayerPresenter extends BasePresenter<VideoPlayerView> {

    private StreamableService streamableService;
    private BaseSchedulerProvider schedulerProvider;
    private CompositeDisposable disposables;

    public VideoPlayerPresenter(StreamableService streamableService, BaseSchedulerProvider schedulerProvider) {
        this.streamableService = streamableService;
        this.schedulerProvider = schedulerProvider;

        disposables = new CompositeDisposable();
    }

    public void loadStreamable(String shortcode) {
        disposables.add(streamableService.getStreamable(shortcode)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<Streamable>() {
                    @Override
                    public void onSuccess(Streamable streamable) {
                        String videoUrl = null;

                        // Get mobile url, if not available, get normal url instead.
                        if (streamable != null) {
                            if (streamable.getFiles() != null) {
                                Streamable.StreamableFiles files = streamable.getFiles();

                                if (files.getMp4Mobile() != null) {
                                    videoUrl = files.getMp4Mobile().getUrl();
                                } else if (files.getMp4() != null) {
                                    videoUrl = files.getMp4().getUrl();
                                }
                            }
                        }

                        if (videoUrl == null) {
                            view.showCouldNotLoadVideoToast();
                            view.finishActivity();
                        } else {
                            view.playStreamable(videoUrl);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showCouldNotLoadVideoToast();
                        view.finishActivity();
                    }
                })
        );
    }

    public void onErrorPlayingVideo(Exception e) {
        Crashlytics.log("Error playing video");
        Crashlytics.logException(e);
        view.showCouldNotLoadVideoToast();
        view.finishActivity();
    }

    public void stop() {
        if (disposables != null) {
            disposables.clear();
        }
    }
}
