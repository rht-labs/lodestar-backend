package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.model.Engagement.EngagementState;
import com.redhat.labs.lodestar.model.filter.EngagementFilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.rest.client.CategoryApiClient;
import com.redhat.labs.lodestar.rest.client.EngagementApiClient;
import com.redhat.labs.lodestar.rest.client.EngagementStatusApiClient;
import com.redhat.labs.lodestar.rest.client.UseCaseApiClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.ListCompareAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "status.file")
    List<String> statusFile;

    @ConfigProperty(name = "commit.watch.files")
    List<String> engagementFiles;

    @ConfigProperty(name = "commit.msg.filter.list", defaultValue = "not.set")
    List<String> commitFilteredMessages;

    @Inject
    ParticipantService participantService;

    @Inject
    HostingService hostingEnvironmentService;

    @Inject
    ArtifactService artifactService;

    @Inject
    ConfigService configService;

    @Inject
    @RestClient
    EngagementApiClient engagementApiClient;

    @Inject
    @RestClient
    CategoryApiClient categoryApiClient;

    @Inject
    @RestClient
    EngagementStatusApiClient engagementStatusApiClient;

    @Inject
    @RestClient
    UseCaseApiClient useCaseApiClient;

    @Inject
    ActivityService activityService;

    Javers javers = JaversBuilder.javers().withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE).build();


    public void flushCache() { //TODO
        //List<Engagement>  allEngagements = engagementApiClient.getEngagements(0,5000);
        //allEngagements.forEach(e -> getEngagement(e.getUuid()));
    }

    //TODO cache this - btw - this caching will probably only work in a single pod sitch.
    // could continue to leverage the db or use distribute cache (back to Alpha!)
    public Engagement getEngagement(String uuid) {
        LOGGER.debug("getting uuid {}", uuid);
        Engagement engagement = engagementApiClient.getEngagement(uuid);
        List<HostingEnvironment> hes = hostingEnvironmentService.getHostingEnvironments(uuid);
        List<Artifact> artifacts = artifactService.getArtifacts(uuid);
        List<EngagementUser> participants = participantService.getParticipantsForEngagement(uuid);
        //As v2 gets rolling in FE - categories will switch to a string list and this call won't be necessary
        List<Category> categories = categoryApiClient.getCategories(uuid);
        List<Commit> activity = activityService.getActivityForUuid(uuid);

        engagement.setHostingEnvironments(hes);
        engagement.setArtifacts(artifacts);
        engagement.setEngagementUsers(new HashSet<>(participants));
        engagement.setCategories(categories);
        engagement.setCommits(activity);

        EngagementState state = engagement.getEngagementCurrentState(Instant.now());
        if(state.equals(EngagementState.ACTIVE) || state.equals(EngagementState.TERMINATING)) {
            engagement.setStatus(getStatus(uuid));
        }

        LOGGER.trace("got uuid {} with last update {}", uuid, engagement.getLastUpdate());
        return engagement;
    }

    private Status getStatus(String uuid) {
        try {
            LOGGER.debug("Getting status for {}", uuid);
            return engagementStatusApiClient.getEngagementStatus(uuid);
        } catch (WebApplicationException wex) {
            if(wex.getResponse().getStatus() == 404) {
                LOGGER.debug("No status for active/terminating engagement {}", uuid);
            } else {
                LOGGER.error("Exception occurred retrieving status for engagement {}", uuid);
            }
        } catch (ProcessingException pe) {
            LOGGER.error("Cannot connect to lodestar-engagement-status for engagement {}", uuid,  pe);
        }

        return null;
    }

    public Response getEngagementHead(String uuid) {
        return engagementApiClient.getEngagementHead(uuid);
    }

    public Engagement getByUuid(String engagementUuid) {
        return engagementApiClient.getEngagement(engagementUuid);
    }

    /**
     * Creates a new {@link Engagement} resource in the data store
     * 
     * @param engagement
     * @return
     */
    public Engagement create(Engagement engagement) {
        LOGGER.debug("sending create request {}", engagement);
        return engagementApiClient.createEngagement(engagement);
    }

    /**
     * Updates the {@link Engagement} resource in the data store
     * This supports the way v1 frontend saves the entire engagement. In the future the FE should use direct
     * updates to each component
     * 
     * @param engagement
     * @return
     */
    public Engagement update(Engagement engagement) {
        boolean somethingChanged = false;

        String author = engagement.getLastUpdateByName();
        String authorEmail = engagement.getLastUpdateByEmail();
        String engagementUuid = engagement.getUuid();

        //Will return a 404 with a message saying the uuid is not valid
        Engagement current = engagementApiClient.getEngagement(engagement.getUuid());

        // Validate activity is sync'd to prevent overwrites
        // This is legacy behavior. We should support last update per service
        validateLastUpdateIsLatest(engagement.getLastUpdate(), current.getLastUpdate(), engagementUuid);

        Diff diff = javers.compare(current, engagement);
        if(diff.hasChanges()) {
            LOGGER.debug("Engagement changes {}", diff);
            engagementApiClient.updateEngagement(engagement);
            //somethingChanged not needed as it will occur during engagement update
        }

        nullToEmpty(engagement);

        List<HostingEnvironment> hes = hostingEnvironmentService.getHostingEnvironments(engagement.getUuid());
        diff = javers.compareCollections(hes, engagement.getHostingEnvironments(), HostingEnvironment.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Hosting Environment changes {}", diff);
            hostingEnvironmentService.updateAndReload(engagementUuid, engagement.getHostingEnvironments(),
                    Author.builder().email(authorEmail).name(author).build());
            somethingChanged = true;
        }

        List<Artifact> artifacts = artifactService.getArtifacts(engagement.getUuid());
        diff = javers.compareCollections(artifacts, engagement.getArtifacts(), Artifact.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Artifacts has changes {}", diff);
            artifactService.update(engagement, author, authorEmail);
            somethingChanged = true;
        }

        List<EngagementUser> participants = participantService.getParticipantsForEngagement(engagement.getUuid());
        diff = javers.compareCollections(new HashSet<>(participants), engagement.getEngagementUsers(), EngagementUser.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Participants changed {}", diff);

            //Validate participant options
            Set<String> allowed = configService.getParticipantOptions(engagement.getType()).keySet();
            String errors = "";
            for(EngagementUser p : engagement.getEngagementUsers()) {
                if(!allowed.contains(p.getRole())) {
                    errors += String.format("Participant %s has invalid role %s. ", p.getEmail(), p.getRole());
                    LOGGER.error("Participant {} has invalid role {} for engagement type {} - {}", p.getEmail(), p.getRole(), engagement.getType(), engagement.getUuid());
                }
            }

            if(!errors.isEmpty()) {
                throw new WebApplicationException(Response.status(400).entity(Map.of("lodestarMessage", errors.trim())).build());
            }
            participants = participantService.updateParticipantsAndReload(engagementUuid, author, authorEmail,
                    engagement.getEngagementUsers());
            LOGGER.debug("Updated {}", participants);
            somethingChanged = true;
        }

        List<Category> categories = categoryApiClient.getCategories(engagement.getUuid());
        diff = javers.compareCollections(categories, engagement.getCategories(), Category.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Categories changed {}", diff);
            Set<String> catString = new TreeSet<>();
            engagement.getCategories().forEach(c -> catString.add(c.getName()));
            categoryApiClient.updateCategories(engagementUuid, author, authorEmail, catString);
            somethingChanged = true;
        }

        //TODO update cache
        if(somethingChanged) {
            engagementApiClient.registerUpdate(engagementUuid);
        } else {
            LOGGER.debug("Nothing changed for {}", engagementUuid);
        }
        return getEngagement(engagementUuid);
    }

    /**
     * Will throw a 409 error if invalid
     */
    private void validateLastUpdateIsLatest(String requestValue, String persistedValue, String engagementUuid) {
        Instant lastUpdateFromRequester = Instant.parse(requestValue);
        Instant lastUpdateFromSystem = Instant.parse(persistedValue);

        if(!lastUpdateFromSystem.equals(lastUpdateFromRequester)) {
            LOGGER.info("Last update is out of sync with the engagement db. Caller needs refresh for {} ui ({}) db ({})",
                    engagementUuid, requestValue, persistedValue);
            throw new WebApplicationException(409);
        }
    }

    private void nullToEmpty(Engagement engagement) {
        if(engagement.getArtifacts() == null) {
            engagement.setArtifacts(Collections.emptyList());
        }

        if(engagement.getHostingEnvironments() == null) {
            engagement.setHostingEnvironments(Collections.emptyList());
        }

        if(engagement.getEngagementUsers() == null) {
            engagement.setEngagementUsers(Collections.emptySet());
        }

        if(engagement.getCategories() == null) {
            engagement.setCategories(Collections.emptyList());
        }
    }

    /**
     * Returns Optional containing {@link Engagement} if found with given subdomain.
     * 
     * @param subdomain check this
     * @return
     */
    public Response getBySubdomain(String engagementUuid, String subdomain) {
        return hostingEnvironmentService.isSubdomainValid(engagementUuid, subdomain);
    }

    /**
     * Updates the {@link Status} and {@link Commit} data on an {@link Engagement}.
     * 
     * TODO Commits should be removed from the engagement API when the FE has implemented the changes
     *
     * TODO If the engagement is not found in the engagement service we should make an effort to
     * load it since we are receiving a message from gitlab
     * 
     * @param hook gitlab webhook info
     */
    public void updateStatusAndCommits(Hook hook) {
        LOGGER.debug("Hook for {} {}", hook.getCustomerName(), hook.getEngagementName());

        Engagement engagement = engagementApiClient.getEngagementByProject(hook.getProjectId());

        // send update status event
        if (hook.didFileChange(statusFile)) {
            LOGGER.debug("Status update {}", hook.getProjectId());
            engagementStatusApiClient.updateEngagementStatus(engagement.getUuid());
            Status status = engagementStatusApiClient.getEngagementStatus(engagement.getUuid());
            //engagementService.setStatus(engagement.getUuid(), status);
            //TODO put this status in the magical cache
        }


        if(hook.didFileChange(engagementFiles)) {
            LOGGER.debug("Engagement update {}", engagement);
            activityService.postHook(hook);
            //TODO should return the uuid from the post as header
            //TODO invalidate cache + load new into cache
        }

        // refresh entire engagement if requested
        if (hook.containsAnyMessage(commitFilteredMessages)) {
            //TODO Need to call a single refresh of engagement in every service
            LOGGER.debug("hook triggered refresh of engagement for project {}", hook.getProjectId());
        }

        getEngagement(engagement.getUuid());
    }


    /**
     *
     * @param customerName customer name
     * @param engagementName project name
     * @return engagement an engagement
     */
    public Engagement getByCustomerAndProjectName(String customerName, String engagementName) {
        return  engagementApiClient.getEngagement(customerName, engagementName);
    }

    public Engagement getByProjectId(int projectId) {
        return  engagementApiClient.getEngagementByProject(projectId);
    }

    public Map<EngagementState, Integer> getEngagementCountByStatus(Instant currentTime, Set<String> regions) {
        return engagementApiClient.getEngagementCounts(regions);
    }

    /**
     * Returns a {@link PagedEngagementResults} of {@link Engagement} that matches
     * the {@link ListFilterOptions}.
     * 
     * @param filter
     * @return ?
     */
    public Response getEngagementsPaged(EngagementFilterOptions filter) {
        //TODO hacking for v1
        //TODO should probably better align last activity field on engagement object so that
        // we can just filter on engagement instead of hitting the activity service
        String sort = filter.getSortFields();
        int pageSize = filter.getPerPage();

        if(pageSize == 5 && sort.equals("last_update")) {
            List<String> activity = activityService.getLatestActivity(0,5, filter.getV2Regions());
            List<Engagement> engagements = activity.stream().map(this::getEngagement).collect(Collectors.toList());
            return Response.ok(engagements).build();
        }

        int page = filter.getPage() - 1;
        pageSize = filter.getPerPage();

        Response response = engagementApiClient.getEngagements(page, pageSize, filter.getRegions(), filter.getTypes(), filter.getStates(), filter.getQ(), filter.getCategory(), sort);
        List<Engagement> engagements = response.readEntity(new GenericType<>(){});
        String total = response.getHeaderString("x-total-engagements");

        Map<String, String> engagementOptions = configService.getEngagementOptions();

        //TODO this loop is to allow frontend to change after v2 deployment.
        // FE should use participant, artifact count field, and categories (string version)
        for(Engagement e : engagements) {
            e.setPrettyType(engagementOptions.containsKey(e.getType()) ? engagementOptions.get(e.getType()) : e.getType());
        }
        return Response.ok(engagements).header("x-total-engagements", total).build();
    }

    /**
     * Returns a {@link List} of all customer names in the data store that match the
     * input
     *
     * @param partial - A string to match the customer name on. Can be all or
     *                  part. case-insensitive
     * @return a {@link List} of all customer names in the data store that match the
     *         input
     */
    public Response getSuggestions(String partial) {
        return engagementApiClient.suggest(partial);
    }

    /**
     * Deletes the {@link Engagement} from the database if it exists and not already
     * launched. Then, sends an event to remove the engagement from Git.
     * 
     * @param uuid id of engagement
     */
    public void deleteEngagement(String uuid) {
        engagementApiClient.deleteEngagement(uuid);
    }

    /**
     * Returns a {@link List} of {@link Category} that match the provided
     * {@link ListFilterOptions}}. Returns all {@link Category} if no
     * {@link ListFilterOptions}.
     * 
     * @param regions - a list of regions to filter on, or all if empty
     * @return aa list of categories and counts
     */
    public Response getCategories(List<String> regions, ListFilterOptions filterOptions) {
        int page = filterOptions.getPage().orElse(1);
        page--;
        int pageSize = filterOptions.getPerPage().orElse(20);
        return categoryApiClient.getCategoryRollup(regions, page, pageSize);
    }

    public Response getUseCases(ListFilterOptions filterOptions) {
        int page = filterOptions.getPage().orElse(1) -1;
        int pageSize = filterOptions.getPerPage().orElse(500);
        return useCaseApiClient.getUseCases(page, pageSize, filterOptions.getV2Regions());
    }

    /**
     * Adds {@link Launch} data to the given {@link Engagement} and uses
     * GitSyncService to process the modified {@link Engagement}.
     * 
     * @param uuid engagement uuid
     * @param author commit author
     * @param authorEmail commit author email
     * @return necessary? TODO
     */
    public Engagement launch(String uuid, String author, String authorEmail) {

        engagementApiClient.launch(uuid, author, authorEmail);
        return getEngagement(uuid);
    }

    public void refresh(Set<String> uuids) {
        engagementApiClient.refresh(uuids);
    }

}