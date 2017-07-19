package com.gmail.jorgegilcavazos.ballislife.data.reddit;

import android.content.SharedPreferences;

import net.dean.jraw.RedditClient;

import io.reactivex.Completable;

/**
 * Manages a reddit user's authentication session.
 */
public interface RedditAuthentication {

    /**
     * Returns the reddit client used in this authenticator.
     */
    RedditClient getRedditClient();

    /**
     * Authenticates with user context if a refresh token is saved in shared preferences. Otherwise
     * authenticates without a user context.
     */
    Completable authenticate(SharedPreferences sharedPreferences);

    /**
     * Authenticates with a user context. On success, saves the refresh token to a shared
     * preferences file.
     */
    Completable authenticateUser(String url, SharedPreferences sharedPreferences);

    /**
     * De-authenticates the user if one is logged in.
     */
    Completable deAuthenticateUser(SharedPreferences sharedPreferences);
}
