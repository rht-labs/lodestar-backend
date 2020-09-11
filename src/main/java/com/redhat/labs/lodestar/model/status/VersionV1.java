package com.redhat.labs.lodestar.model.status;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionV1 {

    private String name;
    private String value;
    @JsonbProperty("link_address")
    private String linkAddress;

}
