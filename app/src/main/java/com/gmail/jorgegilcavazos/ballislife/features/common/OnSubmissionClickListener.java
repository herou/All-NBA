package com.gmail.jorgegilcavazos.ballislife.features.common;

import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;

import net.dean.jraw.models.VoteDirection;

public interface OnSubmissionClickListener {

    void onSubmissionClick(String submissionId);

    void onVoteSubmission(SubmissionWrapper submission, VoteDirection voteDirection);

    void onSaveSubmission(SubmissionWrapper submission, boolean saved);

    void onContentClick(String url);

    void onHideSubmission(SubmissionWrapper submissionWrapper);
}
