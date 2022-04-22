package com.redhat.labs.lodestar.model.filter;

import lombok.*;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementFilterOptions {

    @Parameter(name = "Accept-version", description = "Valid Values are 'v1' or 'v2'. v2 pages results by default.  v1 sets per page to 500.")
    @HeaderParam(value = "Accept-version")
    String apiVersion;

    @Parameter(name = "search", description = "Deprecated. search string used to query engagements.  allows =, not like, like, not exists, exists")
    @QueryParam("search")
    @Deprecated
    private String search;

    @Parameter(name = "q", description = "Free search string. Currently supports Engagement and Customer names.")
    @QueryParam("q")
    private String q;

    @Parameter(description = "sort value. Default Dir to ASC. Ex. field1|DESC,field2,field3|DESC. Always last sort by uuid")
    @QueryParam("sortFields")
    @DefaultValue("lastUpdate|desc")
    private String sortFields;

    @Parameter(name = "page", description = "page to be returned. Starts at 1")
    @QueryParam("page")
    @DefaultValue("1")
    private Integer page;

    @Parameter(name = "perPage", description = "number of results per page to return")
    @QueryParam("perPage")
    @DefaultValue("1000")
    private Integer perPage;

    @Parameter(name = "regions", description = "include only these regions. All regions if empty")
    @QueryParam("regions")
    private Set<String> regions;

    @Parameter(name = "types", description = "include only these types. All types if empty")
    @QueryParam("types")
    private Set<String> types;

    @Parameter(name = "states", description = "include only these states. All states if empty")
    @QueryParam("states")
    private Set<String> states;

    @Parameter(name = "category", description = "find by category")
    @QueryParam("category")
    private String category;

    //Relic
    public Set<String> getV2Regions() {
        return regions;
    }
}
