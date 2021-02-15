package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {

    private String id;
    private String title;
    private String description;
    private String type;
    private String linkAddress;


}