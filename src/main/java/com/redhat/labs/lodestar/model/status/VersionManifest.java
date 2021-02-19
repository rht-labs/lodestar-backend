package com.redhat.labs.lodestar.model.status;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionManifest {

    private Version mainVersion;
    private List<Version> componentVersions;

}
