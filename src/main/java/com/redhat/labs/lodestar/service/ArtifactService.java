package com.redhat.labs.lodestar.service;

import java.util.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.redhat.labs.lodestar.model.Artifact;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementArtifact;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.ArtifactOptions;
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

    @Inject
    ConfigService configService;

    public List<Artifact> getArtifacts(String engagementUuid) {
        ArtifactOptions options = ArtifactOptions.builder().page(0).pageSize(1000)
                .engagementUuid(engagementUuid).build();

        try {
            return artifactRestClient.getArtifacts(options).readEntity(new GenericType<>(){});
        } catch (WebApplicationException wex) {
            if(wex.getResponse().getStatus() >= 500) {
                LOGGER.error("Artifact Server error ({}) from hosting env for euuid {}", wex.getResponse().getStatus(), engagementUuid);
                return Collections.emptyList();
            }
            throw wex;
        }
    }
    
    public Response getArtifacts(ListFilterOptions filterOptions, String engagementUuid, String type, List<String> region) {
        Optional<Integer> pageO = filterOptions.getPage();
        Optional<Integer> pageSizeO = filterOptions.getPerPage();
        int page = pageO.orElse(1);
        page--;
        int pageSize = pageSizeO.orElse(100);
        
        ArtifactOptions options = ArtifactOptions.builder().page(page).pageSize(pageSize)
                .engagementUuid(engagementUuid).type(type).region(region).build(); //1 based vs 0 based. Should stabilize around 0 based

        Response response = artifactRestClient.getArtifacts(options);
        
        int currentPage = page+1; // 0 based vs 1 based. Need to rectify at some point
        int totalArtifacts = Integer.parseInt(response.getHeaderString("x-total-artifacts"));
        int totalPages = totalArtifacts / pageSize + 1;
        List<EngagementArtifact> artifacts = response.readEntity(new GenericType<>(){});

        Map<String, String> artifactOptions = configService.getArtifactOptions();

        for(EngagementArtifact artifact : artifacts) {
            Engagement e = engagementService.getByUuid(artifact.getEngagementUuid());
            artifact.setCustomerName(e.getCustomerName());
            artifact.setProjectName(e.getProjectName());
            artifact.setRegion(e.getRegion());

            artifact.setPrettyType(artifactOptions.get(artifact.getType()));

        }
        
        return Response.ok(artifacts).header("x-current-page", currentPage).header("x-per-page", pageSize)
                .header("x-total-artifacts", totalArtifacts).header("x-next-page", currentPage + 1)
                .header("x-total-pages", totalPages).build();
        
    }

    public Set<String> getTypes(List<String> regions) {
        return artifactRestClient.getTypes(regions);
    }

    public Response getTypesCount(List<String> regions) {
        return artifactRestClient.getTypesCount(regions);
    }

    public void update(Engagement engagement, String author, String authorEmail) {
        String uuid = engagement.getUuid();
        try {
            artifactRestClient.updateArtifacts(uuid, engagement.getRegion(), engagement.getArtifacts(), authorEmail, author);
        } catch (WebApplicationException wae) {
            LOGGER.error("Failed to update artifacts for engagement {} {}", wae.getResponse().getStatus(), uuid);
        } catch (RuntimeException wae) {
            LOGGER.error("Failed to update artifacts for engagement {}", uuid, wae);
        }
    }
    
    @ConsumeEvent(value = EventType.RELOAD_ARTIFACTS_EVENT_ADDRESS, blocking = true)
    public void refresh(String message) {
        try {
            LOGGER.debug("refresh {}", message);
            artifactRestClient.refreshArtifacts();
            LOGGER.debug("refresh {} completed", message);
        } catch (WebApplicationException wae) { //without catching this it will fail silently
            LOGGER.error("Failed to refresh artifacts {}", wae.getResponse(), wae);
        }
    }

}
