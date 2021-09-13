package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.Author;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.rest.client.HostingEnvironmentApiClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.*;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class HostingService {

    @Inject
    @RestClient
    HostingEnvironmentApiClient hostingEnvironmentApiClient;

    public Response getHostingEnvironments(Set<String> engagementUuids, int page, int pageSize) {
        return hostingEnvironmentApiClient.getHostingEnvironments(engagementUuids, page, pageSize);
    }

    public List<HostingEnvironment> getHostingEnvironments(String engagementUuid) {
        return hostingEnvironmentApiClient.getHostingEnvironmentsByEngagementUuid(engagementUuid);
    }

    public List<HostingEnvironment> updateAndReload(String engagementUuid, List<HostingEnvironment> hostingEnvironments, Author author) {
        return hostingEnvironmentApiClient.updateHostingEnvironments(engagementUuid, hostingEnvironments, author.getEmail(), author.getName());
    }

    public Response isSubdomainValid(String engagementUuid, String subdomain) {
        return hostingEnvironmentApiClient.isSubdomainValid(engagementUuid, subdomain);
    }
}
