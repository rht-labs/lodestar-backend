package com.redhat.labs.omp.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.WebApplicationException;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.exception.InvalidRequestException;
import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Commit;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;
import com.redhat.labs.omp.model.Hook;
import com.redhat.labs.omp.model.Launch;
import com.redhat.labs.omp.model.Status;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.omp.model.event.EventType;
import com.redhat.labs.omp.repository.EngagementRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;
import com.redhat.labs.omp.socket.EngagementEventSocket;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "engagement.file.name", defaultValue = "engagement.json")
    String engagementFileName;
    @ConfigProperty(name = "engagement.file.branch", defaultValue = "master")
    String engagementFileBranch;
    @ConfigProperty(name = "engagement.file.commit.message", defaultValue = "updated by omp backend")
    String engagementFileCommitMessage;
    @ConfigProperty(name = "status.file")
    String statusFile;

    @Inject
    Jsonb jsonb;

    @Inject
    EngagementRepository repository;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Inject
    EngagementEventSocket socket;

    @Inject
    @RestClient
    OMPGitLabAPIService gitApi;

    /**
     * Creates a new {@link Engagement} resource in the data store and marks if for
     * asynchronous processing by the {@link GitSyncService}.
     * 
     * @param engagement
     * @return
     */

    public Engagement create(Engagement engagement) {

        // check if engagement exists
        Optional<Engagement> optional = get(engagement.getCustomerName(), engagement.getProjectName());
        if (optional.isPresent()) {
            throw new ResourceAlreadyExistsException("engagement already exists.  use PUT to update resource.");
        }

        // set modified info
        engagement.setAction(FileAction.create);

        // set last update
        engagement.setLastUpdate(getZuluTimeAsString());

        // persist to db
        repository.persist(engagement);

        return engagement;

    }

    /**
     * Updates the {@link Engagement} resource in the data store and marks it for
     * asynchronous processing by the {@link GitSyncService}.
     * 
     * @param customerName
     * @param projectName
     * @param engagement
     * @return
     */
    public Engagement update(String customerName, String projectName, Engagement engagement) {

        // check if engagement exists
        Optional<Engagement> optional = get(customerName, projectName);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        // set modified if already marked for modification
        Engagement persisted = optional.get();

        // mark as updated, if action not already assigned
        engagement.setAction((null != persisted.getAction()) ? persisted.getAction() : FileAction.update);

        // save the current last updated value and reset
        String currentLastUpdated = engagement.getLastUpdate();
        engagement.setLastUpdate(getZuluTimeAsString());

        // determine if launching
        boolean skipLaunch = (null != persisted.getLaunch());

        // update in db
        optional = repository.updateEngagementIfLastUpdateMatched(engagement, currentLastUpdated, skipLaunch);
        if (!optional.isPresent()) {
            throw new WebApplicationException(
                    "Failed to modify engagement because request contained stale data.  Please refresh and try again.",
                    HttpStatus.SC_CONFLICT);
        }

        // send updated engagement to socket
        sendEngagementEvent(jsonb.toJson(persisted));

        return optional.get();

    }

    // Status comes from gitlab so it does not need to to be sync'd
    public Engagement updateStatusAndCommits(Hook hook) {
        LOGGER.debug("Hook for {} {}", hook.getCustomerName(), hook.getEngagementName());

        Optional<Engagement> optional = get(hook.getCustomerName(), hook.getEngagementName());
        if (!optional.isPresent()) {
            // try gitlab in case of special chars
            optional = getEngagementFromNamespace(hook);
        }

        Engagement persisted = optional.get();

        if (hook.didFileChange(statusFile)) {
            Status status = gitApi.getStatus(hook.getCustomerName(), hook.getEngagementName());
            persisted.setStatus(status);
        }

        List<Commit> commits = gitApi.getCommits(hook.getCustomerName(), hook.getEngagementName());
        persisted.setCommits(commits);

        // set last update
        persisted.setLastUpdate(getZuluTimeAsString());

        // update in db
        repository.update(persisted);

        sendEngagementEvent(jsonb.toJson(persisted));

        return persisted;
    }

    private Optional<Engagement> getEngagementFromNamespace(Hook hook) {
        // Need the translated customer name if using special chars
        Engagement gitEngagement = gitApi.getEngagementByNamespace(hook.getProject().getPathWithNamespace());
        Optional<Engagement> optional = get(gitEngagement.getCustomerName(), gitEngagement.getProjectName());
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found. unable to update from hook.");
        }
        return optional;
    }

    /**
     * Returns an {@link Optional} containing an {@link Engagement} if it is present
     * in the data store. Otherwise, an empty {@link Optional} is returned.
     * 
     * @param customerId
     * @param projectId
     * @return
     */
    public Optional<Engagement> get(String customerName, String projectName) {

        Optional<Engagement> optional = Optional.empty();

        if (LOGGER.isTraceEnabled()) {
            repository.listAll().stream().forEach(
                    engagement -> LOGGER.trace("E {} {}", engagement.getCustomerName(), engagement.getProjectName()));
        }
        // check db
        Engagement persistedEngagement = repository.findByCustomerNameAndProjectName(customerName, projectName);

        if (null != persistedEngagement) {
            optional = Optional.of(persistedEngagement);
        }

        return optional;

    }

    /**
     * Returns a {@link List} of all {@link Engagement} in the data store.
     * 
     * @return
     */
    public List<Engagement> getAll() {
        return repository.listAll();
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
        List<Engagement> allEngagements = repository.findCustomerSuggestions(subString);

        Set<String> customers = new TreeSet<>();
        allEngagements.stream().forEach(engagement -> customers.add(engagement.getCustomerName()));

        return customers;
    }

    /**
     * Used by the {@link GitSyncService} to delete all {@link Engagement} from the
     * data store before re-populating from Git.
     */
    public void deleteAll() {
        long count = repository.deleteAll();
        LOGGER.info("removed '{}' engagements from the data store.", count);
    }

    /**
     * This should also update clients about the delete. Need infra here
     * 
     * @param customerName
     * @param engagementName
     */
    public void delete(String customerName, String engagementName) {
        Optional<Engagement> engagement = get(customerName, engagementName);
        if (engagement.isPresent()) {
            LOGGER.debug("Deleting engagement {} for customer {}", engagementName, customerName);
            repository.delete(engagement.get());
        }
    }

    /**
     * Updates the {@link List} of {@link Engagement} in the data store.
     * 
     * @param engagementList
     */
    void updateEngagementListInRepository(List<Engagement> engagementList) {

        // don't update last update already done as part of update
        repository.update(engagementList);

    }

    /**
     * Retrieves the {@link List} of {@link Engagement} from the Git API and then
     * calls the process to update the database.
     * 
     * @param purgeFirst
     */
    public void syncGitToDatabase(boolean purgeFirst) {

        // get all engagements from git
        vertx.<List<Engagement>>executeBlocking(promise -> {

            try {
                List<Engagement> engagementList = gitApi.getEngagments();
                promise.complete(engagementList);
            } catch (WebApplicationException wae) {
                promise.fail(wae);
            }

        }).subscribe().with(list -> {

            LOGGER.debug("found {} engagements in gitlab.", list.size());
            syncGitToDatabase(list, purgeFirst);

        }, failure -> {
            LOGGER.warn("failed to retrieve engagements from git. {}", failure.getMessage(), failure);
        });

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
        engagementList.stream()
                .filter(engagement -> !get(engagement.getCustomerName(), engagement.getProjectName()).isPresent())
                .forEach(engagement -> {
                    engagement.setLastUpdate(lastUpdate);
                    toInsert.add(engagement);
                });

        LOGGER.debug("engagementList size {}, toInsert size {}", engagementList.size(), toInsert.size());
        LOGGER.debug("inserting {} engagement into the database.", toInsert.size());

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

        // do not relaunch
        if (null != engagement.getLaunch()) {
            throw new InvalidRequestException("engagement has already been launched.");
        }

        // create new launch data for engagement
        engagement.setLaunch(Launch.builder().launchedDateTime(ZonedDateTime.now(ZoneId.of("Z")).toString())
                .launchedBy(engagement.getLastUpdateByName()).launchedByEmail(engagement.getLastUpdateByEmail())
                .build());

        // update db
        Engagement updated = update(engagement.getCustomerName(), engagement.getProjectName(), engagement);

        // sync change(s) to git
        sendEngagementsModifiedEvent();

        return updated;

    }

    /**
     * This method consumes a {@link BackendEvent}, which starts the process of
     * inserting any {@link Engagement} from GitLab that are not found in the
     * database.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.DB_REFRESH_REQUESTED_ADDRESS)
    void consumeDbRefreshRequestedEvent(BackendEvent event) {

        LOGGER.debug("consumed database refresh requested event.");
        syncGitToDatabase(false);

    }

    /**
     * Consumes a {@link BackendEvent} and updates the database using the
     * {@link List} of {@link Engagement}s that are contained in the event.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.UPDATE_ENGAGEMENTS_IN_DB_REQUESTED_ADDRESS)
    void consumeUpdateEngagementsInDbRequestedEvent(BackendEvent event) {
        updateEngagementListInRepository(event.getEngagementList());
    }

    /**
     * Consumes the {@link BackendEvent} and triggers the processing of any modified
     * {@link Engagement}s.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.PUSH_TO_GIT_REQUESTED_ADDRESS)
    void consumePushToGitRequestedEvent(BackendEvent event) {

        LOGGER.trace("consuming process time elapsed event.");

        sendEngagementsModifiedEvent();
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
    private String getZuluTimeAsString() {
        return ZonedDateTime.now(ZoneId.of("Z")).toString();
    }

}