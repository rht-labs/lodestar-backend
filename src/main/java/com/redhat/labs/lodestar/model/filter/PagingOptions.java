package com.redhat.labs.lodestar.model.filter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PagingOptions {

    @Parameter(description = "0 based page number")
    @QueryParam("page")
    @DefaultValue("0")
    private int page;
    
    @QueryParam("pageSize")
    @DefaultValue("100")
    private int pageSize;
    
}
