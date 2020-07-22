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

    public static VersionDetail fromVersionV1(Version versionV1) {
        return VersionDetail.builder().name(versionV1.getApplication()).value(versionV1.getVersion()).build();
    }

}
