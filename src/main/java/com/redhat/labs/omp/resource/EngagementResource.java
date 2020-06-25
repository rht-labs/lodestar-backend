package com.redhat.labs.omp.resource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.service.EngagementService;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class EngagementResource {

    private static final String NAME_CLAIM = "name";
    private static final String USER_EMAIL_CLAIM = "email";

    public static final String DEFAULT_USERNAME = "omp-user";
    public static final String DEFAULT_EMAIL = "omp-email";

    @Inject
    JsonWebToken jwt;

    @Inject
    EngagementService engagementService;

    @POST
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "409", description = "Engagement resource already exists"),
            @APIResponse(responseCode = "201", description = "Engagement stored in database")})
    @Operation(summary = "Creates the engagement resource in the database.")
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
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to update"),
            @APIResponse(responseCode = "200", description = "Engagement updated in the database")})
    @Operation(summary = "Updates the engagement resource in the database.")
    public Engagement put(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @Valid Engagement engagement) {

        // pull user info from token
        engagement.setLastUpdateByName(getUsernameFromToken());
        engagement.setLastUpdateByEmail(getUserEmailFromToken());

        return engagementService.update(customerName, projectName, engagement);

    }

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned")})
    @Operation(summary = "Returns the engagement resource for the given customer and project names.")
    public Engagement get(@PathParam("customerName") String customerName,
            @PathParam("projectName") String projectName) {

        Optional<Engagement> optional = engagementService.get(customerName, projectName);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new ResourceNotFoundException("no resource found.");

    }

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "A list or empty list of engagement resources returned")})
    @Operation(summary = "Returns all engagement resources from the database.  Can be empty list if none found.")
    public List<Engagement> getAll() {
        return engagementService.getAll();
    }
    
    @GET
    @Path("/customers/suggest")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { 
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    public Response findCustomers(@NotBlank @QueryParam("suggest") String match) {
        
        Collection<String> customerSuggestions = engagementService.getSuggestions(match);
        
        return Response.ok(customerSuggestions).build();
    }

    @PUT
    @Path("/launch")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Launch data added to engagement resource and persisted to git")})
    @Operation(summary = "Adds launch data to the engagement resource and immediately persists it to git.")
    public Engagement launch(@Valid Engagement engagement) {

        // pull user info from token
        engagement.setLastUpdateByName(getUsernameFromToken());
        engagement.setLastUpdateByEmail(getUserEmailFromToken());

        return engagementService.launch(engagement);

    }

    private String getUsernameFromToken() {
        Optional<String> optional = jwt.claim(NAME_CLAIM);
        return optional.isPresent() ? optional.get() : getUserEmailFromToken();
    }

    private String getUserEmailFromToken() {
        Optional<String> optional = jwt.claim(USER_EMAIL_CLAIM);
        return optional.isPresent() ? optional.get() : DEFAULT_EMAIL;
    }

}