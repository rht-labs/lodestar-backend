package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.bind.annotation.JsonbProperty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProject {

    private String code;
    @JsonbProperty("tasks")
    private List<BillingTask> billingTasks;
}
