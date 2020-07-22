package com.redhat.labs.omp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionDetail {

    private String name;
    private String value;

    public static VersionDetail fromVersion(Version version) {
        return VersionDetail.builder().name(version.getApplication()).value(version.getVersion()).build();
    }

}
