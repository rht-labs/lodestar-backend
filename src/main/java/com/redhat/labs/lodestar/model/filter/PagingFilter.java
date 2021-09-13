package com.redhat.labs.lodestar.model.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagingFilter {

    @DefaultValue("0")
    @Parameter(description = "page number of results to return")
    @QueryParam("page")
    private int page;

    @DefaultValue("500")
    @Parameter(description = "page size")
    @QueryParam("pageSize")
    private int pageSize;
}
