package com.redhat.labs.lodestar.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class exists because the resource doesn't like a public static instance variable being access and
 * the refresh doesn't work when using a static method
 * @return
 */
public class DateFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static DateFormatter formatter = new DateFormatter();
    
    private DateFormatter() {}
    
    public static DateFormatter getInstance() {
        return formatter;
    }
    
    public DateTimeFormatter getDateFormat() {
        return DATE_FORMAT;
    }
    
    public LocalDateTime getDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_FORMAT);
    }

}
