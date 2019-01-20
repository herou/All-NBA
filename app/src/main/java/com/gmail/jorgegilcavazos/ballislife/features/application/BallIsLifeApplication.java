package com.gmail.jorgegilcavazos.ballislife.features.application;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatDelegate;

import com.gmail.jorgegilcavazos.ballislife.BuildConfig;
import com.gmail.jorgegilcavazos.ballislife.crashreporting.CrashlyticsTree;
import com.gmail.jorgegilcavazos.ballislife.dagger.component.AppComponent;
import com.gmail.jorgegilcavazos.ballislife.dagger.component.DaggerAppComponent;
import com.gmail.jorgegilcavazos.ballislife.dagger.module.AppModule;
import com.gmail.jorgegilcavazos.ballislife.dagger.module.DataModule;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.leakcanary.LeakCanary;

import jonathanfinerty.once.Once;
import timber.log.Timber;

public class BallIsLifeApplication extends Application {

    private static Context context;
    private static AppComponent appComponent;

    public static AppComponent getAppComponent() {
        return appComponent;
    }

    public static Context getAppContext() {
        return context;
    }

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        FirebaseApp.initializeApp(context);
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        Once.initialise(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashlyticsTree());
        }

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .dataModule(new DataModule("https://nba-app-ca681.firebaseio.com/",
                        "http://data.nba.com/"))
                .build();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public FirebaseAnalytics getFirebaseAnalytics() {
        return firebaseAnalytics;
    }
}
