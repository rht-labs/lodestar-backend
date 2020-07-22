package com.redhat.labs.omp.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionSummary {

    private VersionDetail mainVersion;
    @Builder.Default
    private List<VersionDetail> componentVersions = new ArrayList<>();

}
