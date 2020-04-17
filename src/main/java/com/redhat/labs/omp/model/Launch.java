package com.redhat.labs.omp.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Launch {

    private LocalDateTime launchedDateTime;
    private String launchedBy;

}
