package com.redhat.labs.lodestar.model;

import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {

    @JsonbProperty("overall_status")
    private String status;
    private List<Message> messages;
    private List<Subsystem> subsystems;

}
