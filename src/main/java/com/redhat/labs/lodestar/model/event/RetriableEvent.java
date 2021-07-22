package com.redhat.labs.lodestar.model.event;

import com.redhat.labs.lodestar.model.Engagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetriableEvent {

    @Builder.Default
    private Integer maxRetryCount = -1;

    @Builder.Default
    private Integer currentRetryCount = 0;

    private Engagement engagement;

    public void incrementCurrentRetryCount() {
        currentRetryCount = currentRetryCount + 1;
    }

    public boolean shouldRetry() {

        if (-1 == maxRetryCount) {
            return true;
        }

        return currentRetryCount < maxRetryCount;

    }

}
