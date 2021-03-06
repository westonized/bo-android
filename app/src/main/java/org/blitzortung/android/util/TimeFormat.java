package org.blitzortung.android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class TimeFormat {

    private TimeFormat(){}

	private static final SimpleDateFormat DATE_TIME_MILLISECONDS_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss.SSS");
	static {
		DATE_TIME_MILLISECONDS_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	private static final SimpleDateFormat JSON_DATE_TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
	static {
		JSON_DATE_TIME_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

    public static long parseTimeWithMilliseconds(String timestampString) {
		try {
			return DATE_TIME_MILLISECONDS_FORMATTER.parse(timestampString).getTime();
		} catch (ParseException e) {
            throw new IllegalArgumentException(String.format("Unable to parse millisecond time string '%s'", timestampString), e);
        }
	}

    public static long parseTimestampWithMillisecondsFromFields(String[] fields) {
        String timeString = fields[0].replace("-", "") + "T" + fields[1];
        return parseTimeWithMilliseconds(timeString.substring(0, timeString.length() - 6));
    }
	
	public static long parseTime(String timestampString) {
		try {
			return JSON_DATE_TIME_FORMATTER.parse(timestampString).getTime();
		} catch (ParseException e) {
            throw new IllegalArgumentException(String.format("Unable to parse time string '%s'", timestampString), e);
		}
	}
}