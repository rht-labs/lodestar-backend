package com.redhat.labs.lodestar.model.pagination;

import com.redhat.labs.lodestar.model.HostingEnvironment;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class PagedHostingEnvironmentResults extends PagedResults<HostingEnvironment> {

}