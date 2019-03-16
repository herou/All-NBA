package com.gmail.jorgegilcavazos.ballislife.features.posts;

import com.gmail.jorgegilcavazos.ballislife.base.BasePresenter;
import com.gmail.jorgegilcavazos.ballislife.data.local.LocalRepository;
import com.gmail.jorgegilcavazos.ballislife.data.reddit.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.data.repository.posts.PostsRepository;
import com.gmail.jorgegilcavazos.ballislife.data.service.RedditService;
import com.gmail.jorgegilcavazos.ballislife.features.model.NBASubChips;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;
import com.gmail.jorgegilcavazos.ballislife.util.Constants;
import com.gmail.jorgegilcavazos.ballislife.util.Utilities;
import com.gmail.jorgegilcavazos.ballislife.util.exception.NotAuthenticatedException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.NotLoggedInException;
import com.gmail.jorgegilcavazos.ballislife.util.schedulers.BaseSchedulerProvider;

import net.dean.jraw.models.MultiReddit;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class PostsPresenter extends BasePresenter<PostsView> {

    private RedditAuthentication redditAuthentication;
    private LocalRepository localRepository;
    private PostsRepository postsRepository;
    private RedditService redditService;
    private BaseSchedulerProvider schedulerProvider;

    private CompositeDisposable disposables;
    private String subreddit;

    @Inject
    public PostsPresenter(
            RedditAuthentication redditAuthentication,
            LocalRepository localRepository,
            PostsRepository postsRepository,
            RedditService redditService,
            BaseSchedulerProvider schedulerProvider) {
        this.redditAuthentication = redditAuthentication;
        this.localRepository = localRepository;
        this.postsRepository = postsRepository;
        this.redditService = redditService;
        this.schedulerProvider = schedulerProvider;

        disposables = new CompositeDisposable();
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public void loadSubscriberCount() {
        disposables.add(redditAuthentication.authenticate()
                .andThen(redditService.getSubscriberCount(redditAuthentication.getRedditClient(),
                        subreddit))
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableSingleObserver<SubscriberCount>() {
                    @Override
                    public void onSuccess(SubscriberCount subscriberCount) {
                        if (isViewAttached()) {
                            view.showSubscribers(subscriberCount);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );
    }

    public void loadFirstAvailable(Sorting sorting, TimePeriod timePeriod) {
        List<SubmissionWrapper> submissions = postsRepository.getCachedSubmissions();
        if (submissions.isEmpty()) {
            resetPaginatorThenLoadPosts(sorting, timePeriod);
        } else {
            if (localRepository.stickyChipsEnabled() && sorting == Sorting.HOT) {
                NBASubChips chips = removeCommonStickiedSubmissions(submissions);
                if (chips != null) {
                    // Chips != null means that we have at least 1 chip to show (in this batch).
                    // If so, then show them in our view. If there aren't any available then leave
                    // the view as is WITHOUT setting the chips to null.
                    // We don't want to remove existing shown chips even if this batch of
                    // submissions doesn't contain any, because the 1st page of loaded
                    // submissions may have contained chips and we don't want to remove those.
                    view.setNbaSubChips(chips);
                }
            } else {
                view.setNbaSubChips(null);
            }
            view.showPosts(filterHiddenPosts(submissions), true /* clear */);
        }
    }

    public void resetPaginatorThenLoadPosts(Sorting sorting, TimePeriod timePeriod) {
        if (subreddit.equals(Constants.MULTI_SWISH)) {
            redditService.getMultiReddit(redditAuthentication.getRedditClient(),
                    "Obi-Wan_Ginobili", Constants.MULTI_SWISH)
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribeWith(new DisposableSingleObserver<MultiReddit>() {
                        @Override
                        public void onSuccess(MultiReddit multiReddit) {
                            postsRepository.reset(sorting, timePeriod, multiReddit);
                            loadPosts(true /* reset */);
                        }

                        @Override
                        public void onError(Throwable e) {
                            view.showPostsLoadingFailedSnackbar(true);
                        }
                    });
        } else {
            postsRepository.reset(sorting, timePeriod, subreddit);
            loadPosts(true /* reset */);
        }
    }

    public void loadPosts(final boolean reset) {
        if (reset) {
            view.resetScrollState();
            view.setLoadingIndicator(true);
        }

        view.dismissSnackbar();

        disposables.add(redditAuthentication.authenticate()
                .andThen(postsRepository.next())
                .map(this::filterHiddenPosts)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui()).subscribeWith(
                        new DisposableSingleObserver<List<SubmissionWrapper>>() {
                    @Override
                    public void onSuccess(List<SubmissionWrapper> submissions) {
                        if (submissions.isEmpty()) {
                            view.showNothingToShowToast();
                            return;
                        }

                        if (localRepository.stickyChipsEnabled()
                                && postsRepository.getCurrentSorting() == Sorting.HOT) {
                            NBASubChips chips = removeCommonStickiedSubmissions(submissions);
                            if (chips != null) {
                                // Chips != null means that we have at least 1 chip to show
                                // (in this batch). If so, then show them in our view. If there
                                // aren't any available then leave the view as is WITHOUT setting
                                // the chips to null. We don't want to remove existing shown chips
                                // even if this batch of submissions doesn't contain any, because
                                // the 1st page of loaded submissions may have contained chips and
                                // we don't want to remove those.
                                view.setNbaSubChips(chips);
                            }
                        }
                        else {
                            view.setNbaSubChips(null);
                        }

                        view.showPosts(submissions, reset);
                        if (reset) {
                            view.scrollToTop();
                        }

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof NotAuthenticatedException) {
                            view.showNotAuthenticatedToast();
                        } else {
                            view.showPostsLoadingFailedSnackbar(reset);
                        }

                        if (reset) {
                            view.setLoadingIndicator(false);
                        }
                    }
                })
        );
    }

    public void onVote(final Submission submission, final VoteDirection direction) {
        if (!redditAuthentication.isUserLoggedIn()) {
            view.showNotLoggedInToast();
            return;
        }

        disposables.add(redditAuthentication.authenticate()
                .andThen(redditAuthentication.checkUserLoggedIn()).flatMapCompletable(loggedIn -> {
                    if (loggedIn) {
                        return redditService.voteSubmission(redditAuthentication.getRedditClient(),
                                                      submission,
                                                      direction);
                    } else {
                        throw new NotLoggedInException();
                    }
                })
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );
    }

    public void onSave(final Submission submission, final boolean saved) {
        if (!redditAuthentication.isUserLoggedIn()) {
            view.showNotLoggedInToast();
            return;
        }

        disposables.add(redditAuthentication.authenticate()
                .andThen(redditAuthentication.checkUserLoggedIn()).flatMapCompletable(loggedIn -> {
                    if (loggedIn) {
                        return redditService.saveSubmission(redditAuthentication.getRedditClient(),
                                                      submission,
                                                      saved);
                    } else {
                        throw new NotLoggedInException();
                    }
                })
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                })
        );
    }

    public void onHide(final SubmissionWrapper submissionWrapper,
                       int index, final boolean hide) {
        if (!redditAuthentication.isUserLoggedIn()) {
            view.showNotLoggedInToast();
            return;
        }

        disposables.add(redditAuthentication.authenticate()
                .andThen(redditAuthentication.checkUserLoggedIn()).flatMapCompletable(loggedIn -> {
                    if (loggedIn) {
                        return redditService.hideSubmission(redditAuthentication.getRedditClient(),
                                submissionWrapper.getSubmission(),
                                hide);
                    } else {
                        throw new NotLoggedInException();
                    }
                })
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        if (!isViewAttached()) {
                            return;
                        }
                        if (hide) {
                            view.hideSubmission(submissionWrapper, index);
                            view.showHideSubmissionSnackbar(submissionWrapper, index);
                        } else {
                            view.unHideSubmission(submissionWrapper, index);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) {
                            view.showUnknownErrorToast();
                        }
                    }
                })
        );
    }

    public void onContentClick(String url) {
        if (url != null) {
            if (url.contains(Constants.STREAMABLE_DOMAIN)) {
                String shortCode = Utilities.getStreamableShortcodeFromUrl(url);
                if (shortCode != null) {
                    view.openStreamable(shortCode);
                } else {
                    view.openContentTab(url);
                }
            } else {
                view.openContentTab(url);
            }
        } else {
            view.showContentUnavailableToast();
        }
    }

    public void onViewTypeSelected(int viewType) {
        localRepository.saveFavoritePostsViewType(viewType);
        view.changeViewType(viewType);
    }

    public void subscribeToSubmissionShare(Observable<Submission> shareObservable) {
        disposables.add(shareObservable
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeWith(new DisposableObserver<Submission>() {
                    @Override
                    public void onNext(Submission submission) {
                        if (submission.isSelfPost()) {
                            view.share(Constants.HTTPS + Constants.REDDIT_DOMAIN +
                                    submission.getPermalink());
                        } else {
                            view.share(submission.getUrl());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showUnknownErrorToast();
                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
    }

    public void stop() {
        if (disposables != null) {
            disposables.clear();
        }
    }

    private NBASubChips removeCommonStickiedSubmissions(List<SubmissionWrapper> submissions) {
        SubmissionWrapper sub1 = submissions.get(0);
        SubmissionWrapper sub2 = submissions.get(1);

        String dailyLockerId = null;
        String powerRankingsId = null;
        String trashTalkId = null;
        String freeTalkFridayId = null;

        boolean shouldRemoveSub1 = true;
        if (isDailyRockerRoomThread(sub1)) {
            dailyLockerId = sub1.getId();
        } else if (isFreeTalkFridayThread(sub1)) {
            freeTalkFridayId = sub1.getId();
        } else if (isPowerRankingsThread(sub1)) {
            powerRankingsId = sub1.getId();
        } else if (isTrashTalkThread(sub1)) {
            trashTalkId = sub1.getId();
        } else {
            shouldRemoveSub1 = false;
        }

        boolean shouldRemoveSub2 = true;
        if (isDailyRockerRoomThread(sub2)) {
            dailyLockerId = sub2.getId();
        } else if (isFreeTalkFridayThread(sub2)) {
            freeTalkFridayId = sub2.getId();
        } else if (isPowerRankingsThread(sub2)) {
            powerRankingsId = sub2.getId();
        } else if (isTrashTalkThread(sub2)) {
            trashTalkId = sub2.getId();
        } else {
            shouldRemoveSub2 = false;
        }

        if (shouldRemoveSub2) {
            submissions.remove(1);
        }
        if (shouldRemoveSub1) {
            submissions.remove(0);
        }

        if (shouldRemoveSub1 || shouldRemoveSub2) {
            return new NBASubChips(dailyLockerId, powerRankingsId, trashTalkId, freeTalkFridayId);
        } else {
            return null;
        }
    }

    private boolean isDailyRockerRoomThread(SubmissionWrapper submission) {
        String DAILY_LOCKER_ROOM = "DAILY LOCKER ROOM";
        return submission.isStickied()
                && submission.getTitle().toUpperCase().contains(DAILY_LOCKER_ROOM);
    }

    private boolean isPowerRankingsThread(SubmissionWrapper submission) {
        String POWER_RANKINGS = "OFFICIAL /R/NBA POWER RANKINGS";
        return submission.isStickied()
                && submission.getTitle().toUpperCase().contains(POWER_RANKINGS);
    }

    private boolean isTrashTalkThread(SubmissionWrapper submission) {
        String TRASH_TALK = "TRASH TALK";
        return submission.isStickied()
                && submission.getTitle().toUpperCase().contains(TRASH_TALK);

    }

    private boolean isFreeTalkFridayThread(SubmissionWrapper submission) {
        String FREE_TALK_FRIDAY = "FREE TALK FRIDAY";
        return submission.isStickied()
                && submission.getTitle().toUpperCase().contains(FREE_TALK_FRIDAY);
    }

    private List<SubmissionWrapper> filterHiddenPosts(List<SubmissionWrapper> allPosts) {
        List<SubmissionWrapper> filteredPosts = new ArrayList<>();

        for (SubmissionWrapper submissionWrapper: allPosts) {
            if(!submissionWrapper.isHidden()) {
                filteredPosts.add(submissionWrapper);
            }
        }

        return filteredPosts;
    }
}
