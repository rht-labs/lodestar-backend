package com.redhat.labs.lodestar.service;

import com.redhat.labs.lodestar.model.*;
import com.redhat.labs.lodestar.model.Engagement.EngagementState;
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
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "status.file")
    List<String> statusFile;

    @ConfigProperty(name = "commit.watch.files")
    List<String> engagementFiles;

    @ConfigProperty(name = "commit.msg.filter.list", defaultValue = "not.set")
    List<String> commitFilteredMessages;
    
    @ConfigProperty(name = "v2.enabled")
    boolean v2Enabled;

    @Inject
    ParticipantService participantService;

    @Inject
    HostingService hostingEnvironmentService;

    @Inject
    ArtifactService artifactService;

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
        List<Engagement>  allEngagements = engagementApiClient.getEngagements(0,5000);
        allEngagements.forEach(e -> getEngagement(e.getUuid()));
    }

    //TODO cache this - btw - this caching will probably only work in a single pod sitch.
    // could continue to leverage the db or use distribute cache (back to Alpha!)
    public Engagement getEngagement(String uuid) {
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

        //TODO Add Status for Non Archived
        //TODO Add Activity and Set Last Update
        return engagement;
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
     * 
     * @param engagement
     * @return
     */
    public Engagement update(Engagement engagement) {

        String author = engagement.getLastUpdateByName();
        String authorEmail = engagement.getLastUpdateByEmail();
        String engagementUuid = engagement.getUuid();

        //TODO add here a call to validate that the user can update.
        //This should be from the activity service + verify non shanigans either
        String currentLastUpdated = engagement.getLastUpdate();

        //Will return a 404 with a message saying the uuid is not valid
        Engagement current = engagementApiClient.getEngagement(engagement.getUuid());
        Diff diff = javers.compare(current, engagement);
        if(diff.hasChanges()) {
            LOGGER.debug("Engagement changes {}", diff);
            engagementApiClient.updateEngagement(engagement);
        }

        nullToEmpty(engagement);

        List<HostingEnvironment> hes = hostingEnvironmentService.getHostingEnvironments(engagement.getUuid());
        diff = javers.compareCollections(hes, engagement.getHostingEnvironments(), HostingEnvironment.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Hosting Environment changes {}", diff);
            hostingEnvironmentService.updateAndReload(engagementUuid, engagement.getHostingEnvironments(),
                    Author.builder().email(authorEmail).name(author).build());
        }

        List<Artifact> artifacts = artifactService.getArtifacts(engagement.getUuid());
        diff = javers.compareCollections(artifacts, engagement.getArtifacts(), Artifact.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Artifacts has changes {}", diff);
            artifacts = artifactService.updateAndReload(engagement, author, authorEmail);
        }

        List<EngagementUser> participants = participantService.getParticipantsForEngagement(engagement.getUuid());
        diff = javers.compareCollections(new HashSet<>(participants), engagement.getEngagementUsers(), EngagementUser.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Participants changed {}", diff);
            participants = participantService.updateParticipantsAndReload(engagementUuid, author, authorEmail,
                    engagement.getEngagementUsers());
            LOGGER.debug("Updated {}", participants);

        }

        List<Category> categories = categoryApiClient.getCategories(engagement.getUuid());
        diff = javers.compareCollections(categories, engagement.getCategories(), Category.class);
        if(diff.hasChanges()) {
            LOGGER.debug("Categories changed {}", diff);
            Set<String> catString = new TreeSet<>();
            engagement.getCategories().forEach(c -> catString.add(c.getName()));
            categoryApiClient.updateCategories(engagementUuid, author, authorEmail, catString);
        }

        //TODO update cache
        return getEngagement(engagementUuid);
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
     * @param subdomain
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

        Engagement engagement = engagementApiClient.getEngagement(hook.getCustomerName(), hook.getEngagementName());

        // refresh entire engagement if requested
        if (hook.containsAnyMessage(commitFilteredMessages)) {
            activityService.postHook(hook);
            getEngagement(engagement.getUuid());
            LOGGER.debug("hook triggered refresh of engagement for project {}", hook.getProjectId());
            return;
        }

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
            //TODO ensure response contains latest
            //TODO invalidate cache
        }
    }


    /**
     * //Should prob deprecate this in favor of uuid. On create a uuid is returned
     * 
     * @param customerName customer namee
     * @param engagementName project name
     * @return engagement an engagement
     */
    public Engagement getByCustomerAndProjectName(String customerName, String engagementName) {
        return  engagementApiClient.getEngagement(customerName, engagementName);
    }

    public Map<EngagementState, Integer> getEngagementCountByStatus(Instant currentTime) {
        return engagementApiClient.getEngagementCounts();
    }

    /**
     * Returns a {@link PagedEngagementResults} of {@link Engagement} that matches
     * the {@link ListFilterOptions}.
     * 
     * @param listFilterOptions
     * @return ?
     */
    public Response getEngagementsPaged(ListFilterOptions listFilterOptions) {
        //TODO hacking for v1
        //TODO should probably better align last activity field on engagement object so that
        // we can just filter on engagement instead of hitting the activity service
        String sort = listFilterOptions.getSortFields().orElse("");
        int pageSize = listFilterOptions.getPerPage().orElse(5);
        if(pageSize == 5 && sort.equals("last_update")) {
            List<Commit> activity = activityService.getLatestActivity(0,5);
            List<Engagement> engagements = new ArrayList<>();
            activity.forEach(a -> {
                Engagement e = getEngagement(a.getEngagementUuid());
                e.setLastUpdate(a.getCommitDate());
                engagements.add(e);
            });
            return Response.ok(engagements).build();
        }
        //TODO not really paging here

        int page = listFilterOptions.getPage().orElse(1) - 1;
        pageSize = listFilterOptions.getPerPage().orElse(1000);
        return Response.status(200).entity(engagementApiClient.getEngagements(page, pageSize)).build();
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
        return useCaseApiClient.getUseCases(page, pageSize);
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
        return getEngagement(uuid); //TODO This method should happen via UUID not full engagement
    }

}