package com.redhat.labs.lodestar.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.HostingEnvOpenShfitRollup;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.rest.client.HostingApiClient;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class HostingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostingService.class);

    @Inject
    @RestClient
    HostingApiClient hostingApiClient;

    @Inject
    EngagementService engagementService;

    public Response getHostingEnvironments(int page, int pageSize) {
        return hostingApiClient.getHostingEnvironments(page, pageSize);
    }

    public Response getHostingEnvironments(String engagementUuid) {
        return hostingApiClient.getHostingEnvironmentsForEngagement(engagementUuid);
    }
    
    public Response isSubdomainValidResponse(String engagementUuid, String subdomain) {
        return hostingApiClient.isSubdomainValid(engagementUuid, subdomain);
    }
    
    public Response getOcpVersionRollup(HostingEnvOpenShfitRollup rollup, List<String> region) {
        return hostingApiClient.getOpenShiftVersions(rollup, region);
    }

    /**
     * Sync - directly called api from the FE
     * 
     * @param engagementUuid      the engagement uuid to update
     * @param authorName          the name of the updater
     * @param authorEmail         the email of the user
     * @param hostingEnvironments the full list of hostingEnvironments for an
     *                            engagement
     * @return A response value indicating success (200) or failure (anything else)
     */
    public Response updateHostingEnvironments(String engagementUuid, String authorName, String authorEmail,
            List<HostingEnvironment> hostingEnvironments) {
        return hostingApiClient.updateHostingEnvironments(engagementUuid, authorName, authorEmail, hostingEnvironments);
    }

    /**
     * Async - with update engagement
     * 
     * @param message Comma separated string of uuid, name, email
     */
    @ConsumeEvent(value = EventType.UPDATE_HOSTING_EVENT_ADDRESS, blocking = true)
    public void updateHostingEnvironments(String message) {
        LOGGER.debug("hosting update via engagement save {}", message);
        String[] uuidNameEmail = message.split(",");

        Engagement engagement = engagementService.getByUuid(uuidNameEmail[0], new FilterOptions());

        try {
            hostingApiClient.updateHostingEnvironments(engagement.getUuid(), uuidNameEmail[1], uuidNameEmail[2], engagement.getHostingEnvironments());
            LOGGER.debug("Updated hosting for engagement {}", engagement.getUuid());
        } catch (WebApplicationException wae) {
            LOGGER.error("Failed to update hosting for engagement {} {}", wae.getResponse().getStatus(), message);
        } catch (RuntimeException wae) {
            LOGGER.error("Failed to update hosting for engagement {}", message, wae);
        }

    }

    @ConsumeEvent(value = EventType.RELOAD_HOSTING_EVENT_ADDRESS, blocking = true)
    public void refesh(String message) {
        try {
            LOGGER.debug("refresh {}", message);
            Response response = hostingApiClient.refreshHostingEnvironments();
            LOGGER.debug("refresh {} completed. Hosting count is {} ", message, response.getHeaderString("x-total-hosting"));
        } catch (WebApplicationException wae) { // without catching this it will fail silently
            LOGGER.error("Failed to refresh hosting environments {}", wae.getResponse(), wae);
        }
    }
}
