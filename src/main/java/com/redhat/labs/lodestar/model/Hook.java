package com.redhat.labs.lodestar.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hook {

    private String objectKind;
    private String eventName;
    private Integer projectId;
    private List<Commit> commits;
    private GitlabProject project;
    private String groupId;

    public boolean didFileChange(String fileName) {
        for (Commit commit : commits) {
            if (commit.didFileChange(fileName)) {
                return true;
            }
        }

        return false;
    }

    public String getCustomerName() {
        return project.getCustomerNameFromName();

    }

    public String getEngagementName() {
        return project.getEngagementNameFromName();
    }

    public boolean wasProjectDeleted() {
        return "project_deleted".equals(eventName);
    }

    public boolean refreshEngagement() {
        return commits.stream().anyMatch(c -> "manual_refresh".equalsIgnoreCase(c.getMessage()));
    }

}
