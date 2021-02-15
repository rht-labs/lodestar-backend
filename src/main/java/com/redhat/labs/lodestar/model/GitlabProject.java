package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabProject {
    private static final String NAME_REGEX = "(.*)\\/ (.*) \\/ (.*) \\/ iac";
    private static final String PATH_REGEX = "(.*)\\/(.*)\\/(.*)\\/iac";

    private String pathWithNamespace;
    private String nameWithNamespace;
    
    public String getCustomerNameFromName() {
    	if(nameWithNamespace == null) {
    		return pathWithNamespace.replaceAll(PATH_REGEX, "$2");
    	}
        return nameWithNamespace.replaceAll(NAME_REGEX, "$2");
    }
    
    public String getEngagementNameFromName() {
    	if(nameWithNamespace == null) {
    		return pathWithNamespace.replaceAll(PATH_REGEX, "$3");
    	}
        return nameWithNamespace.replaceAll(NAME_REGEX, "$3");
    }
}
