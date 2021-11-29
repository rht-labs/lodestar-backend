package com.redhat.labs.lodestar.resource;

import java.time.*;
import java.util.*;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.service.ActivityService;
import com.redhat.labs.lodestar.service.ParticipantService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Engagement.EngagementState;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.service.ConfigService;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.util.JWTUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Engagements", description = "Base engagement apis")
public class EngagementResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementResource.class);

    private static final String ACCEPT_VERSION_1 = "v1";

    public static final String ACCESS_CONTROL_EXPOSE_HEADER = "Access-Control-Expose-Headers";
    public static final String LAST_UPDATE_HEADER = "last-update";

    @Inject
    JsonWebToken jwt;
    
    @Inject
    JWTUtils jwtUtils;

    @Inject
    EngagementService engagementService;

    @Inject
    ActivityService activityService;
    
    @Inject
    ConfigService configService;

    @Inject
    ParticipantService participantService;

    /*
     * GET LIST
     */

    @GET
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "A list or empty list of engagement resources returned") })
    @Operation(summary = "Returns all engagement resources from the database.  Can be empty list if none found.")
    public Response getAll(@Context UriInfo uriInfo, @BeanParam ListFilterOptions filterOptions) {

        // create one page with many results for v1
        setDefaultPagingFilterOptions(filterOptions);

        return engagementService.getEngagementsPaged(filterOptions);
    }
    
    @GET
    @Path("/count")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagement counts computed.") })
    @Operation(summary = "Gets a map of engagement counts by status")
    public Map<EngagementState, Integer> countByStatus(@QueryParam(value = "localTime") String localTime) {
        
        Instant currentTime = localTime == null ? Instant.now() : Instant.parse(localTime);

        return engagementService.getEngagementCountByStatus(currentTime);
    }

    //TODO metrics is saying this is not called
    //TODO instead appears to filter list held in-memory (which is ridiculous)
    @GET
    @Path("/customers/suggest")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    public Response findCustomers(@Context UriInfo uriInfo,
            @Parameter(name = "suggest", deprecated = true, description = "uses suggestion as case insensitive search string") @QueryParam("suggest") String suggest,
            @BeanParam ListFilterOptions filterOptions) {

        return engagementService.getSuggestions(suggest);

//        if (suggest != null) {
//            filterOptions.addLikeSearchCriteria("customer_name", suggest);
//        }
//
//        PagedStringResults page = engagementService.getSuggestions(filterOptions);
//        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
//        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
//        return builder.build();

    }

    /*
     * GET SINGLE
     */

    //TODO remove this access
    @Deprecated
    @GET
    @SecurityRequirement(name = "jwt")
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given customer and project names.")
    public Response get(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @BeanParam FilterOptions filterOptions) {

        LOGGER.warn("Deprecated get method used /customers/{}/projects/{}", customerName, projectName);

        Engagement engagement = engagementService.getByCustomerAndProjectName(customerName, projectName);
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        engagement.setWriteable(writer);
        
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).header("deprecated", "use get by uuid").build();

    }

    @GET
    @SecurityRequirement(name = "jwt")
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with id does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given id.")
    public Response get(@PathParam("id") String uuid, @BeanParam FilterOptions filterOptions) {

        Engagement engagement = engagementService.getEngagement(uuid);//.getByUuid(uuid, filterOptions);

        String exclude = filterOptions.getExclude() == null ? "" : filterOptions.getExclude();
        if(exclude.contains("commits")) {
            engagement.setCommits(null);
        }

        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        engagement.setWriteable(writer);
        
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    /*
     * GET - Queries
     */

    // Not sure if this one is being used currently
    @GET
    @Path("/state/{state}")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagements with state have been returned.") })
    @Operation(summary = "Returns engagement list")
    public Response getByState(@Context UriInfo uriInfo, @PathParam("state") String state,
            @Parameter(name = "start", required = true, description = "start date of range") @NotBlank @QueryParam("start") String start,
            @Parameter(name = "end", required = true, description = "end date of range") @NotBlank @QueryParam("end") String end,
            @BeanParam ListFilterOptions filterOptions) {

        // set defaults for paging if not already set
        setDefaultPagingFilterOptions(filterOptions);

        // set state parameter
        filterOptions.addEqualsSearchCriteria("state", state);

        // set start parameter
        filterOptions.addEqualsSearchCriteria("start", start);

        // set end parameter
        filterOptions.addEqualsSearchCriteria("end", end);

        PagedEngagementResults page = new PagedEngagementResults(); //TODO engagementService.getEngagementsPaged(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().forEach(builder::header);
        return builder.build();

    }

    @GET
    @Path("/users/summary")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "400", description = "Use /engagements/participants/enabled") })
    @Operation(summary = "Use /engagements/participants/enabled")
    public Response getUserSummary(@QueryParam("search") String search) {
        List<String> regions = new ArrayList<>();

        if(search != null) {
            String[] params = search.split("&");

            for (String param : params) {
                String[] keyValues = param.split("=");

                if(keyValues.length == 0) {
                    Response.status(Response.Status.BAD_REQUEST).build();
                }
                if (keyValues[0].equals("engagement_region")) {
                    String[] regionsArray = keyValues[1].split(",");
                    regions = Arrays.asList(regionsArray);
                }
            }
        }

        //Legacy conversion - when FE is ready remove EngagementUserSummary
        Map<String, Long> v2 = participantService.getEnabledParticipants(regions);
        EngagementUserSummary enabled = EngagementUserSummary.builder().allUsersCount(v2.get("All"))
                .otherUsersCount(v2.get("Others")).rhUsersCount(v2.get("Red Hat")).build();

        return Response.ok(enabled).build();
    }

    /*
     * HEAD
     */

    @HEAD
    @SecurityRequirement(name = "jwt")
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    @Operation(summary = "Returns metadata regarding the engagement resource for the given customer and project names.")
    public Response head(@PathParam("id") String uuid) {
        return engagementService.getEngagementHead(uuid);
    }

    @HEAD
    @SecurityRequirement(name = "jwt")
    @Path("{uuid}/subdomain/{subdomain}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "409", description = "Subdomain is already in use"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    public Response uniqueSubdomain(@PathParam("uuid") String uuid, @PathParam("subdomain") String subdomain) {
        return engagementService.getBySubdomain(uuid, subdomain);
    }

    @HEAD
    @SecurityRequirement(name = "jwt")
    @Path("/subdomain/{subdomain}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "409", description = "Subdomain is already in use"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    @Operation(summary = "Deprecated. Use uuid version which filters an engagement that might already be using it (the 'current' view.")
    public Response uniqueSubdomainDeprecated(@PathParam("subdomain") String subdomain) {
        return uniqueSubdomain("phony-uuid", subdomain);
    }

    /*
     * POST
     */

    @POST
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "409", description = "Engagement resource already exists"),
            @APIResponse(responseCode = "201", description = "Engagement stored in database") })
    @Operation(summary = "Creates the engagement resource in the database.")
    public Response post(@Valid Engagement engagement, @Context UriInfo uriInfo) {
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        if(!writer) {
            return forbiddenResponse(engagement.getType());
        }

        // pull user info from token
        engagement.setLastUpdateByName(jwtUtils.getUsernameFromToken(jwt));
        engagement.setLastUpdateByEmail(jwtUtils.getUserEmailFromToken(jwt));

        // create the resource
        Engagement created = engagementService.create(engagement);

        // build location response
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        builder.path("/" + engagement.getUuid());
        return Response.created(builder.build()).entity(created).build();

    }

    @PUT
    @Deprecated
    @SecurityRequirement(name = "jwt")
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to update"),
            @APIResponse(responseCode = "200", description = "Engagement updated in the database") })
    @Operation(deprecated = true, summary = "Updates the engagement resource in the database.")
    public Response put(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @Valid Engagement engagement) {

        LOGGER.warn("Deprecated put method used /customers/{}/projects/{}", customerName, projectName);
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        if(!writer) {
            return forbiddenResponse(engagement.getType());
        }

        // pull user info from token
        engagement.setLastUpdateByName(jwtUtils.getUsernameFromToken(jwt));
        engagement.setLastUpdateByEmail(jwtUtils.getUserEmailFromToken(jwt));

        return Response.ok(engagementService.update(engagement)).build();

    }

    @PUT
    @SecurityRequirement(name = "jwt")
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to update"),
            @APIResponse(responseCode = "200", description = "Engagement updated in the database") })
    @Operation(summary = "Updates the engagement resource in the database.")
    public Response put(@PathParam("id") String uuid, @Valid Engagement engagement) {

        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        if(!writer) {
            return forbiddenResponse(engagement.getType());
        }
        
        // pull user info from token
        engagement.setLastUpdateByName(jwtUtils.getUsernameFromToken(jwt));
        engagement.setLastUpdateByEmail(jwtUtils.getUserEmailFromToken(jwt));

        return Response.ok(engagementService.update(engagement)).build();

    }

    //TODO this method should be /launch/{uuid} with no body. fix after v2 rollout
    @PUT
    @Path("/launch")
    @SecurityRequirement(name = "jwt")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "200", description = "Launch data added to engagement resource and persisted to git") })
    @Operation(summary = "Adds launch data to the engagement resource and immediately persists it to git.")
    public Response launch(Engagement engagement) {

        Engagement launch = engagementService.getEngagement(engagement.getUuid());
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(launch.getType()));
        if(!writer) {
            return forbiddenResponse(launch.getType());
        }

        // pull user info from token
        engagement.setLastUpdateByName(jwtUtils.getUsernameFromToken(jwt));
        engagement.setLastUpdateByEmail(jwtUtils.getUserEmailFromToken(jwt));

        launch = engagementService.launch(engagement.getUuid(), jwtUtils.getUsernameFromToken(jwt),
                jwtUtils.getUserEmailFromToken(jwt));
        return Response.ok(launch).build();

    }

    /*
     * DELETE
     */

    @DELETE
    @SecurityRequirement(name = "jwt")
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to delete"),
            @APIResponse(responseCode = "400", description = "Engagement resource has already been launched"),
            @APIResponse(responseCode = "202", description = "Engagement deleted in the database and sent to Git for processing") })
    @Operation(summary = "Deletes the engagement resource in the database and sends to Git API for deletion")
    public Response delete(@PathParam("id") String uuid) {

        Engagement engagement = engagementService.getByUuid(uuid);
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        if(!writer) {
            return forbiddenResponse(engagement.getType());
        }
        
        engagementService.deleteEngagement(uuid);
        return Response.accepted().build();

    }
    
    private Response forbiddenResponse(String type) {
        String message = String.format("{\"message\": \"You cannot modify %s engagements\"}", type);
        return Response.status(403).entity(message).build();
    }

    private void setDefaultPagingFilterOptions(ListFilterOptions options) {

        boolean isV1 = null == options.getApiVersion() || ACCEPT_VERSION_1.equals(options.getApiVersion());

        options.setPage(options.getPage().orElse(1));
        options.setPerPage(options.getPerPage().orElse(isV1 ? 500 : 20));

    }

}