package com.redhat.labs.omp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.cache.EngagementDataCache;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class EngagementService {

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    @Inject
    EngagementDataCache engagementCache;

    public Response createEngagement(Engagement engagement, UriInfo uriInfo) {

        // use rest client to call git api service
        Response gitResponse = gitApi.createEngagement(engagement);

        if(gitResponse.getStatus() == 201) {
            String location = gitResponse.getHeaderString("Location");
            String projectId = location.substring(location.lastIndexOf("/"));

            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(projectId);
            return Response.created(builder.build()).build();
        }

        return gitResponse;

    }
    
}
