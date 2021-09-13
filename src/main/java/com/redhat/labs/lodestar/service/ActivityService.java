package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.rest.client.*;
import org.eclipse.microprofile.config.inject.*;
import org.eclipse.microprofile.rest.client.inject.*;

import javax.enterprise.context.*;
import javax.inject.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
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

    /**
     *
     * @param page page number
     * @param pageSize page size
     * @return A list of commits of the latest activity - 1 per engagement
     */
    public List<Commit> getLatestActivity(int page, int pageSize) {
        return activityApiClient.getLatestActivity(page, pageSize);
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
