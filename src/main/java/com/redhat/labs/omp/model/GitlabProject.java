package com.redhat.labs.omp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabProject {
    private static final String REGEX = "(.*) \\/ (.*) \\/ (.*) \\/ iac";

    private String pathWithNamespace;
    private String nameWithNamespace;
    
    public String getCustomerNameFromName() {
        return nameWithNamespace.replaceAll(REGEX, "$2");
    }
    
    public String getEngagementNameFromName() {
        return nameWithNamespace.replaceAll(REGEX, "$3");
    }
}
