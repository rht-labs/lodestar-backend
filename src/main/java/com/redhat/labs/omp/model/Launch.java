package com.redhat.labs.omp.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Launch {

    @JsonbProperty("launched_date_time")
    private String launchedDateTime;
    @JsonbProperty("launched_by")
    private String launchedBy;
    @JsonbProperty("launched_by_email")
    private String launchedByEmail;

}
