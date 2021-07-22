package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementUserSummary {

    private Integer allUsersCount;
    private Integer rhUsersCount;
    private Integer otherUsersCount;
}
