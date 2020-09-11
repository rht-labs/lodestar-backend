package com.redhat.labs.lodestar.model;

import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

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
    private List<Message> messages;
    @JsonbProperty("access_urls")
    private List<Map<String, Object>> accessUrls;

}
