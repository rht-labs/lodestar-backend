package com.redhat.labs.omp.model;

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
public class File {

    @NotBlank
    @JsonbProperty("file_path")
    private String filePath;
    @NotBlank
    @JsonbProperty("branch")
    private String branch;
    @NotBlank
    @JsonbProperty("content")
    private String content;
    @NotBlank
    @JsonbProperty("commit_message")
    private String commitMessage;

}
