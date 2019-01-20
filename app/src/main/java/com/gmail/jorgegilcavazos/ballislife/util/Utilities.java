package com.gmail.jorgegilcavazos.ballislife.util;

import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.google.common.base.Optional;

import net.dean.jraw.models.Submission;

public final class Utilities {

    public static String getPeriodString(String periodValue, String periodName) {
        if (periodValue.isEmpty()) {
            return periodValue;
        }
        int period = Integer.parseInt(periodValue);
        int overtimePeriod = period - 4;
        if (period <= 4) {
            return "Q" + period;
        } else {
            return "OT" + overtimePeriod;
        }
    }

    public static String getStreamableShortcodeFromUrl(String url) {
        final String streamableUrl = "streamable.com/";

        int i = url.indexOf(streamableUrl);

        if (i == -1) {
            return null;
        }

        return url.substring(i + streamableUrl.length());
    }

    public static String getYoutubeVideoIdFromUrl(String url) {
        if (url.contains("youtu.be")) {
            return url.substring(url.lastIndexOf("/") + 1);
        } else if (url.contains("youtube.com")) {
            String a = url.substring(url.lastIndexOf("v=") + 2);
            if (a.contains("&")) {
                return a.substring(0, a.indexOf("&"));
            } else if (a.contains("#")) {
                return a.substring(0, a.indexOf("#"));
            } else {
                return a;
            }
        } else {
            return null;
        }
    }

    // Get data from real submission if available, otherwise used data from fake one.
    public static Optional<Pair<ThumbnailType, String>> getThumbnailToShowFromCustomSubmission
    (SubmissionWrapper submissionWrapper) {
        String thumbnail;
        String highResThumbnail;
        if (submissionWrapper.getSubmission() == null) {
            thumbnail = submissionWrapper.getThumbnail();
            highResThumbnail = submissionWrapper.getHighResThumbnail();
        } else {
            Submission submission = submissionWrapper.getSubmission();
            thumbnail = submission.getThumbnail();
            try {
                highResThumbnail = submission.getOEmbedMedia().getThumbnail().getUrl()
                        .toExternalForm().replace("&amp;", "&");
            } catch (NullPointerException e) {
                highResThumbnail = null;
            }
        }

        if (StringUtils.Companion.isNullOrEmpty(thumbnail) && StringUtils.Companion.isNullOrEmpty
                (highResThumbnail)) {
            return Optional.absent();
        }

        // Show HD thumbnail over lower res version.
        if (StringUtils.Companion.isNullOrEmpty(highResThumbnail)) {
            return Optional.of(new Pair<>(ThumbnailType.LOW_RES, thumbnail));
        } else {
            return Optional.of(new Pair<>(ThumbnailType.HIGH_RES, highResThumbnail));
        }
    }

    public enum ThumbnailType {
        LOW_RES,
        HIGH_RES
    }
}
