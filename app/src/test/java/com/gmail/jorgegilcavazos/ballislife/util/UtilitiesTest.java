package com.gmail.jorgegilcavazos.ballislife.util;

import com.gmail.jorgegilcavazos.ballislife.features.model.SubmissionWrapper;
import com.google.common.base.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilitiesTest {

    @Test
    public void testGetPeriodString() {
        String firstQtr = Utilities.getPeriodString("1", "Qtr");
        String fourthQtr = Utilities.getPeriodString("4", "Qtr");
        String overTime1 = Utilities.getPeriodString("5", "OT");
        String overTime2 = Utilities.getPeriodString("6", "OT");
        String overTime6 = Utilities.getPeriodString("10", "OT");

        assertEquals("Q1", firstQtr);
        assertEquals("Q4", fourthQtr);
        assertEquals("OT1", overTime1);
        assertEquals("OT2", overTime2);
        assertEquals("OT6", overTime6);
    }

    @Test
    public void testGetPeriodString_empty() {
        assertEquals(Utilities.getPeriodString("", "Qtr"), "");
    }

    @Test
    public void testGetStreamableShortcodeFromUrl() {
        String url1 = "http://streamable.com/12345";
        String url2 = "streamable.com/ft67e";
        String url3 = "http://streamable.com/a23r";
        String url4 = "http://google.com/12345";

        assertEquals("12345", Utilities.getStreamableShortcodeFromUrl(url1));
        assertEquals("ft67e", Utilities.getStreamableShortcodeFromUrl(url2));
        assertEquals("a23r", Utilities.getStreamableShortcodeFromUrl(url3));
        assertEquals(null, Utilities.getStreamableShortcodeFromUrl(url4));
    }

    @Test
    public void testGetThumbnailToShowFromCustomSubmission() {
        SubmissionWrapper submissionWrapper = new SubmissionWrapper("", null, "", "");
        submissionWrapper.setThumbnail("sdThumbnail");
        submissionWrapper.setHighResThumbnail("hdThumbnail");
        SubmissionWrapper submissionWrapper1 = new SubmissionWrapper("", null, "", "");
        submissionWrapper1.setThumbnail("sdThumbnail");
        SubmissionWrapper submissionWrapper2 = new SubmissionWrapper("", null, "", "");

        Optional<Pair<Utilities.ThumbnailType, String>> optional = Utilities
                .getThumbnailToShowFromCustomSubmission(submissionWrapper);
        Optional<Pair<Utilities.ThumbnailType, String>> optional1 = Utilities
                .getThumbnailToShowFromCustomSubmission(submissionWrapper1);
        Optional<Pair<Utilities.ThumbnailType, String>> optional2 = Utilities
                .getThumbnailToShowFromCustomSubmission(submissionWrapper2);

        assertTrue(optional.isPresent());
        assertEquals("hdThumbnail", optional.get().second);
        assertTrue(optional1.isPresent());
        assertEquals("sdThumbnail", optional1.get().second);
        assertFalse(optional2.isPresent());
    }

    @Test
    public void getYoutubeVideoIdFromUrl() {
        String url1 = "https://youtu.be/LEOODbUCge";
        String url2 = "https://streamable.com/jcb6p";
        String url3 = "https://www.youtube.com/watch?v=cwlx4QyCTBU";
        String url4 = "https://www.youtube.com/watch?v=cwlx4QyCTBU&index=5&ts=23455";
        String url5 = "https://www.youtube.com/watch?v=Jxrn2CO092w#t=0m20s";

        assertEquals("LEOODbUCge", Utilities.getYoutubeVideoIdFromUrl(url1));
        assertEquals(null, Utilities.getYoutubeVideoIdFromUrl(url2));
        assertEquals("cwlx4QyCTBU", Utilities.getYoutubeVideoIdFromUrl(url3));
        assertEquals("cwlx4QyCTBU", Utilities.getYoutubeVideoIdFromUrl(url4));
        assertEquals("Jxrn2CO092w", Utilities.getYoutubeVideoIdFromUrl(url5));
    }
}
