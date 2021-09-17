package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.rest.client.*;
import org.eclipse.microprofile.config.inject.*;
import org.eclipse.microprofile.rest.client.inject.*;

import javax.enterprise.context.*;
import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class ActivityService {

    @Inject
    @RestClient
    ActivityApiClient activityApiClient;

    @ConfigProperty(name = "webhook.token")
    String hookToken;

    public void postHook(Hook hook) {
        activityApiClient.postHook(hook, hookToken);
    }

    public List<Commit> getActivityForUuid(String engagementUuid) {
        return activityApiClient.getActivityForUuid(engagementUuid);
    }

    public Response getActivityHead(String engagementUuid) {
        return activityApiClient.getLastActivity(engagementUuid);
    }

    public Instant getLatestActivity(String engagementUuid) {
        Response response = getActivityHead(engagementUuid);
        String lastUpdate = response.getHeaderString("last-update");

        return Instant.parse(lastUpdate);
    }

    /**
     *
     * @param page page number
     * @param pageSize page size
     * @return A list of the last activity (commit) for each engagement
     */
    public List<String> getLatestActivity(int page, int pageSize, Set<String> regions) {
        return activityApiClient.getLatestActivity(page, pageSize, regions);
    }

    public Response getPaginatedActivityForUuid(String engagementUuid, int page, int pageSize) {
        return activityApiClient.getPaginatedActivityForUuid(engagementUuid, page, pageSize);
    }

    public Response getPaginatedActivity(int page, int pageSize) {
        return activityApiClient.getPaginatedActivity(page, pageSize);
    }

    public Response refresh() {
        return activityApiClient.refresh();
    }
}
