package com.gmail.jorgegilcavazos.ballislife.features.posts;

import com.gmail.jorgegilcavazos.ballislife.features.model.NBASubChips;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;

import java.util.List;

public interface PostsView {

    void setLoadingIndicator(boolean active);

    void showPosts(List<SubmissionWrapper> submissions, boolean reset);

    void showPostsLoadingFailedSnackbar(boolean reset);

    void dismissSnackbar();

    void showNotAuthenticatedToast();

    void showNotLoggedInToast();

    void showSubscribers(SubscriberCount subscriberCount);

    void openContentTab(String url);

    void showNothingToShowToast();

    void openStreamable(String shortcode);

    void showContentUnavailableToast();

    void changeViewType(int viewType);

    void scrollToTop();

    void resetScrollState();

    void share(String url);

    void showUnknownErrorToast();

    void setNbaSubChips(NBASubChips nbaSubChips);

    void hideSubmission(SubmissionWrapper submission, int index);

    void showHideSubmissionSnackbar(SubmissionWrapper submission, int index);

    void unHideSubmission(SubmissionWrapper submissionWrapper, int index);
}
