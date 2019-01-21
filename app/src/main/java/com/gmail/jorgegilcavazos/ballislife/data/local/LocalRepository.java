package com.gmail.jorgegilcavazos.ballislife.data.local;

import com.gmail.jorgegilcavazos.ballislife.features.model.HighlightViewType;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishCard;
import com.gmail.jorgegilcavazos.ballislife.features.model.SwishTheme;

public interface LocalRepository {

    void saveFavoritePostsViewType(int viewType);

    int getFavoritePostsViewType();

    void saveFavoriteHighlightViewType(HighlightViewType viewType);

    HighlightViewType getFavoriteHighlightViewType();

    void saveUsername(String username);

    String getUsername();

    String getStartupFragment();

    boolean getOpenYouTubeInApp();

    boolean getOpenBoxScoreByDefault();

    void saveAppTheme(SwishTheme theme);

    SwishTheme getAppTheme();

    boolean shouldShowWhatsNew();

    void setShouldShowWhatsNew(boolean showWhatsNew);

    boolean stickyChipsEnabled();

    boolean isUserWhitelisted();

    boolean noSpoilersModeEnabled();

    boolean swishCardSeen(SwishCard swishCard);

    void markSwishCardSeen(SwishCard swishCard);

    String getFavoriteTeam();

    void saveGameStreamAsUnlocked(String gameId);

    boolean isGameStreamUnlocked(String gameId);

    boolean isGameThreadStreamingEnabled();

    void setGameThreadStreamingEnabled(boolean enabled);
}
