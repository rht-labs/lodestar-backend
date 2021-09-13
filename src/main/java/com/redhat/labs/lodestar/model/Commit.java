package com.redhat.labs.lodestar.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Commit {
    private String id;
    private String message;
    private String title;
    
    @Builder.Default private List<String> added = new ArrayList<>();
    @Builder.Default private List<String> modified = new ArrayList<>();
    @Builder.Default private List<String> removed = new ArrayList<>();
    
    @JsonbProperty("short_id")
    private String shortId;
    @JsonbProperty("author_name")
    private String authorName;
    @JsonbProperty("author_email")
    private String authorEmail;
    @JsonbProperty("committer_name")
    private String commiterName;
    @JsonbProperty("committer_email")
    private String commiterEmail;
    @JsonbProperty("authored_date")
    private String authoredDate;
    @JsonbProperty("committed_date")
    private String commitDate;
    @JsonbProperty("web_url")
    private String url;
    @JsonbProperty("engagement_uuid")
    private String engagementUuid;
    
    
    public boolean didFileChange(List<String> fileName) {
        Set<String> changedFiles = new HashSet<>(added);
        changedFiles.addAll(modified);
        changedFiles.addAll(removed);
        
        return changedFiles.stream().filter(fileName::contains).count() > 0;
    }
}
