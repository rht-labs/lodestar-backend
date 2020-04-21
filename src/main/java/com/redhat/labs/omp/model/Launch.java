package com.redhat.labs.omp.model;

import java.time.LocalDateTime;

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
    private LocalDateTime launchedDateTime;
    @JsonbProperty("launched_by")
    private String launchedBy;

}
