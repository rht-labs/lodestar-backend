package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ErrorMessage {
    
    String message;
    
    public ErrorMessage(String messasge, Object... substitutions) {
        this.message = String.format(messasge, substitutions);
    }
}
