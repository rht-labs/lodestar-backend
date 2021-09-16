package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//Legacy
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EngagementUserSummary {

    private Long allUsersCount;
    private Long rhUsersCount;
    private Long otherUsersCount;
}
