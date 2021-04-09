package com.redhat.labs.lodestar.model.search;

import java.util.Arrays;

public enum EngagementState {
    UPCOMING, ACTIVE, PAST, TERMINATING;

    public static EngagementState lookup(String value) {
        return Arrays.asList(EngagementState.values()).stream().filter(e -> e.name().equalsIgnoreCase(value)).findAny()
                .orElse(null);
    }

}
