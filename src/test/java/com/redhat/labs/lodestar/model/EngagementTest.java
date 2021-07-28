package com.redhat.labs.lodestar.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.util.DateFormatter;

class EngagementTest {
    private String currentTime = "2021-05-25T00:00:00.000Z";
    
    @Test
    void testEngagementUpcoming() {
        String pastTime = "2021-04-08T00:00:00.000Z";
        
        Engagement notLaunched = new Engagement();
        
        LocalDateTime currentTimeLocal = DateFormatter.getInstance().getDateTime(currentTime);
        
        Assertions.assertEquals(Engagement.EngagementState.UPCOMING, notLaunched.getEngagementCurrentState(currentTimeLocal));
        
        notLaunched.setLaunch(new Launch());
        Assertions.assertEquals(Engagement.EngagementState.UPCOMING, notLaunched.getEngagementCurrentState(currentTimeLocal));
        
        notLaunched.setStartDate(pastTime);
        Assertions.assertEquals(Engagement.EngagementState.UPCOMING, notLaunched.getEngagementCurrentState(currentTimeLocal));
        
        notLaunched.setEndDate(pastTime);
        notLaunched.setStartDate(null);
        Assertions.assertEquals(Engagement.EngagementState.UPCOMING, notLaunched.getEngagementCurrentState(currentTimeLocal));
    }
    
    @Test
    void testEngagementPast() {
        LocalDateTime currentTimeLocal = DateFormatter.getInstance().getDateTime(currentTime);
        LocalDateTime startTimeLocal = currentTimeLocal.minus(60, ChronoUnit.DAYS);
        LocalDateTime endTimeLocal = currentTimeLocal.minus(30, ChronoUnit.DAYS);
        LocalDateTime archiveTimeLocal = currentTimeLocal.minus(1, ChronoUnit.DAYS);
        
        Engagement past = Engagement.builder().startDate(startTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .endDate(endTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .archiveDate(archiveTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .launch(new Launch()).build();
        
        Assertions.assertEquals(Engagement.EngagementState.PAST, past.getEngagementCurrentState(currentTimeLocal));
        
        past.setArchiveDate(null);
        Assertions.assertEquals(Engagement.EngagementState.PAST, past.getEngagementCurrentState(currentTimeLocal));
    }
    
    @Test
    void testEngagementTerminating() {
        LocalDateTime currentTimeLocal = DateFormatter.getInstance().getDateTime(currentTime);
        LocalDateTime startTimeLocal = currentTimeLocal.minus(60, ChronoUnit.DAYS);
        LocalDateTime endTimeLocal = currentTimeLocal.minus(30, ChronoUnit.DAYS);
        LocalDateTime archiveTimeLocal = currentTimeLocal.plus(1, ChronoUnit.DAYS);
        
        Engagement terminating = Engagement.builder().startDate(startTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .endDate(endTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .archiveDate(archiveTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .launch(new Launch()).build();
        
        Assertions.assertEquals(Engagement.EngagementState.TERMINATING, terminating.getEngagementCurrentState(currentTimeLocal));
    }
    
    @Test
    void testEngagementActive() {
        LocalDateTime currentTimeLocal = DateFormatter.getInstance().getDateTime(currentTime);
        LocalDateTime startTimeLocal = currentTimeLocal.minus(60, ChronoUnit.DAYS);
        LocalDateTime endTimeLocal = currentTimeLocal.plus(15, ChronoUnit.DAYS);
        LocalDateTime archiveTimeLocal = currentTimeLocal.plus(30, ChronoUnit.DAYS);
        
        Engagement active = Engagement.builder().startDate(startTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .endDate(endTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .archiveDate(archiveTimeLocal.format(DateFormatter.getInstance().getDateFormat()))
                .launch(new Launch()).build();
        
        Assertions.assertEquals(Engagement.EngagementState.ACTIVE, active.getEngagementCurrentState(currentTimeLocal));
    }

}
