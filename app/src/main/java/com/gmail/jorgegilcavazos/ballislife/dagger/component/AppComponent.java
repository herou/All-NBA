package com.gmail.jorgegilcavazos.ballislife.dagger.component;

import com.gmail.jorgegilcavazos.ballislife.dagger.module.AppModule;
import com.gmail.jorgegilcavazos.ballislife.dagger.module.BindModule;
import com.gmail.jorgegilcavazos.ballislife.dagger.module.DataModule;
import com.gmail.jorgegilcavazos.ballislife.features.boxscore.BoxScoreFragment;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesFragment;
import com.gmail.jorgegilcavazos.ballislife.features.games.GamesHomeFragment;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.CommentsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.gamethread.GameThreadFragment;
import com.gmail.jorgegilcavazos.ballislife.features.gopremium.GoPremiumActivity;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.HighlightsMenuFragment;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.favorites.FavoritesFragment;
import com.gmail.jorgegilcavazos.ballislife.features.highlights.home.HighlightsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.login.LoginActivity;
import com.gmail.jorgegilcavazos.ballislife.features.main.MainActivity;
import com.gmail.jorgegilcavazos.ballislife.features.playoffs.bracket.BracketFragment;
import com.gmail.jorgegilcavazos.ballislife.features.posts.PostsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.profile.ProfileActivity;
import com.gmail.jorgegilcavazos.ballislife.features.reply.ReplyActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsActivity;
import com.gmail.jorgegilcavazos.ballislife.features.settings.SettingsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.standings.StandingsFragment;
import com.gmail.jorgegilcavazos.ballislife.features.submission.SubmissionActivity;
import com.gmail.jorgegilcavazos.ballislife.features.tour.TourLoginActivity;
import com.gmail.jorgegilcavazos.ballislife.features.videoplayer.VideoPlayerActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {AppModule.class, DataModule.class, BindModule.class})
public interface AppComponent {
    void inject(MainActivity activity);

    void inject(HighlightsFragment fragment);

    void inject(PostsFragment fragment);

    void inject(StandingsFragment fragment);

    void inject(GamesFragment gamesFragment);

    void inject(ProfileActivity profileActivity);

    void inject(GameThreadFragment gameThreadFragment);

    void inject(LoginActivity loginActivity);

    void inject(SettingsFragment settingsFragment);

    void inject(SubmissionActivity submissionActivity);

    void inject(BoxScoreFragment boxScoreFragment);

    void inject(VideoPlayerActivity videoPlayerActivity);

    void inject(ReplyActivity replyActivity);

    void inject(CommentsActivity commentsActivity);

    void inject(SettingsActivity settingsActivity);

    void inject(GoPremiumActivity goPremiumActivity);

    void inject(TourLoginActivity tourLoginActivity);

    void inject(FavoritesFragment favoritesFragment);

    void inject(GamesHomeFragment gamesHomeFragment);

    void inject(HighlightsMenuFragment highlightsMenuFragment);

    void inject(BracketFragment bracketFragment);
}
