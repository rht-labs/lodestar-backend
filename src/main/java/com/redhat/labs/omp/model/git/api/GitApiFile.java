package com.redhat.labs.omp.model.git.api;

import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitApiFile {

    @NotBlank
    @JsonbProperty("file_path")
    private String filePath;
    @NotBlank
    private String branch;
    @NotBlank
    private String content;
    @NotBlank
    @JsonbProperty("commit_message")
    private String commitMessage;
    @JsonbProperty("author_email")
    private String authorEmail;
    @JsonbProperty("author_name")
    private String authorName;

}
