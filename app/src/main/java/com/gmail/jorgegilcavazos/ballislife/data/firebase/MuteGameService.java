package com.gmail.jorgegilcavazos.ballislife.data.firebase;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.util.Set;

/**
 * Adds a game to the set of muted games in shared preferences. The service is used when the user
 * no longer wants to receive notifications from a specific game without having to disable alerts
 * future alerts for those teams.
 */

public class MuteGameService extends Service {
    private static final String TAG = "MuteGameService";

    public static final String MUTE_GAMES_PREFS = "muteGamesPrefs";
    public static final String KEY_MUTE_GAMES = "keyMuteGames";

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(MUTE_GAMES_PREFS, MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (intent == null) {
            return START_NOT_STICKY;
        }

        int id = intent.getIntExtra("id", -1);
        Log.d(TAG, "id: " + id);
        if (id != -1) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        } else {
            Crashlytics.logException(new Exception("ID should not be -1 in MuteGameService.java"));
        }

        Set<String> mutedGames = sharedPreferences.getStringSet(KEY_MUTE_GAMES, null);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Copy old contents into new Set because of a bug in the framework
        // See: https://code.google.com/p/android/issues/detail?id=27801
        Set<String> mutedGamesCopy = new ArraySet<>(mutedGames);

        mutedGamesCopy.add(String.valueOf(id));
        editor.putStringSet(KEY_MUTE_GAMES, mutedGamesCopy);
        editor.commit();

        return START_REDELIVER_INTENT;
    }
}
