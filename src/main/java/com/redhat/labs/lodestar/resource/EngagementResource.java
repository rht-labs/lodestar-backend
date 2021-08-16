package com.redhat.labs.lodestar.resource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
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
import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.service.ConfigService;
import com.redhat.labs.lodestar.service.EngagementService;
import com.redhat.labs.lodestar.util.DateFormatter;
import com.redhat.labs.lodestar.util.JWTUtils;

@RequestScoped
@Path("/engagements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Engagements", description = "Base engagement apis")
public class EngagementResource {

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
    ConfigService configService;

    /*
     * GET LIST
     */

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "A list or empty list of engagement resources returned") })
    @Operation(summary = "Returns all engagement resources from the database.  Can be empty list if none found.")
    @Counted(name = "engagement-get-all-counted")
    @Timed(name = "engagement-get-all-timer", unit = MetricUnits.MILLISECONDS)
    public Response getAll(@Context UriInfo uriInfo,
            @Parameter(name = "categories", deprecated = true, required = false, description = "filter based on category names.  Use search instead.") @QueryParam("categories") Optional<String> categories,
            @BeanParam ListFilterOptions filterOptions) {

        // add categories filter if deprecated param used
        if (categories.isPresent()) {
            filterOptions.addLikeSearchCriteria("categories.name", categories.get());
        }

        // create one page with many results for v1
        setDefaultPagingFilterOptions(filterOptions);

        PagedEngagementResults page = engagementService.getEngagementsPaged(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }
    
    @GET
    @Path("/count")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagement counts computed.") })
    @Operation(summary = "Gets a map of engagement counts by status")
    @Counted(name = "engagement-total-by-status-count")
    @Timed(name = "engagement-total-by-status-timer", unit = MetricUnits.MILLISECONDS)
    public Map<EngagementState, Integer> countByStatus(@QueryParam(value = "localTime") String localTime) {
        
        LocalDateTime currentTime = localTime == null ? LocalDateTime.now() : DateFormatter.getInstance().getDateTime(localTime);

        return engagementService.getEngagementCountByStatus(currentTime);
    }

    @GET
    @Path("/customers/suggest")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Customer data has been returned.") })
    @Operation(summary = "Returns customers list")
    @Counted(name = "engagement-suggest-url-counted")
    @Timed(name = "engagement-suggest-url-timer", unit = MetricUnits.MILLISECONDS)
    public Response findCustomers(@Context UriInfo uriInfo,
            @Parameter(name = "suggest", deprecated = true, required = false, description = "uses suggestion as case insensitive search string") @QueryParam("suggest") Optional<String> suggest,
            @BeanParam ListFilterOptions filterOptions) {

        if (suggest.isPresent()) {
            filterOptions.addLikeSearchCriteria("customer_name", suggest.get());
        }

        PagedStringResults page = engagementService.getSuggestions(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

    /*
     * GET SINGLE
     */

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given customer and project names.")
    @Counted(name = "engagement-get-counted")
    @Timed(name = "enagement-get-timer", unit = MetricUnits.MILLISECONDS)
    public Response get(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @BeanParam FilterOptions filterOptions) {

        Engagement engagement = engagementService.getByCustomerAndProjectName(customerName, projectName, filterOptions);
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        engagement.setWriteable(writer);
        
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    @GET
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with id does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and returned") })
    @Operation(summary = "Returns the engagement resource for the given id.")
    @Counted(name = "engagement-get-by-uuid-counted")
    @Timed(name = "engagement-get-by-uuid-timer", unit = MetricUnits.MILLISECONDS)
    public Response get(@PathParam("id") String uuid, @BeanParam FilterOptions filterOptions) {

        Engagement engagement = engagementService.getByUuid(uuid, filterOptions);
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        engagement.setWriteable(writer);
        
        return Response.ok(engagement).header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    /*
     * GET - Queries
     */

    @GET
    @Path("/state/{state}")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagements with state have been returned.") })
    @Operation(summary = "Returns engagement list")
    @Counted(name = "engagement-get-all-by-state-counted")
    @Timed(name = "engagement-get-all--by-state-timer", unit = MetricUnits.MILLISECONDS)
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

        PagedEngagementResults page = engagementService.getEngagementsPaged(filterOptions);
        ResponseBuilder builder = Response.ok(page.getResults()).links(page.getLinks(uriInfo.getAbsolutePathBuilder()));
        page.getHeaders().entrySet().stream().forEach(e -> builder.header(e.getKey(), e.getValue()));
        return builder.build();

    }

    @GET
    @Path("/users/summary")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Engagement user summary filtered by supplied options") })
    @Operation(summary = "Returns total users count as well as counts for redhat or other users")
    @Counted(name = "engagement-get-user-summary-counted")
    @Timed(name = "engagement-get-user-summary-timer", unit = MetricUnits.MILLISECONDS)
    public EngagementUserSummary getUserSummary(@QueryParam("search") String search) {
        ListFilterOptions filterOptions = ListFilterOptions.builder().search(search).build();
        return engagementService.getUserSummary(filterOptions);
    }

    /*
     * HEAD
     */

    @HEAD
    @Deprecated
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    @Operation(deprecated = true, summary = "Returns metadata regarding the engagement resource for the given customer and project names.")
    @Counted(name = "engagement-head-dep-counted")
    @Timed(name = "engagement-head-dep-timer", unit = MetricUnits.MILLISECONDS)
    public Response head(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName) {

        Engagement engagement = engagementService.getByCustomerAndProjectName(customerName, projectName,
                new FilterOptions());
        return Response.ok().header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    @HEAD
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "404", description = "Engagement resource with customer and project names does not exist"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    @Operation(summary = "Returns metadata regarding the engagement resource for the given customer and project names.")
    @Counted(name = "engagement-head-by-uuid-counted")
    @Timed(name = "engagement-head-by-uuid-timer", unit = MetricUnits.MILLISECONDS)
    public Response head(@PathParam("id") String uuid) {

        Engagement engagement = engagementService.getByUuid(uuid, new FilterOptions());
        return Response.ok().header(LAST_UPDATE_HEADER, engagement.getLastUpdate())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();

    }

    @HEAD
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/subdomain/{subdomain}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "409", description = "Subdomain is already in use"),
            @APIResponse(responseCode = "200", description = "Engagement resource found and metadata returned in headers") })
    @Counted(name = "engagement-head-unq-subdomain-counted")
    @Timed(name = "engagement-head-unq-subdomain-timer", unit = MetricUnits.MILLISECONDS)
    public Response uniqueSubdomain(@PathParam("subdomain") String subdomain) {
        int status = engagementService.getBySubdomain(subdomain).isPresent() ? HttpStatus.SC_CONFLICT
                : HttpStatus.SC_OK;
        return Response.status(status).build();
    }

    /*
     * POST
     */

    @POST
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "409", description = "Engagement resource already exists"),
            @APIResponse(responseCode = "201", description = "Engagement stored in database") })
    @Operation(summary = "Creates the engagement resource in the database.")
    @Counted(name = "engagement-post-counted")
    @Timed(name = "engagement-post-timer", unit = MetricUnits.MILLISECONDS)
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

    /*
     * PUT
     */

    @PUT
    @Deprecated
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/customers/{customerName}/projects/{projectName}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to update"),
            @APIResponse(responseCode = "200", description = "Engagement updated in the database") })
    @Operation(deprecated = true, summary = "Updates the engagement resource in the database.")
    @Counted(name = "engagement-put-dep-counted")
    @Timed(name = "engagement-put-dep-timer", unit = MetricUnits.MILLISECONDS)
    public Response put(@PathParam("customerName") String customerName, @PathParam("projectName") String projectName,
            @Valid Engagement engagement) {
        
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
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to update"),
            @APIResponse(responseCode = "200", description = "Engagement updated in the database") })
    @Operation(summary = "Updates the engagement resource in the database.")
    @Counted(name = "engagement-put-by-uuid-counted")
    @Timed(name = "engagement-put-by-uuid-timer", unit = MetricUnits.MILLISECONDS)
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

    @PUT
    @Path("/launch")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "200", description = "Launch data added to engagement resource and persisted to git") })
    @Operation(summary = "Adds launch data to the engagement resource and immediately persists it to git.")
    @Counted(name = "engagement-put-launch-counted")
    @Timed(name = "engagement-put-launch-timer", unit = MetricUnits.MILLISECONDS)
    public Response launch(@Valid Engagement engagement) {
        
        boolean writer = jwtUtils.isAllowedToWriteEngagement(jwt, configService.getPermission(engagement.getType()));
        if(!writer) {
            return forbiddenResponse(engagement.getType());
        }

        // pull user info from token
        engagement.setLastUpdateByName(jwtUtils.getUsernameFromToken(jwt));
        engagement.setLastUpdateByEmail(jwtUtils.getUserEmailFromToken(jwt));

        engagementService.launch(engagement);
        return Response.ok(engagement).build();

    }

    @PUT
    @Path("/uuids/set")
    @SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "The request was successful.") })
    @Operation(summary = "Sets UUIDs on all engagement and users that do not already have a UUID")
    @Counted(name = "engagement-put-uuid-counted")
    @Timed(name = "engagement-put-uuid-timer", unit = MetricUnits.MILLISECONDS)
    public Response setUuids() {

        engagementService.setNullUuids();
        return Response.ok().build();

    }

    /*
     * DELETE
     */

    @DELETE
    @SecurityRequirement(name = "jwt", scopes = {})
    @Path("/{id}")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "403", description = "Not authorized for engagement type"),
            @APIResponse(responseCode = "404", description = "Engagement resource not found to delete"),
            @APIResponse(responseCode = "400", description = "Engagement resource has already been launched"),
            @APIResponse(responseCode = "202", description = "Engagement deleted in the database and sent to Git for processing") })
    @Operation(summary = "Deletes the engagement resource in the database and sends to Git API for deletion")
    @Counted(name = "engagement-delete-by-uuid-counted")
    @Timed(name = "engagement-delete-by-uuid-timer", unit = MetricUnits.MILLISECONDS)
    public Response delete(@PathParam("id") String uuid) {

        Engagement engagement = engagementService.getByUuid(uuid, new FilterOptions());
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

        boolean isV1 = false;
        if (null == options.getApiVersion() || ACCEPT_VERSION_1.equals(options.getApiVersion())) {
            isV1 = true;
        }

        options.setPage(getPage(options.getPage()));
        options.setPerPage(getPerPage(options.getPerPage(), isV1));

    }

    private Integer getPage(Optional<Integer> page) {
        return page.isPresent() ? page.get() : 1;
    }

    private Integer getPerPage(Optional<Integer> perPage, boolean isV1) {
        if (perPage.isPresent()) {
            return perPage.get();
        }
        return isV1 ? 500 : 20;
    }

}