package com.redhat.labs.lodestar.model.pagination;

import java.util.List;

import com.redhat.labs.lodestar.model.Artifact;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagedArtifactResults extends PagedResults {

    private List<Artifact> results;

}
