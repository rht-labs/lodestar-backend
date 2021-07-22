package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.Engagement;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedEngagementResults extends PagedResults<Engagement> {

}
