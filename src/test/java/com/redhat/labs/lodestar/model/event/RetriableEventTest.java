package com.redhat.labs.lodestar.model.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Engagement;

class RetriableEventTest {

    @Test
    void testShouldRetry() {

        RetriableEvent event = RetriableEvent.builder().engagement(Engagement.builder().build()).build();
        assertTrue(event.shouldRetry());

    }

    @Test
    void testShouldRetryMaxReached() {

        RetriableEvent event = RetriableEvent.builder().maxRetryCount(2).engagement(Engagement.builder().build()).build();
        assertTrue(event.shouldRetry());
        event.incrementCurrentRetryCount();
        event.incrementCurrentRetryCount();
        assertFalse(event.shouldRetry());

    }    

}
