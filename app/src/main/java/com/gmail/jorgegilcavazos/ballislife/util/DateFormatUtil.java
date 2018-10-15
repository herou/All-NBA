package com.gmail.jorgegilcavazos.ballislife.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods used in various points of the application.
 */
public final class DateFormatUtil {
    private static final String TAG = "DateFormatUtil";
    public static final int TIME_UNIT_JUST_NOW = 0;
    public static final int TIME_UNIT_MINUTES = 1;
    public static final int TIME_UNIT_HOURS = 2;
    public static final int TIME_UNIT_DAYS = 3;

    /**
     * Receives a Date object and returns a human-readable string, e.g. "5m ago".
     */
    public static String formatRedditDate(Date date) {
        String postedOn;
        Date now = new Date();
        long minutesAgo = (TimeUnit.MILLISECONDS.toMinutes(now.getTime() - date.getTime()));
        long hoursAgo = (TimeUnit.MILLISECONDS.toHours(now.getTime() - date.getTime()));
        long daysAgo = (TimeUnit.MILLISECONDS.toDays(now.getTime() - date.getTime()));

        if (minutesAgo == 0) {
            postedOn = " just now ";
        } else if (minutesAgo < 60) {
            postedOn = minutesAgo + "m";
        } else {
            if (hoursAgo < 49) {
                postedOn = hoursAgo + "hr";
            } else {
                postedOn = daysAgo + " days";
            }
        }

        return postedOn;
    }

    /**
     * Receives a Date object and returns a human-readable string, e.g. "5 minutes ago".
     */
    public static Pair<Integer, Optional<Long>> formatRedditDateLong(Date date) {
        Date now = new Date();
        long minutesAgo = (TimeUnit.MILLISECONDS.toMinutes(now.getTime() - date.getTime()));
        long hoursAgo = (TimeUnit.MILLISECONDS.toHours(now.getTime() - date.getTime()));
        long daysAgo = (TimeUnit.MILLISECONDS.toDays(now.getTime() - date.getTime()));

        if (minutesAgo == 0) {
            return new Pair<>(TIME_UNIT_JUST_NOW, Optional.absent());
        } else if (minutesAgo < 60) {
            return new Pair<>(TIME_UNIT_MINUTES, Optional.of(minutesAgo));
        } else {
            if (hoursAgo < 49) {
                return new Pair<>(TIME_UNIT_HOURS, Optional.of(hoursAgo));
            } else {
                return new Pair<>(TIME_UNIT_DAYS, Optional.of(daysAgo));
            }
        }
    }

    /**
     * Returns a dash-separated date given a year, month and day, e.g. "2016-03-20".
     */
    public static String formatScoreboardDate(int year, int month, int day) {
        String monthString;
        if (month < 10) {
            monthString = "0" + month;
        } else {
            monthString = String.valueOf(month);
        }

        String dayString;
        if (day < 10) {
            dayString = "0" + day;
        } else {
            dayString = String.valueOf(day);
        }

        return year + "-" + monthString + "-" + dayString;
    }

    /**
     * Returns a "month/day" formatted date given a "yyyymmdd" string, unless the given date is
     * today, in which case the string "Today" is returned.
     */
    public static String formatToolbarDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
            Date date = format.parse(dateString);
            if (isDateToday(date)) {
                return "Today";
            }
        } catch (ParseException e) {
            throw new RuntimeException("Un-parsable string " + dateString);
        }
        return dateString.substring(4, 6) + "/" + dateString.substring(6, 8);
    }

    /**
     * Returns a prettier date for the game date navigator, e.g. "Tuesday, October 25".
     */
    public static String formatNavigatorDate(Date date) {
        if (isDateToday(date)) {
            return "Today";
        } else if(isDateYesterday(date)) {
            return "Yesterday";
        } else if(isDateTomorrow(date)) {
            return "Tomorrow";
        } else {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
            return format.format(date);
        }

    }

    public static boolean isDateToday(Date date) {
        Calendar calNow = Calendar.getInstance();
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        return areDatesEqual(calNow, calDate);
    }

    public static boolean isDateYesterday(Date date) {
        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        return areDatesEqual(calYesterday, calDate);
    }

    public static boolean isDateTomorrow(Date date) {
        Calendar calTomorrow = Calendar.getInstance();
        calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        return areDatesEqual(calTomorrow, calDate);
    }

    public static boolean areDatesEqual(Calendar calendar1, Calendar calendar2) {
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
                && calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }

    public static String getNoDashDateString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return format.format(date);
    }

    public static String localizeGameTime(String dateETString) {
        return localizeGameTime(dateETString, TimeZone.getDefault());
    }

    /**
     * Returns localized date from game time String (e.g. 9:00 pm ET) -> 7:00 PM of timeZone
     */
    public static String localizeGameTime(@NonNull String dateETString, TimeZone timeZone) {
        try {
            String formattedDate = dateETString.replace(" ET", "");

            LocalTime timeWithoutTZ = DateTimeFormat.forPattern("hh:mm aa")
                    .withLocale(Locale.US).parseLocalTime(formattedDate);

            DateTime timeET = DateTime.now(DateTimeZone
                    .forTimeZone(TimeZone.getTimeZone("America/New_York")))
                    .withTime(timeWithoutTZ);

            DateTime localized = timeET.toDateTime(DateTimeZone.forTimeZone(timeZone));
            return localized.toString(DateTimeFormat.forPattern("h:mm aa"));
        } catch (Exception e) {
            Crashlytics.log(
                    Log.ERROR,
                    DateFormatUtil.class.getSimpleName(),
                    "Error localizing date " + dateETString
                            + " to timezone " + timeZone.toString());
            Crashlytics.logException(e);
            return dateETString;
        }
    }

    public static long getDateStartUtc(Calendar date) {
        Calendar dateStart = Calendar.getInstance();
        dateStart.setTimeInMillis(date.getTimeInMillis());
        dateStart.set(Calendar.HOUR_OF_DAY, 0);
        dateStart.set(Calendar.MINUTE, 0);
        dateStart.set(Calendar.SECOND, 0);
        dateStart.set(Calendar.MILLISECOND, 0);
        return dateStart.getTimeInMillis() / 1000;
    }

    public static long getDateEndUtc(Calendar date) {
        Calendar dateEnd = Calendar.getInstance();
        dateEnd.setTimeInMillis(date.getTimeInMillis());
        dateEnd.set(Calendar.HOUR_OF_DAY, 0);
        dateEnd.set(Calendar.MINUTE, 0);
        dateEnd.set(Calendar.SECOND, 0);
        dateEnd.set(Calendar.MILLISECOND, 0);
        dateEnd.add(Calendar.DAY_OF_YEAR, 1);
        dateEnd.add(Calendar.MINUTE, -1);
        return dateEnd.getTimeInMillis() / 1000;
    }

    public static long addHoursToTime(long timeUtc, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeUtc * 1000);
        calendar.add(Calendar.HOUR, hours);
        return calendar.getTimeInMillis() / 1000;
    }

    /**
     * Returns today's date formatted as "YYYY-MM-DD".
     */
    public static String getTodayForHighlights() {
        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(
                TimeZone.getTimeZone("America/New_York")));
        return DateTimeFormat.forPattern("yyyy-MM-dd").print(dateTime);
    }

    /**
     * Returns the current week of the year (1-52).
     */
    public static String getWeekForHighlights() {
        DateTime dateTime = new DateTime(DateTimeZone.forTimeZone(
                TimeZone.getTimeZone("America/New_York")));
        return String.valueOf(dateTime.getWeekOfWeekyear());
    }
}
