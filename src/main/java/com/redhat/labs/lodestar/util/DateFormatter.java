package com.redhat.labs.lodestar.util;

import com.redhat.labs.lodestar.exception.mapper.BackendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * This class exists because the resource doesn't like a public static instance variable being access and
 * the refresh doesn't work when using a static method
 * @return
 */
public class DateFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateFormatter.class);
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
        List<DateTimeFormatter> formats = List.of(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_INSTANT);

        for(DateTimeFormatter format : formats) {
            try {
                return LocalDateTime.parse(dateTime, format);
            } catch (DateTimeParseException dpe) {
                LOGGER.error("date format error {} for {} - {}", dateTime, format, dpe.getMessage());
            }

        }
        throw new BackendException( "Unsupported date format " + dateTime);
    }

}
