package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.Author;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.rest.client.HostingEnvironmentApiClient;
import io.quarkus.vertx.ConsumeEvent;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class HostingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HostingService.class);

    @Inject
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    public Response getHostingEnvironments(Set<String> engagementUuids, int page, int pageSize) {
        return hostingEnvironmentApiClient.getHostingEnvironments(engagementUuids, page, pageSize);
    }

    public List<HostingEnvironment> getHostingEnvironments(String engagementUuid) {
        try {
            return hostingEnvironmentApiClient.getHostingEnvironmentsByEngagementUuid(engagementUuid);
        } catch (WebApplicationException wex) {
            if(wex.getResponse().getStatus() >= 500) {
                LOGGER.error("Hosting Server error ({}) from hosting env for euuid {}", wex.getResponse().getStatus(), engagementUuid);
                return Collections.EMPTY_LIST;
            }
            throw wex;
        }
    }

    public List<HostingEnvironment> updateAndReload(String engagementUuid, List<HostingEnvironment> hostingEnvironments, Author author) {
        return hostingEnvironmentApiClient.updateHostingEnvironments(engagementUuid, hostingEnvironments, author.getEmail(), author.getName());
    }

    public Response isSubdomainValid(String engagementUuid, String subdomain) {
        return hostingEnvironmentApiClient.isSubdomainValid(engagementUuid, subdomain);
    }

    @ConsumeEvent(value = EventType.RELOAD_HOSTING_EVENT_ADDRESS, blocking = true)
    public void refresh(String message) {
        try {
            LOGGER.debug("refresh {}", message);
            hostingEnvironmentApiClient.refresh();
            LOGGER.debug("refresh {} completed", message);
        } catch (WebApplicationException wae) { //without catching this it will fail silently
            LOGGER.error("Failed to refresh hosting {}", wae.getResponse(), wae);
        }
    }
}
