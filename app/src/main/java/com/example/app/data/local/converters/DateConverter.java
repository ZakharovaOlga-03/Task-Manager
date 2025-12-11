package com.example.app.data.local.converters;

import androidx.room.TypeConverter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateConverter {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static final SimpleDateFormat DATE_ONLY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @TypeConverter
    public static Date fromTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            try {
                return DATE_ONLY_FORMAT.parse(value);
            } catch (ParseException ex) {
                return null;
            }
        }
    }

    @TypeConverter
    public static String dateToTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }
}