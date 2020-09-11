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
public class VersionManifestV1 {

    private VersionV1 mainVersion;
    private List<VersionV1> componentVersions;

}
