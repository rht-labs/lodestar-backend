package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.Score;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedScoreResults extends PagedResults<Score> {

}