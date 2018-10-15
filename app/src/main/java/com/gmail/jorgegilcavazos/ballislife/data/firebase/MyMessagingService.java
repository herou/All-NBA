package com.gmail.jorgegilcavazos.ballislife.data.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.gmail.jorgegilcavazos.ballislife.R;
import com.gmail.jorgegilcavazos.ballislife.features.main.MainActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsFragment;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Set;

public class MyMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyMessagingService";
    private static final String NOTIFICATION_CHANNEL_ID = "swish.main.channel";

    private static final String TYPE_KEY = "type";
    private static final String CGA_TYPE = "CGA";
    private static final String SCORES_UPDATE_TYPE = "scores";

    public static final String KEY_SCORES_UPDATED = "scores_updated";
    public static final String FILTER_SCORES_UPDATED = "com.gmail.jorgegilcavazos.ballislife.SCORES_UPDATED";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(getApplicationContext());
        }

        // Ignore notifications if alerts are disabled in settings.
        if (!areAlertsEnabled()) {
            return;
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            switch (data.get(TYPE_KEY)) {
                case CGA_TYPE:
                    onCgaMessageReceived(data);
                    break;
                case SCORES_UPDATE_TYPE:
                    onScoresUpdateReceived(data);
                    break;
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            // Handle notification.
        }

    }

    private void onScoresUpdateReceived(Map<String, String> data) {
        Intent intent = new Intent(FILTER_SCORES_UPDATED);
        intent.putExtra(KEY_SCORES_UPDATED, data.get("body"));
        sendBroadcast(intent);
    }

    private void onCgaMessageReceived(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        int id = Integer.parseInt(data.get("id"));

        if (!isGameMuted(id)) {
            sendCgaNotification(title, body, id);
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param body FCM message body received.
     */
    private void sendCgaNotification(String title, String body, int id) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Intent actionIntent = new Intent(this, MuteGameService.class);
        actionIntent.putExtra("id", id);
        PendingIntent actionPendingIntent = PendingIntent.getService(this, id,
                actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                R.drawable.ic_alarm_off_black_24dp, getResources().getString(R.string.mute_game),
                actionPendingIntent)
                .build();

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_ball)
                .setColor(getResources().getColor(R.color.red))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setOnlyAlertOnce(true)
                .addAction(action)
                .setContentIntent(pendingIntent);

        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }

    private boolean isGameMuted(int id) {
        String sId = String.valueOf(id);

        SharedPreferences sharedPreferences = getSharedPreferences(
                MuteGameService.MUTE_GAMES_PREFS, MODE_PRIVATE);
        Set<String> mutedGames = sharedPreferences.getStringSet(
                MuteGameService.KEY_MUTE_GAMES, null);

        if (mutedGames == null) {
            return false;
        }

        for (String mutedGame : mutedGames) {
            if (sId.equals(mutedGame)) {
                return true;
            }
        }

        return false;
    }

    private boolean areAlertsEnabled() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getBoolean(SettingsFragment.KEY_ENABLE_ALERTS, false);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(Context context) {
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        final String name = "Swish";
        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationChannel.setDescription("Notifications");
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
