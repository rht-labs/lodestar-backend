package com.redhat.labs.lodestar.model.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactOptions {
    
    @Parameter(name = "engagementUuid", required = false, description = "return only artifacts for the given engagement uuid")
    @QueryParam("engagementUuid")
    private String engagementUuid;
    
    @Parameter(name = "type", required = false, description = "return only artifacts for the given type. Do not use with engagementUuid")
    @QueryParam("type")
    private String type;
    
    @Parameter(name = "region", required = false, description = "return only artifacts for the given region. Do not use with engagementUuid")
    @QueryParam("region")
    @Builder.Default
    private List<String> region = new ArrayList<>();
    
    public Optional<String> getEngagementUuid() {
        return Optional.ofNullable(engagementUuid);
    }
    
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }
    
    @Parameter(name = "page", required = false, description = "page number of results to return")
    @QueryParam("page")
    private Integer page;

    @Parameter(name = "pageSize", required = false, description = "number of results per page to return")
    @QueryParam("pageSize")
    private Integer pageSize;
}
