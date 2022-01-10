package com.redhat.labs.lodestar.resource;

import com.redhat.labs.lodestar.rest.client.GitApiApiClient;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/engagements/migrate")
@Tag(name = "Migration", description = "Migration services")
public class MigrationResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationResource.class);

    @Inject
    @RestClient
    GitApiApiClient migrationApiClient;

    @PUT
    @SecurityRequirement(name = "jwt")
    @Produces("application/json")
    public Response migrate(@QueryParam(value = "participants") boolean migrateParticipants,
                            @QueryParam("artifacts") boolean migrateArtifacts,
                            @QueryParam("projects") boolean migrateUuids,
                            @QueryParam("hosting") boolean migrateHosting,
                            @QueryParam("engagements") boolean migrateEngagements,
                            @QueryParam("overwrite") boolean overwrite,
                            @QueryParam("dryRun") boolean dryRun,
                            @QueryParam("uuids") List<String> uuids) {

        try {
            migrationApiClient.migrate(migrateUuids, migrateParticipants, migrateArtifacts, migrateHosting, migrateEngagements,
                    overwrite, dryRun, uuids);
        } catch (Exception ex) {
            LOGGER.error("Migration did not complete successfully", ex);
            return Response.status(Response.Status.BAD_REQUEST).entity("{ \"message\": \"Migration did not complete successfully\"}").build();
        }

        return Response.ok().build();
    }
}
