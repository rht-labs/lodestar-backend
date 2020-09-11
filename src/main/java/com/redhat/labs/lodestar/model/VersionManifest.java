package com.redhat.labs.lodestar.model;

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
public class VersionManifest {

    private List<Version> containers;
    private List<Version> applications;

    public void addContainer(Version version) {
        if(containers == null) {
            containers = new ArrayList<>();
        }
        containers.add(version);
    }

    public void clearAndAddContainer(Version version) {
        if(containers == null) {
            containers = new ArrayList<>();
        } else {
            containers.clear();
        }
        containers.add(version);

    }
}
