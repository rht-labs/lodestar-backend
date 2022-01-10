package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.git.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@Path("/api/migrate")
public interface GitApiApiClient {

    @PUT
    Response migrate(@QueryParam(value = "participants") boolean migrateParticipants,
                     @QueryParam("artifacts") boolean migrateArtifacts,
                     @QueryParam("projects") boolean migrateUuids,
                     @QueryParam("hosting") boolean migrateHosting,
                     @QueryParam("engagements") boolean migrateEngagements,
                     @QueryParam("overwrite") boolean overwrite,
                     @QueryParam("dryRun") boolean dryRun,
                     @QueryParam("uuids") List<String> uuids);
}
