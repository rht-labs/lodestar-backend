package com.redhat.labs.omp.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Version {
    
    private String application;
    @JsonbProperty("git_commit")
    private String gitCommit;
    @JsonbProperty("git_tag")
    private String gitTag;
    private String version;
}
