package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.Artifact;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedArtifactResults extends PagedResults<Artifact> {

}
