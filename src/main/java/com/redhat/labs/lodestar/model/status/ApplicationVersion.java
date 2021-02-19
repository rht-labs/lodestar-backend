package com.redhat.labs.lodestar.model.status;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationVersion {
    
    private String application;
    @JsonbProperty("git_commit")
    private String gitCommit;
    @JsonbProperty("git_tag")
    private String gitTag;
    private String version;
}
