package com.redhat.labs.omp.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.EngagementService;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EngagementResource {

    private static final String USERNAME_CLAIM = "preferred_username";
    private static final String USER_EMAIL_CLAIM = "email";

    public static final String DEFAULT_USERNAME = "omp-user";
    public static final String DEFAULT_EMAIL = "omp-email";

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    @POST
    public Response post(@Valid Engagement engagement, @Context UriInfo uriInfo) {

        // pull user info from token
        engagement.setLastUpdateByName(getUsernameFromToken());
        engagement.setLastUpdateByEmail(getUserEmailFromToken());

        // create the resource
        Engagement created = engagementService.create(engagement);

        // build location response
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path("/customers/" + created.getCustomerName() + "/projects/" + created.getProjectName());
        return Response.created(builder.build()).entity(created).build();

    }

    @PUT
    @Path("/customers/{customerName}/projects/{projectName}")
    public Engagement put(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @Valid Engagement engagement) {

        // pull user info from token
        engagement.setLastUpdateByName(getUsernameFromToken());
        engagement.setLastUpdateByEmail(getUserEmailFromToken());

        return engagementService.update(customerName, projectName, engagement);

    }

    @GET
    @Path("/customers/{customerName}/projects/{projectName}")
    public Engagement get(@PathParam("customerName") String customerName,
            @PathParam("projectName") String projectName) {

        Optional<Engagement> optional = engagementService.get(customerName, projectName);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new ResourceNotFoundException("no resource found.");

    }

    @GET
    public List<Engagement> getAll() {
        return engagementService.getAll();
    }

    private String getUsernameFromToken() {
        Optional<String> optional = jwt.claim(USERNAME_CLAIM);
        return optional.isPresent() ? optional.get() : DEFAULT_USERNAME;
    }

    private String getUserEmailFromToken() {
        Optional<String> optional = jwt.claim(USER_EMAIL_CLAIM);
        return optional.isPresent() ? optional.get() : DEFAULT_EMAIL;
    }

}