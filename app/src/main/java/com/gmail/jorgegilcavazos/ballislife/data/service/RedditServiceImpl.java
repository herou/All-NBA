package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyNotAvailableException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyToCommentException;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.SubmissionRequest;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.MultiRedditManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.MultiReddit;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.Paginator;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class RedditServiceImpl implements RedditService {

    @Inject
    public RedditServiceImpl() {
    }

    @Override
    public Single<List<Contribution>> getUserContributions(
            final UserContributionPaginator paginator) {
        return Single.create(e -> {
            try {
                e.onSuccess(new ArrayList<>(paginator.next()));
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<CommentNode> getComment(
            final RedditClient redditClient,
            final String threadId,
            final String commentId) {
        return Single.create(e -> {
            SubmissionRequest.Builder builder = new SubmissionRequest.Builder(threadId);
            builder.sort(CommentSort.NEW);

            SubmissionRequest submissionRequest = builder.build();
            Submission submission = null;
            try {
                submission = redditClient.getSubmission(submissionRequest);

                Iterable<CommentNode> iterable = submission.getComments().walkTree();
                for (CommentNode node : iterable) {
                    if (node.getComment().getId().equals(commentId)) {
                        if (!e.isDisposed()) {
                            e.onSuccess(node);
                            return;
                        }
                    }
                }

                if (!e.isDisposed()) {
                    e.onError(new ReplyNotAvailableException());
                }
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<String> replyToComment(
            final RedditClient redditClient,
            final Comment parent,
            final String text) {
        return Single.create(e -> {
            AccountManager accountManger = new AccountManager(redditClient);
            try {
                String id = accountManger.reply(parent, text);
                e.onSuccess(id);
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(new ReplyToCommentException());
                }
            }
        });
    }

    @Override
    public Completable voteComment(
            final RedditClient redditClient,
            final Comment comment,
            final VoteDirection direction) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                accountManager.vote(comment, direction);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable savePublicContribution(final RedditClient redditClient,
            final PublicContribution contribution) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                accountManager.save(contribution);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable unsavePublicContribution(final RedditClient redditClient,
            final PublicContribution contribution) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                accountManager.unsave(contribution);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<String> replyToThread(
            final RedditClient redditClient,
            final Submission submission,
            final String text) {
        return Single.create((e) -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                e.onSuccess(accountManager.reply(submission, text));
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<Submission> getSubmission(
            final RedditClient redditClient,
            final String threadId,
            final CommentSort sort) {
        return Single.create(e -> {
            SubmissionRequest.Builder builder = new SubmissionRequest.Builder(threadId);
            if (sort != null) {
                builder.sort(sort);
            }

            SubmissionRequest submissionRequest = builder.build();
            try {
                e.onSuccess(redditClient.getSubmission(submissionRequest));
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<Listing<Submission>> getSubmissionListing(final Paginator<Submission> paginator) {
        return Single.create(e -> {
            try {
                Listing<Submission> listing = paginator.next(false);
                e.onSuccess(listing);
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable voteSubmission(
            final RedditClient redditClient,
            final Submission submission,
            final VoteDirection vote) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                accountManager.vote(submission, vote);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable saveSubmission(
            final RedditClient redditClient,
            final Submission submission,
            final boolean saved) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                if (saved) {
                    accountManager.save(submission);
                } else {
                    accountManager.unsave(submission);
                }
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable hideSubmission(RedditClient redditClient,
                                      Submission submission,
                                      boolean hide) {
        return Completable.create(e -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                accountManager.hide(hide, submission);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<SubscriberCount> getSubscriberCount(
            final RedditClient redditClient,
            final String subreddit) {
        return Single.create(e -> {
            try {
                Subreddit rnba = redditClient.getSubreddit(subreddit);
                Long subscribers = rnba.getSubscriberCount();
                int activeUsers = rnba.getAccountsActive();

                e.onSuccess(new SubscriberCount(subscribers, activeUsers));
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable userlessAuthentication(final RedditClient reddit,
                                              final Credentials credentials) {
        return Completable.create(e -> {
            try {
                OAuthData oAuthData = reddit.getOAuthHelper().easyAuth(credentials);
                reddit.authenticate(oAuthData);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable userAuthentication(final RedditClient reddit, final Credentials credentials,
                                          final String url) {
        return Completable.create(e -> {
            OAuthHelper oAuthHelper = reddit.getOAuthHelper();

            try {
                OAuthData oAuthData = oAuthHelper.onUserChallenge(url, credentials);
                reddit.authenticate(oAuthData);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable refreshToken(final RedditClient reddit, final Credentials credentials,
                                          final String refreshToken) {
        return Completable.create(e -> {
            OAuthHelper helper = reddit.getOAuthHelper();
            helper.setRefreshToken(refreshToken);

            try {
                OAuthData oAuthData = helper.refreshToken(credentials);
                reddit.authenticate(oAuthData);
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Completable deAuthenticate(final RedditClient reddit, final Credentials credentials) {
        return Completable.create(e -> {
            OAuthHelper helper = reddit.getOAuthHelper();
            try {
                helper.revokeAccessToken(credentials);
                reddit.deauthenticate();
                e.onComplete();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
        });
    }

    @Override
    public Single<List<CommentNode>> loadMoreComments(RedditClient reddit, CommentNode
            commentNode) {
        return Single.create(e -> e.onSuccess(commentNode.loadMoreComments(reddit)));
    }

    @Override
    public Single<MultiReddit> getMultiReddit(RedditClient reddit, String owner, String multi) {
        return Single.create(e -> {
            MultiRedditManager multiRedditManager = new MultiRedditManager(reddit);
            e.onSuccess(multiRedditManager.get(owner, multi));
        });
    }
}
