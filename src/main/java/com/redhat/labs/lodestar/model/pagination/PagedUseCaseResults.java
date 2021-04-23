package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.UseCase;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedUseCaseResults extends PagedResults<UseCase> {

}