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
public class Commit {
    private String message;
    private String title;
    
    private List<String> added;
    private List<String> modified;
    private List<String> removed;
    
    
    public boolean didFileChange(String fileName) {
        return added.contains(fileName) || modified.contains(fileName) || removed.contains(fileName);
    }
}
