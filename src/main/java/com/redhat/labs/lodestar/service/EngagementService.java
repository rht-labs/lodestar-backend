package com.redhat.labs.lodestar.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.labs.lodestar.model.Artifact;
import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.CreationDetails;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.Engagement.EngagementState;
import com.redhat.labs.lodestar.model.EngagementAttribute;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.EngagementUserSummary;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.Score;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.UseCase;
import com.redhat.labs.lodestar.model.event.EventType;
import com.redhat.labs.lodestar.model.filter.FilterOptions;
import com.redhat.labs.lodestar.model.filter.ListFilterOptions;
import com.redhat.labs.lodestar.model.pagination.PagedArtifactResults;
import com.redhat.labs.lodestar.model.pagination.PagedCategoryResults;
import com.redhat.labs.lodestar.model.pagination.PagedEngagementResults;
import com.redhat.labs.lodestar.model.pagination.PagedHostingEnvironmentResults;
import com.redhat.labs.lodestar.model.pagination.PagedScoreResults;
import com.redhat.labs.lodestar.model.pagination.PagedStringResults;
import com.redhat.labs.lodestar.model.pagination.PagedUseCaseResults;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.ConfigApiClient;
import com.redhat.labs.lodestar.rest.client.LodeStarGitApiClient;

import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    private static final String BACKEND_BOT = "lodestar-backend-bot";
    private static final String BACKEND_BOT_EMAIL = "lodestar-backend-bot@bot.com";

    @ConfigProperty(name = "status.file")
    List<String> statusFile;

    @ConfigProperty(name = "commit.msg.filter.list", defaultValue = "not.set")
    List<String> commitFilteredMessages;

    @Inject
    Jsonb jsonb;

    @Inject
    EngagementRepository repository;

    @Inject
    EventBus eventBus;

    @Inject
    @RestClient
    LodeStarGitApiClient gitApi;
    
    @Inject
    @RestClient
    ConfigApiClient configApi;

    /**
     * Creates a new {@link Engagement} resource in the data store and marks if for
     * asynchronous processing by the {@link GitSyncService}.
     * 
     * @param engagement
     * @return
     */
    public Engagement create(Engagement engagement) {

        cleanEngagement(engagement);

        if (getByIdOrName(engagement).isPresent()) {
            throw new WebApplicationException("engagement already exists, use PUT to update resource",
                    HttpStatus.SC_CONFLICT);
        }

        validateHostingEnvironments(engagement.getHostingEnvironments());
        validateSubdomainOnCreate(engagement);
        setBeforeInsert(engagement);

        // create copy to send to git api
        Engagement copy = clone(engagement);

        // reset commit message
        engagement.setCommitMessage(null);

        // save to database
        repository.persist(engagement);

        // send create engagement event after save to database
        eventBus.sendAndForget(EventType.CREATE_ENGAGEMENT_EVENT_ADDRESS, copy);

        return engagement;

    }

    /**
     * Sets required {@link Engagement} attributes required before the initial
     * insert in to the data store.
     * 
     * @param engagement
     */
    void setBeforeInsert(Engagement engagement) {

        // set uuid
        engagement.setUuid(UUID.randomUUID().toString());

        // set last update
        setLastUpdate(engagement);

        // set creation details
        setCreationDetails(engagement);

    }

    /**
     * Sets the {@link CreationDetails} on the given {@link Engagement}.
     * 
     * @param engagement
     */
    void setCreationDetails(Engagement engagement) {

        // set creation details
        CreationDetails creationDetails = CreationDetails.builder().createdByUser(engagement.getLastUpdateByName())
                .createdByEmail(engagement.getLastUpdateByEmail())
                .createdOn(ZonedDateTime.now(ZoneId.of("Z")).toString()).build();
        engagement.setCreationDetails(creationDetails);

    }

    /**
     * Performs any data cleaning on the provided {@link Engagement}.
     * 
     * @param engagement
     */
    void cleanEngagement(Engagement engagement) {

        // trim whitespace from customer and project names
        engagement.setCustomerName(engagement.getCustomerName().trim());
        engagement.setProjectName(engagement.getProjectName().trim());

        // names cannot be greater than 255 in length because of gitlab
        validateNameLength(engagement.getCustomerName());
        validateNameLength(engagement.getProjectName());

    }

    /**
     * Returns an {@link Optional} containing an {@link Engagement} if one is found.
     * Search uses UUID if provided or combination of customer and project names
     * otherwise.
     * 
     * @param engagement
     * @return
     */
    Optional<Engagement> getByIdOrName(Engagement engagement) {

        if (null == engagement.getUuid()) {
            return repository.findByCustomerNameAndProjectName(engagement.getCustomerName(),
                    engagement.getProjectName());
        }

        return repository.findByUuid(engagement.getUuid());

    }

    /**
     * Updates the {@link Engagement} resource in the data store and marks it for
     * asynchronous processing by the {@link GitSyncService}.
     * 
     * @param engagement
     * @return
     */
    public Engagement update(Engagement engagement) {

        Engagement existing = getByIdOrName(engagement).orElseThrow(
                () -> new WebApplicationException("no engagement found, use POST to create", HttpStatus.SC_NOT_FOUND));

        String currentLastUpdated = engagement.getLastUpdate();

        validateHostingEnvironments(engagement.getHostingEnvironments());
        validateSubdomainOnUpdate(engagement);
        validateCustomerAndProjectNames(engagement, existing);
        setBeforeUpdate(engagement, existing);

        // create copy to send to git api
        Engagement copy = clone(engagement);

        // reset values before save
        engagement.setCommitMessage(null);
        if (null != engagement.getEngagementUsers()) {
            engagement.getEngagementUsers().stream().forEach(u -> u.setReset(false));
        }

        Engagement updated = repository.updateEngagement(engagement, currentLastUpdated)
                .orElseThrow(() -> new WebApplicationException(
                        "Failed to modify engagement because request contained stale data.  Please refresh and try again.",
                        HttpStatus.SC_CONFLICT));

        // send update engagement event once saved
        eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, copy);
        
        String message = String.format("%s,%s,%s", engagement.getUuid(), engagement.getLastUpdateByEmail(), engagement.getLastUpdateByName());

        if(commitMessageContains(copy, "engagement_users")) {
            eventBus.sendAndForget(EventType.UPDATE_PARTICIPANTS_EVENT_ADDESS, message);
        }
        
        if(commitMessageContains(copy, "artifacts")) {
            eventBus.sendAndForget(EventType.UPDATE_ARTIFACTS_EVENT_ADDRESS, message);
        }

        return updated;

    }
    
    private boolean commitMessageContains(Engagement engagement, String containee) {
        return engagement.getCommitMessage() != null 
                && engagement.getCommitMessage().contains(containee);
    }

    /**
     * Throws a {@link WebApplicationException} if the Customer or Project name has
     * changed and there is already an {@link Engagement} that has the
     * customer/project combination.
     * 
     * @param toUpdate
     * @param existing
     */
    void validateCustomerAndProjectNames(Engagement toUpdate, Engagement existing) {

        // names cannot be greater than 255 in length because of gitlab
        validateNameLength(toUpdate.getCustomerName());
        validateNameLength(toUpdate.getProjectName());

        // return if names have not changed
        if (toUpdate.getCustomerName().equals(existing.getCustomerName())
                && toUpdate.getProjectName().equals(existing.getProjectName())) {
            return;
        }

        try {
            getByCustomerAndProjectName(toUpdate.getCustomerName(), toUpdate.getProjectName(), new FilterOptions());
        } catch (WebApplicationException wae) {
            // return if not found
            return;
        }

        // throw exception if names changed to match an existing project
        throw new WebApplicationException("failed to change name(s).  engagement with customer name '"
                + toUpdate.getCustomerName() + "' and project '" + toUpdate.getProjectName() + "' already exists.",
                409);

    }

    /**
     * Throws a {@link WebApplicationException} if the provided name is greater than
     * 255 characters.
     * 
     * @param name
     */
    void validateNameLength(String name) {

        if (name.length() > 255) {
            throw new WebApplicationException("names cannot be greater than 255 characters.",
                    HttpStatus.SC_BAD_REQUEST);
        }

    }

    /**
     * Throws {@link WebApplicationException} if the supplied {@link List} of
     * {@link HostingEnvironment} contains duplicate subdomain.
     * 
     * @param heList
     */
    void validateHostingEnvironments(List<HostingEnvironment> heList) {

        if (null != heList) {

            List<String> duplicateSubdomains = heList.stream().filter(he -> null != he.getOcpSubDomain())
                    .collect(Collectors.groupingBy(HostingEnvironment::getOcpSubDomain, Collectors.counting()))
                    .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
                    .collect(Collectors.toList());

            if (!duplicateSubdomains.isEmpty()) {
                throw new WebApplicationException(
                        "supplied hosting environments has duplicate subdomains for entries " + duplicateSubdomains,
                        400);
            }

        }

    }

    /**
     * Throws {@link WebApplicationException} if the supplied subdomain is used by
     * another {@link Engagement}.
     * 
     * @param engagement
     */
    void validateSubdomainOnCreate(Engagement engagement) {

        if (null != engagement.getHostingEnvironments()) {

            // validate each subdomain
            List<String> subdomainsInUse = engagement.getHostingEnvironments().stream()
                    .filter(env -> null != env.getOcpSubDomain())
                    .filter(env -> doesSubdomainExist(env.getOcpSubDomain())).map(HostingEnvironment::getOcpSubDomain)
                    .collect(Collectors.toList());

            if (!subdomainsInUse.isEmpty()) {
                throw new WebApplicationException(
                        String.format("The following subdomains are already in use: %s", subdomainsInUse),
                        HttpStatus.SC_CONFLICT);
            }

        }

    }

    /**
     * Throws {@link WebApplicationException} if the supplied subdomain is different
     * from the persisted domain and another {@link Engagement} is already using it.
     * 
     * @param toUpdate
     * 
     */
    void validateSubdomainOnUpdate(Engagement toUpdate) {

        if (null != toUpdate.getHostingEnvironments()) {

            List<String> subdomainsInUse = toUpdate.getHostingEnvironments().stream()
                    .filter(he -> null != he.getOcpSubDomain())
                    .filter(he -> repository
                            .findBySubdomain(he.getOcpSubDomain(), Optional.ofNullable(toUpdate.getUuid())).isEmpty())
                    .filter(he -> repository.findBySubdomain(he.getOcpSubDomain()).isPresent())
                    .map(HostingEnvironment::getOcpSubDomain).collect(Collectors.toList());

            LOGGER.debug("subdomains in use: {}", subdomainsInUse);

            if (!subdomainsInUse.isEmpty()) {
                throw new WebApplicationException(
                        String.format("The following subdomains are already in use: %s", subdomainsInUse),
                        HttpStatus.SC_CONFLICT);
            }

        }

    }

    /**
     * Return false if subdomain is null, blank, or is not found in the data store.
     * Otherwise, true.
     * 
     * @param subdomain
     */
    boolean doesSubdomainExist(String subdomain) {
        return getBySubdomain(subdomain).isPresent();
    }

    /**
     * Returns Optional containing {@link Engagement} if found with given subdomain.
     * 
     * @param subdomain
     * @return
     */
    public Optional<Engagement> getBySubdomain(String subdomain) {
        return (subdomain == null || subdomain.isBlank()) ? Optional.empty() : repository.findBySubdomain(subdomain);
    }

    /**
     * Sets required {@link Engagement} attributes required before the update in to
     * the data store.
     * 
     * @param engagement
     * @param existing
     */
    void setBeforeUpdate(Engagement engagement, Engagement existing) {

        setImmutableFieldsOnUpdate(engagement, existing);

        setLastUpdate(engagement);

        // create new or use existing uuids for users
        setUserUuidsBeforeUpdate(engagement, existing);

        // aggregate commit messages if already set
        if (null != existing.getCommitMessage()) {

            // get existing message
            String existingMessage = existing.getCommitMessage();

            // if another message on current request, append to existing message
            String message = (null == engagement.getCommitMessage()) ? existingMessage
                    : new StringBuilder(existingMessage).append("\n\n").append(engagement.getCommitMessage())
                            .toString();

            // set the message on the engagement before persisting
            engagement.setCommitMessage(message);

        }

        // set ids and/or time stamps for modified attributes
        setIdsAndTimestamps(engagement, existing);

    }

    /**
     * Uses values from the existing {@link Engagement} to set fields that should be
     * immutable.
     * 
     * @param engagement
     * @param existing
     */
    void setImmutableFieldsOnUpdate(Engagement engagement, Engagement existing) {

        // uuid
        engagement.setUuid(existing.getUuid());

        // mongo id
        engagement.setMongoId(existing.getMongoId());

        // project id
        engagement.setProjectId(existing.getProjectId());

        // creation details
        engagement.setCreationDetails(existing.getCreationDetails());

        // status
        engagement.setStatus(existing.getStatus());

        // commits
        engagement.setCommits(existing.getCommits());

        // launch
        if (null != engagement.getLaunch() &&  null != existing.getLaunch()) {
            engagement.setLaunch(existing.getLaunch());
        }

    }

    /**
     * Updates the UUID and/or timestamps if the {@link Artifact}, {@link Category},
     * {@link UseCase}, {@link Score}, or {@link HostingEnvironment} is new or has
     * been modified.
     * 
     * @param engagement
     * @param existing
     */
    void setIdsAndTimestamps(Engagement engagement, Engagement existing) {

        // categories
        setIdAndTimstampsOnEngagementAttribute(engagement.getCategories(), existing.getCategories());

        // artifacts
        setIdAndTimstampsOnEngagementAttribute(engagement.getArtifacts(), existing.getArtifacts());

        // use cases
        setIdAndTimstampsOnEngagementAttribute(engagement.getUseCases(), existing.getUseCases());

        // scores
        setIdAndTimstampsOnEngagementAttribute(engagement.getScores(), existing.getScores());

        // hosting environments
        setIdAndTimstampsOnEngagementAttribute(engagement.getHostingEnvironments(), existing.getHostingEnvironments());

    }

    /**
     * For each {@link EngagementAttribute} in the {@link List} of incoming, if the
     * attribute is new the UUID, created and updated timestamps will be set. If the
     * attribute exists in the {@link List} of existing, the updated timestamp will
     * be updated in it has been modified. Otherwise, nothing will be updated.
     * 
     * @param incoming
     * @param existing
     */
    void setIdAndTimstampsOnEngagementAttribute(List<? extends EngagementAttribute> incoming,
            List<? extends EngagementAttribute> existing) {

        // nothing to do if incoming is null
        if (null == incoming) {
            return;
        }

        // all new, set id and time stamps
        if (existing == null) {
            setIdAndTimestampsOnEngagementAttributes(incoming);
            return;
        }

        // check if each attribute in incoming already exists, if true, and modified,
        // set updated ts
        incoming.stream().forEach(ia -> {

            // need list of new and already exists
            Optional<? extends EngagementAttribute> optional = existing.stream()
                    .filter(ea -> null != ia.getUuid() && ia.getUuid().equals(ea.getUuid())).findAny();
            if (optional.isPresent()) {

                // set updated ts if modified
                if (!ia.equals(optional.get())) {
                    ia.setUpdated();
                }

            } else {

                setIdAndTimestamps(ia);

            }

        });

    }

    /**
     * Sets a {@link UUID} for new {@link EngagementUser} or uses existing
     * {@link UUID} for existing {@link EngagementUser}s.
     * 
     * @deprecated Migrated to participant service. It is safe to remove this when no longer
     * persisting users to engagements. 
     * 
     * @param engagement
     * @param existing
     */
    @Deprecated
    void setUserUuidsBeforeUpdate(Engagement engagement, Engagement existing) {

        Set<EngagementUser> incomingUsers = engagement.getEngagementUsers();
        Set<EngagementUser> existingUsers = existing.getEngagementUsers();

        // do nothing if no users incoming
        if (null == incomingUsers || incomingUsers.isEmpty()) {
            return;
        }

        incomingUsers.stream().forEach(user -> {

            // find existing user with matching email
            Optional<EngagementUser> optionalUser = (null == existingUsers) ? Optional.empty()
                    : existingUsers.stream().filter(eUser -> eUser.getEmail().equals(user.getEmail())).findFirst();

            if (optionalUser.isPresent()) {
                // set uuid to that of existing user
                user.setUuid(optionalUser.get().getUuid());
            } else {
                // set uuid to new uuid for new users
                user.setUuid(UUID.randomUUID().toString());
            }

        });

    }

    /**
     * Sets the last update timestamp on the provided {@link Engagement}. Returns
     * the prior value, which could be null.
     * 
     * @param engagement
     * @return
     */
    void setLastUpdate(Engagement engagement) {
        engagement.setLastUpdate(getZuluTimeAsString());
    }

    /**
     * Sets the project ID for the {@link Engagement} with the matching UUID.
     * 
     * @param uuid
     * @param projectId
     */
    public void setProjectId(String uuid, Integer projectId) {
        repository.setProjectId(uuid, projectId);
    }

    /**
     * Updates the {@link Status} and {@link Commit} data on an {@link Engagement}.
     * 
     * TODO Commits should be removed from the engagement API when the FE has implemented the changes
     * 
     * @param hook
     * @return
     */
    public void updateStatusAndCommits(Hook hook) {
        LOGGER.debug("Hook for {} {}", hook.getCustomerName(), hook.getEngagementName());

        // refresh entire engagement if requested
        if (hook.containsAnyMessage(commitFilteredMessages)) {
            LOGGER.debug("hook triggered refresh of engagement for project {}", hook.getProjectId());
            syncGitToDatabase(false, null, String.valueOf(hook.getProjectId()));
            return;
        }

        // create engagement for get
        Engagement search = Engagement.builder().customerName(hook.getCustomerName())
                .projectName(hook.getEngagementName()).build();

        Engagement persisted = getByIdOrName(search).orElseGet(() -> getEngagementFromNamespace(hook));

        // send update status event
        if (hook.didFileChange(statusFile)) {
            eventBus.sendAndForget(EventType.UPDATE_STATUS_EVENT_ADDRESS, persisted);
        }

        // send update commits event
        eventBus.sendAndForget(EventType.UPDATE_COMMITS_EVENT_ADDRESS, persisted);

    }

    /**
     * Updates the {@link Status} for the given UUID in the database.
     * 
     * @param uuid
     * @param status
     */
    public void setStatus(String uuid, Status status) {
        LOGGER.trace("\tupdating {} with status {}", uuid, status);
        repository.setStatus(uuid, status);
    }

    /**
     * Updates the {@link List} of {@link Commit}s for the given UUID in the
     * database.
     * 
     * @param uuid
     * @param commits
     */
    public void setCommits(String uuid, List<Commit> commits) {
        LOGGER.trace("\tupdating {} with {} commits.", uuid, commits.size());
        repository.setCommits(uuid, commits);
    }

    /**
     * Returns the {@link Engagement} using the namespace from the provided
     * {@link Hook}.
     * 
     * @param hook
     * @return
     */
    Engagement getEngagementFromNamespace(Hook hook) {
        // Need the translated customer name if using special chars
        Engagement gitEngagement = gitApi.getEngagementByNamespace(hook.getProject().getPathWithNamespace());
        return getByIdOrName(gitEngagement)
                .orElseThrow(() -> new WebApplicationException("no engagement found. unable to update from hook.",
                        HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Returns an {@link Optional} containing an {@link Engagement} if it is present
     * in the data store. Otherwise, an empty {@link Optional} is returned.
     * 
     * @param customerId
     * @param projectId
     * @return
     */
    public Engagement getByCustomerAndProjectName(String customerName, String projectName, FilterOptions options) {
        return repository.findByCustomerNameAndProjectName(customerName, projectName, options)
                .orElseThrow(() -> new WebApplicationException(
                        "no engagement found with customer:project " + customerName + ":" + projectName,
                        HttpStatus.SC_NOT_FOUND));
    }

    public Map<EngagementState, Integer> getEngagementCountByStatus(LocalDateTime currentTime) {

        List<Engagement> engagementList = repository.listAll();
        Map<EngagementState, Integer> statusCounts = new EnumMap<>(EngagementState.class);

        for (Engagement engagement : engagementList) {
            EngagementState state = engagement.getEngagementCurrentState(currentTime);

            int count = statusCounts.containsKey(state) ? statusCounts.get(state) + 1 : 1;
            statusCounts.put(state, count);
        }

        statusCounts.put(EngagementState.ANY, engagementList.size());

        return statusCounts;
    }

    /**
     * Returns an {@link Engagement} if it is present in the data store. Otherwise,
     * throws a Not FOUND {@link WebApplicationException}.
     * 
     * @param uuid
     * @return
     */
    public Engagement getByUuid(String uuid, FilterOptions options) {
        return repository.findByUuid(uuid, options).orElseThrow(
                () -> new WebApplicationException("no engagement found with id " + uuid, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Returns a {@link PagedEngagementResults} of {@link Engagement} that matches
     * the {@link ListFilterOptions}.
     * 
     * @param listFilterOptions
     * @return
     */
    public PagedEngagementResults getEngagementsPaged(ListFilterOptions listFilterOptions) {
        return repository.findPagedEngagements(listFilterOptions);
    }

    /**
     * Returns a {@link EngagementUserSummary} given the supplied
     * {@link ListFilterOptions}.
     * 
     * @param listFilterOptions
     * @return
     */
    public EngagementUserSummary getUserSummary(ListFilterOptions listFilterOptions) {
        return repository.findEngagementUserSummary(listFilterOptions);
    }

    /**
     * Returns a {@link List} of all customer names in the data store that match the
     * input
     * 
     * @param subString - A string to match the customer name on. Can be all or
     *                  part. case-insensitive
     * @return a {@link List} of all customer names in the data store that match the
     *         input
     */
    public PagedStringResults getSuggestions(ListFilterOptions filterOptions) {
        return repository.findCustomerSuggestions(filterOptions);
    }

    /**
     * Used by the {@link GitSyncService} to delete all {@link Engagement} from the
     * data store before re-populating from Git.
     */
    public void deleteAll() {
        repository.deleteAll();
    }

    /**
     * Removes an {@link Engagement} with the provided customer and project name
     * from the data store. Otherwise, throws a NOT FOUND
     * {@link WebApplicationException}.
     * 
     * @param customerName
     * @param projectName
     */
    public void deleteByCustomerAndProjectName(String customerName, String projectName) {
        repository.delete(getByCustomerAndProjectName(customerName, projectName, new FilterOptions()));
    }

    /**
     * Removes an {@link Engagement} with the provided UUID from the data store.
     * Otherwise, throws a NOT FOUND {@link WebApplicationException}.
     * 
     * @param uuid
     */
    public void deleteByUuid(String uuid) {
        repository.delete(getByUuid(uuid, new FilterOptions()));
    }

    /**
     * Deletes the {@link Engagement} from the database if it exists and not already
     * launched. Then, sends an event to remove the engagement from Git.
     * 
     * @param uuid
     */
    public void deleteEngagement(String uuid) {

        // get engagement by uuid
        Engagement engagement = getByUuid(uuid, new FilterOptions());

        // throw 400 if already launched
        if (isLaunched(engagement)) {
            throw new WebApplicationException("cannot delete engagement that has already been launched.",
                    HttpStatus.SC_BAD_REQUEST);
        }

        // delete from db
        repository.delete(engagement);

        // send delete event
        eventBus.sendAndForget(EventType.DELETE_ENGAGEMENT_EVENT_ADDRESS, engagement);

    }

    /**
     * Returns a {@link List} of {@link Category} that match the provided
     * {@link ListFilterOptions}}. Returns all {@link Category} if no
     * {@link ListFilterOptions}.
     * 
     * @param options
     * @return
     */
    public PagedCategoryResults getCategories(ListFilterOptions options) {
        return repository.findCategories(options);
    }

    /**
     * Returns a {@link List} of Artifact Types as {@link String} that match the
     * provided {@link ListFilterOptions}. Otherwise, all Types are returned.
     * 
     * @param match
     * @return
     */
    public PagedStringResults getArtifactTypes(ListFilterOptions filterOptions) {
        return repository.findArtifactTypes(filterOptions);
    }

    /**
     * Returns a {@link PagedArtifactResults} that match the provided
     * {@link ListFilterOptions}. Otherwise, all {@link Artifact}s returned.
     * 
     * @param filterOptions
     * @return
     */
    public PagedArtifactResults getArtifacts(ListFilterOptions filterOptions) {
        return repository.findArtifacts(filterOptions);
    }

    public PagedHostingEnvironmentResults getHostingEnvironments(ListFilterOptions filterOptions) {
        return repository.findHostingEnvironments(filterOptions);
    }

    public PagedScoreResults getScores(ListFilterOptions filterOptions) {
        return repository.findScores(filterOptions);
    }

    public PagedUseCaseResults getUseCases(ListFilterOptions filterOptions) {
        return repository.findUseCases(filterOptions);
    }

    /**
     * Sets a generated UUID value for each {@link Engagement} or
     * {@link EngagementUser} in the data store that does not have a UUID. Also,
     * triggers a push to Git to make sure the UUID value(s) are set in case of a
     * data store refresh.
     */
    public long setNullUuids() {

        // update UUIDs on engagements and engagment users if missing
        List<Engagement> updated = repository.streamAll().filter(this::uuidUpdated).map(e -> {
            e.setLastUpdateByName(BACKEND_BOT);
            e.setLastUpdateByEmail(BACKEND_BOT_EMAIL);
            LOGGER.debug("uuid(s) updated for enagement {}", e.getUuid());
            return e;
        }).collect(Collectors.toList());

        // send updates to git api
        updated.stream().forEach(e -> eventBus.sendAndForget(EventType.UPDATE_ENGAGEMENT_EVENT_ADDRESS, e));

        long count = updated.size();

        repository.update(updated);

        return count;

    }

    /**
     * Returns true if a UUID was set on the {@link Engagement} or any
     * {@link EngagementUser}. Otherwise, false.
     * 
     * @param engagement
     * @return
     */
    boolean uuidUpdated(Engagement engagement) {

        // set engagement uuid if required
        boolean eUuidSet = setUuidOnEngagement(engagement);

        // set engagement user(s) uuids if required
        boolean uUuidSet = setUuidOnUsers(engagement.getEngagementUsers());

        // set other attribute uuids if required
        boolean aUuidSet = setUuidOnEngagementAttributes(engagement);

        return eUuidSet || uUuidSet || aUuidSet;

    }

    /**
     * Returns true if the UUID for the {@link Engagement} was set. Otherwise,
     * returns false.
     * 
     * @param engagement
     * @return
     */
    boolean setUuidOnEngagement(Engagement engagement) {

        if (null == engagement.getUuid()) {
            engagement.setUuid(UUID.randomUUID().toString());
            LOGGER.debug("set engagement uuid to {} for {}:{}", engagement.getUuid(), engagement.getCustomerName(),
                    engagement.getProjectName());
            return true;
        }

        return false;

    }

    /**
     * Returns true if any {@link EngagementUser} had a UUID set. Otherwise, returns
     * false.
     * 
     * @param engagementUsers
     * @return
     */
    boolean setUuidOnUsers(Set<EngagementUser> engagementUsers) {

        if (null != engagementUsers) {

            Set<Boolean> result = engagementUsers.stream().filter(user -> null == user.getUuid()).map(user -> {
                user.setUuid(UUID.randomUUID().toString());
                LOGGER.debug("set uuid for user {}, to {}", user.getEmail(), user.getUuid());
                return Boolean.TRUE;
            }).collect(Collectors.toSet());

            return result.contains(Boolean.TRUE);

        }

        return false;

    }

    /**
     * Returns true if any {@link Artifact}, {@link Category}, {@link UseCase},
     * {@link Score}, or {@link HostingEnvironment} has its UUID or timestamps
     * updated. Otherwise, false.
     * 
     * @param engagement
     * @return
     */
    boolean setUuidOnEngagementAttributes(Engagement engagement) {

        // artifacts
        boolean aModified = setIdAndTimestampsOnEngagementAttributes(engagement.getArtifacts());

        // categories
        boolean cModified = setIdAndTimestampsOnEngagementAttributes(engagement.getCategories());

        // use cases
        boolean uModified = setIdAndTimestampsOnEngagementAttributes(engagement.getUseCases());

        // scores
        boolean sModified = setIdAndTimestampsOnEngagementAttributes(engagement.getScores());

        // hosting environments
        boolean hModified = setIdAndTimestampsOnEngagementAttributes(engagement.getHostingEnvironments());

        return aModified || cModified || uModified || sModified || hModified;

    }

    /**
     * Returns true if any {@link EngagementAttribute} has its UUID or timestamps
     * updated. Otherwise, false.
     * 
     * @param attributes
     * @return
     */
    boolean setIdAndTimestampsOnEngagementAttributes(List<? extends EngagementAttribute> attributes) {

        if (null != attributes) {
            return attributes.stream().map(this::setIdAndTimestamps).collect(Collectors.toSet()).contains(Boolean.TRUE);
        }

        return false;

    }

    /**
     * Returns true if the UUID is set and the created and updated timestamps are
     * set. Otherwise, false.
     * 
     * @param attribute
     * @return
     */
    Boolean setIdAndTimestamps(EngagementAttribute attribute) {

        if (null == attribute.getUuid()) {

            // generate id
            attribute.generateId();
            // set both created and updated
            attribute.setCreatedAndUpdated();

            return Boolean.TRUE;

        }

        return Boolean.FALSE;

    }

    /**
     * If UUID provided, send event to refresh the {@link Engagement} that matches
     * the provided UUID. If no UUID is provided, all engagements will be refreshed
     * using the Git data. If purgeFirst is true, the data in the database will be
     * removed before the insert.
     * 
     * @param purgeFirst
     * @param uuid
     */
    public void syncGitToDatabase(boolean purgeFirst, String uuid, String projectId) {

        if (null != uuid) {

            // find in database or throw 404
            Engagement engagement = getByUuid(uuid, new FilterOptions());

            // send event for processing
            eventBus.sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS,
                    String.valueOf(engagement.getProjectId()));

        } else if (null != projectId) {

            // send event for processing
            eventBus.sendAndForget(EventType.DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS, projectId);

        } else if (purgeFirst) {

            eventBus.sendAndForget(EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS,
                    EventType.DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS);

        } else {

            eventBus.sendAndForget(EventType.LOAD_DATABASE_EVENT_ADDRESS, EventType.LOAD_DATABASE_EVENT_ADDRESS);

        }

    }

    /**
     * Persists the given {@link Engagement} into the database if not found.
     * 
     * @param engagement
     */
    public boolean persistEngagementIfNotFound(Engagement engagement) {

        if (getByIdOrName(engagement).isEmpty()) {
            LOGGER.trace("persisting engagment {}:{}:{}", engagement.getUuid(), engagement.getCustomerName(),
                    engagement.getProjectName());

            engagement.setLastUpdate(getZuluTimeAsString());
            repository.persist(engagement);

            return true;

        }

        return false;

    }

    /**
     * Adds {@link Launch} data to the given {@link Engagement} and uses
     * {@link GitSyncService} to process the modified {@link Engagement}.
     * 
     * @param engagement
     * @return
     */
    public Engagement launch(Engagement engagement) {

        if (isLaunched(engagement)) {
            throw new WebApplicationException("engagement has already been launched.", HttpStatus.SC_BAD_REQUEST);
        }

        engagement.setLaunch(createLaunchInstance(engagement.getLastUpdateByName(), engagement.getLastUpdateByEmail()));

        update(engagement);
        return engagement;

    }

    /**
     * Returns a {@link Launch} instance based on the current time.
     * 
     * @param launchedBy
     * @param launchedByEmail
     * @return
     */
    Launch createLaunchInstance(String launchedBy, String launchedByEmail) {
        return Launch.builder().launchedDateTime(getZuluTimeAsString()).launchedBy(launchedBy)
                .launchedByEmail(launchedByEmail).build();
    }

    /**
     * Returns true if {@link Launch} exists on provided {@link Engagement}.
     * Otherwise, false.
     * 
     * @param engagement
     * @return
     */
    boolean isLaunched(Engagement engagement) {
        return null != engagement.getLaunch();
    }

    /**
     * Returns {@link String} representation of the current Zulu time.
     * 
     * @return
     */
    String getZuluTimeAsString() {
        return ZonedDateTime.now(ZoneId.of("Z")).toString();
    }

    /**
     * Uses {@link ObjectMapper} to create a deep copy of the given
     * {@link Engagement}.
     * 
     * @param toClone
     * @return
     */
    Engagement clone(Engagement toClone) {
        return jsonb.fromJson(jsonb.toJson(toClone), Engagement.class);
    }

}