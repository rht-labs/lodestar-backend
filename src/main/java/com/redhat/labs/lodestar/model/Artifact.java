package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {

    private String title;
    private String type;
    private String linkAddress;

}