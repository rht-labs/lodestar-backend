package com.redhat.labs.omp.model;

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
    private String gitCommit;
    private String gitTag;
    private String version;
}
