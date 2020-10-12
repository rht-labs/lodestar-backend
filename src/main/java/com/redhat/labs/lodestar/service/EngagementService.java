package com.redhat.labs.lodestar.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.CreationDetails;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.FileAction;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.model.Status;
import com.redhat.labs.lodestar.model.event.BackendEvent;
import com.redhat.labs.lodestar.repository.EngagementRepository;
import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;
import com.redhat.labs.lodestar.socket.EngagementEventSocket;

import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    private static final String BACKEND_BOT = "lodestar-backend-bot";
    private static final String BACKEND_BOT_EMAIL = "lodestar-backend-bot@bot.com";

    @ConfigProperty(name = "status.file")
    String statusFile;

    @Inject
    Jsonb jsonb;

    @Inject
    EngagementRepository repository;

    @Inject
    EventBus eventBus;

    @Inject
    EngagementEventSocket socket;

    @Inject
    @RestClient
    LodeStarGitLabAPIService gitApi;

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

        setBeforeInsert(engagement);

        repository.persist(engagement);

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

        // set action
        setEngagementAction(engagement, FileAction.create);

        // set last update
        setLastUpdate(engagement);

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

        return repository.findByUiid(engagement.getUuid());

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

        validateCustomerAndProjectNames(engagement, existing);
        setBeforeUpdate(engagement);
        String currentLastUpdated = setLastUpdate(existing);
        boolean skipLaunch = skipLaunch(engagement);

        Engagement updated = repository.updateEngagementIfLastUpdateMatched(engagement, currentLastUpdated, skipLaunch)
                .orElseThrow(() -> new WebApplicationException(
                        "Failed to modify engagement because request contained stale data.  Please refresh and try again.",
                        HttpStatus.SC_CONFLICT));

        // send to socket
        sendEngagementEvent(jsonb.toJson(updated));

        return updated;

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

        // return if names have not changed
        if (toUpdate.getCustomerName().equals(existing.getCustomerName())
                && toUpdate.getProjectName().equals(existing.getProjectName())) {
            return;
        }

        try {
            getByCustomerAndProjectName(toUpdate.getCustomerName(), toUpdate.getProjectName());
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
     * Sets required {@link Engagement} attributes required before the update in to
     * the data store.
     * 
     * @param engagement
     */
    void setBeforeUpdate(Engagement engagement) {

        // mark as updated, if action not already assigned
        setEngagementAction(engagement, FileAction.update);

        // aggregate commit messages if already set
        if (null != engagement.getCommitMessage()) {

            // get existing message
            String existing = engagement.getCommitMessage();

            // if another message on current request, append to existing message
            String message = (null == engagement.getCommitMessage()) ? existing
                    : new StringBuilder(existing).append("\n\n").append(engagement.getCommitMessage()).toString();

            // set the message on the engagement before persisting
            engagement.setCommitMessage(message);

        }

    }

    /**
     * Sets the {@link FileAction} to the provided action if the {@link Engagement}
     * does not have it set.
     * 
     * @param engagement
     * @param action
     */
    void setEngagementAction(Engagement engagement, FileAction action) {
        // set only if action not already assigned
        engagement.setAction((null != engagement.getAction()) ? engagement.getAction() : action);
    }

    /**
     * Sets the last update timestamp on the provided {@link Engagement}. Returns
     * the prior value, which could be null.
     * 
     * @param engagement
     * @return
     */
    String setLastUpdate(Engagement engagement) {

        String currentLastUpdated = engagement.getLastUpdate();
        engagement.setLastUpdate(getZuluTimeAsString());

        return currentLastUpdated;

    }

    /**
     * Returns true if the {@link Launch} data is present on the {@link Engagement}.
     * Otherwise, false.
     * 
     * @param engagement
     * @return
     */
    boolean skipLaunch(Engagement engagement) {
        return (null != engagement.getLaunch());
    }

    /**
     * Updates the {@link Status} and {@link Commit} data on an {@link Engagement}.
     * 
     * @param hook
     * @return
     */
    public Engagement updateStatusAndCommits(Hook hook) {
        LOGGER.debug("Hook for {} {}", hook.getCustomerName(), hook.getEngagementName());

        // create engagement for get
        Engagement search = Engagement.builder().customerName(hook.getCustomerName())
                .projectName(hook.getEngagementName()).build();

        Engagement persisted = getByIdOrName(search).orElseGet(() -> getEngagementFromNamespace(hook));

        if (hook.didFileChange(statusFile)) {
            Status status = gitApi.getStatus(hook.getCustomerName(), hook.getEngagementName());
            persisted.setStatus(status);
        }

        List<Commit> commits = gitApi.getCommits(hook.getCustomerName(), hook.getEngagementName());
        persisted.setCommits(commits);

        // update in db
        repository.update(persisted);

        // send to socket
        sendEngagementEvent(jsonb.toJson(persisted));

        return persisted;
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
    public Engagement getByCustomerAndProjectName(String customerName, String projectName) {
        return repository.findByCustomerNameAndProjectName(customerName, projectName)
                .orElseThrow(() -> new WebApplicationException(
                        "no engagement found with customer:project " + customerName + ":" + projectName,
                        HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Returns an {@link Engagement} if it is present in the data store. Otherwise,
     * throws a Not FOUND {@link WebApplicationException}.
     * 
     * @param uuid
     * @return
     */
    public Engagement getByUuid(String uuid) {
        return repository.findByUiid(uuid).orElseThrow(
                () -> new WebApplicationException("no engagement found with id " + uuid, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Returns a {@link List} of all {@link Engagement} in the data store.
     * 
     * @return
     */
    public List<Engagement> getAll(String categories) {

        if (null == categories || categories.isBlank()) {
            return repository.listAll();
        }

        return Arrays.stream(categories.split(","))
                .flatMap(category -> repository.findEngagementsByCategory(category, false).stream())
                .collect(Collectors.toList());

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
    public Collection<String> getSuggestions(String subString) {

        return repository.findCustomerSuggestions(subString).stream().map(e -> e.getCustomerName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Used by the {@link GitSyncService} to delete all {@link Engagement} from the
     * data store before re-populating from Git.
     */
    void deleteAll() {
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
        repository.delete(getByCustomerAndProjectName(customerName, projectName));
    }

    /**
     * Removes an {@link Engagement} with the provided UUID from the data store.
     * Otherwise, throws a NOT FOUND {@link WebApplicationException}.
     * 
     * @param uuid
     */
    public void deleteByUuid(String uuid) {
        repository.delete(getByUuid(uuid));
    }

    /**
     * Returns a {@link List} of {@link Category} that match the provided
     * {@link String}. Returns all {@link Category} if no match {@link String}
     * provided.
     * 
     * @param optionalMatch
     * @return
     */
    public List<Category> getCategories(String match) {

        if (null == match || match.isBlank()) {
            return repository.findAllCategoryWithCounts();
        }

        return repository.findCategorySuggestions(match);

    }

    /**
     * Returns a {@link List} of Artifact Types as {@link String} that match the
     * provided input {@link String}. Otherwise, all Types are returned.
     * 
     * @param match
     * @return
     */
    public List<String> getArtifactTypes(String match) {

        if (null == match || match.isBlank()) {
            return repository.findAllArtifactTypes();
        }

        return repository.findArtifactTypeSuggestions(match);

    }

    /**
     * Updates the {@link List} of {@link Engagement} in the data store.
     * 
     * @param engagementList
     */
    public void updateProcessedEngagementListInRepository(List<Engagement> engagementList) {

        for (Engagement e : engagementList) {

            // get current engagement from db
            String customerName = e.getCustomerName();
            String projectName = e.getProjectName();
            Optional<Engagement> optional = getByIdOrName(e);
            if (optional.isPresent()) {

                Engagement persisted = optional.get();

                // always update creation details if missing
                Optional<CreationDetails> creationDetails = (null == persisted.getCreationDetails())
                        ? Optional.ofNullable(e.getCreationDetails())
                        : Optional.empty();

                // always update project id if missing
                Optional<Integer> projectId = (null == persisted.getProjectId()) ? Optional.ofNullable(e.getProjectId())
                        : Optional.empty();

                // reset action and commit message only if it has not changed since last push to
                // git; otherwise, keep values to allow new changes to be pushed to git
                boolean resetFlags = e.getLastUpdate().equals(persisted.getLastUpdate()) ? true : false;

                repository.updateEngagement(customerName, projectName, creationDetails, projectId, resetFlags);

            }

        }

    }

    /**
     * Sets a generated UUID value for each {@link Engagement} in the data store
     * that does not have a UUID. Also, triggers a push to Git to make sure the UUID
     * value is set in case of a data store refresh.
     */
    public void setNullUuids() {

        LOGGER.debug("{} with null uuids", repository.findByNullUuid().size());
        // get all engagements with null UUID
        repository.findByNullUuid().stream().map(e -> {
            setEngagementAction(e, FileAction.update);
            e.setUuid(UUID.randomUUID().toString());
            Optional<Engagement> o = repository.updateUuidForEngagement(e.getCustomerName(), e.getProjectName(),
                    e.getUuid(), FileAction.update.name(), BACKEND_BOT, BACKEND_BOT_EMAIL);
            LOGGER.debug("optional after uuid update {}", o);
            return e;
        });

    }

    /**
     * Retrieves the {@link List} of {@link Engagement} from the Git API and then
     * calls the process to update the database.
     * 
     * @param purgeFirst
     */
    public void syncGitToDatabase(boolean purgeFirst) {

        // get all engagements from git
        List<Engagement> engagementList = gitApi.getEngagments();
        // push engagements to db
        syncGitToDatabase(engagementList, purgeFirst);

    }

    /**
     * Inserts all {@link Engagement} from the {@link List} that are not already in
     * the database. If the purgeFirst flag is set, the database will be cleared
     * before the insert. {@link List} of {@link Engagement}.
     * 
     * @param engagementList
     */
    void syncGitToDatabase(List<Engagement> engagementList, boolean purgeFirst) {

        if (purgeFirst) {

            // remove all from database
            deleteAll();

        }

        // There's probably a better way to to this all on the mongo side. Should be
        // updated if possible.

        List<Engagement> toInsert = new ArrayList<>();
        String lastUpdate = getZuluTimeAsString();

        // filter any engagements that already exist and set modified
        engagementList.stream().filter(engagement -> !getByIdOrName(engagement).isPresent()).forEach(engagement -> {
            engagement.setLastUpdate(lastUpdate);
            toInsert.add(engagement);
        });

        LOGGER.debug("inserting {} engagements of {} into the database.", toInsert.size(), engagementList.size());

        // insert
        repository.persist(toInsert);

    }

    /**
     * Returns a {@link List} of {@link Engagement} where {@link FileAction} is set.
     * 
     * @return
     */
    List<Engagement> getModifiedEngagements() {
        return repository.findByModified();
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

        // send to socket
        sendEngagementEvent(jsonb.toJson(engagement));

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
     * If any {@link Engagement}s in the database have been modified, it creates a
     * {@link BackendEvent} and places it on the {@link EventBus} for processing.
     */
    void sendEngagementsModifiedEvent() {

        List<Engagement> modifiedList = getModifiedEngagements();

        if (modifiedList.isEmpty()) {
            LOGGER.debug("no modified engagements to process");
            return;
        }

        LOGGER.debug("emitting db engagements modified event");
        BackendEvent event = BackendEvent.createUpdateEngagementsInGitRequestedEvent(modifiedList);
        eventBus.sendAndForget(event.getEventType().getEventBusAddress(), event);

    }

    /**
     * Sends the given message to the configured socket sessions
     * 
     * @param message
     */
    void sendEngagementEvent(String message) {
        socket.broadcast(message);
    }

    /**
     * Returns {@link String} representation of the current Zulu time.
     * 
     * @return
     */
    String getZuluTimeAsString() {
        return ZonedDateTime.now(ZoneId.of("Z")).toString();
    }

}
