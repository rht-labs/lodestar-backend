package com.redhat.labs.omp.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subsystem {

    private String name;
    private String status;
    private String state;
    private String info;
    private String updated;
    private String webConsole;
    private String api;
    private List<Message> messages;

}
