package com.redhat.labs.lodestar.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementArtifact;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.rest.client.ArtifactApiClient;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class ArtifactService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactService.class);
    
    @Inject
    @RestClient
    ArtifactApiClient artifactRestClient;
    
    @Inject
    EngagementService engagementService;
    
    public Response getArtifacts(ListFilterOptions filterOptions, String engagementUuid, String type, boolean dashboardView) {
        int page = 0;
        int pageSize = 100;
        
        if(filterOptions.getPage().isPresent()) {
            page = filterOptions.getPage().get() - 1;
        }
        
        if(filterOptions.getPerPage().isPresent()) {
            pageSize = filterOptions.getPerPage().get();
        }
        

        ArtifactOptions options = ArtifactOptions.builder().page(page).pageSize(pageSize)
                .engagementUuid(engagementUuid).type(type).build(); //1 based vs 0 based. Should stabilize around 0 based

        
        Response response = artifactRestClient.getArtifacts(options);
        
        int currentPage = page + 1; // 0 based vs 1 based. Need to rectify at some point
        int totalArtifacts = Integer.parseInt(response.getHeaderString("x-total-artifacts"));
        int totalPages = totalArtifacts / pageSize + 1;
        List<EngagementArtifact>  artifacts = response.readEntity(new GenericType<List<EngagementArtifact>>(){});
        
        if(dashboardView) { //enrich data with customer and engagement name
            for(EngagementArtifact artifact : artifacts) {
                Engagement e = engagementService.getByUuid(artifact.getEngagementUuid(), new FilterOptions("customerName,projectName", null));
                artifact.setCustomerName(e.getCustomerName());
                artifact.setProjectName(e.getProjectName());
                
            }
        }
        
        return Response.ok(artifacts).header("x-current-page", currentPage).header("x-per-page", pageSize)
                .header("x-total-artifacts", totalArtifacts).header("x-next-page", currentPage + 1)
                .header("x-total-pages", totalPages).build();
        
    }
    
    @ConsumeEvent(value = EventType.UPDATE_ARTIFACTS_EVENT_ADDRESS, blocking = true)
    public void sendUpdate(String message) {

        String[] uuidNameEmail = message.split(",");
        try {
            Engagement engagement = engagementService.getByUuid(uuidNameEmail[0], new FilterOptions());
            Response response = artifactRestClient.updateArtifacts(uuidNameEmail[0], engagement.getArtifacts(), uuidNameEmail[1], uuidNameEmail[2]);
            LOGGER.debug("Artifact update response for {} is {}", uuidNameEmail[0], response.getStatus());
        } catch (WebApplicationException wae) {
            LOGGER.error("Failed to update artifacts for engagement {} {}", wae.getResponse().getStatus(), message);
        } catch (RuntimeException wae) {
            LOGGER.error("Failed to update artifacts for engagement {}", message, wae);
        }
    }
    
    @ConsumeEvent(value = EventType.RELOAD_ARTIFACTS_EVENT_ADDRESS, blocking = true)
    public void refesh(String message) {
        try {
            LOGGER.debug("refresh {}", message);
            artifactRestClient.refreshArtifacts();
            LOGGER.debug("refresh {} completed", message);
        } catch (WebApplicationException wae) { //without catching this it will fail silently
            LOGGER.error("Failed to refresh artifacts {}", wae.getResponse(), wae);
        }
    }
    
    
}
