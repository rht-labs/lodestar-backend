package com.redhat.labs.omp.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.omp.exception.InvalidRequestException;
import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;
import com.redhat.labs.omp.model.Launch;
import com.redhat.labs.omp.model.Status;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.omp.model.event.EventType;
import com.redhat.labs.omp.repository.EngagementRepository;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;
import com.redhat.labs.omp.socket.EngagementEventSocket;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class EngagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);
    private static final String ENGAGEMENTS_UPDATED_ADDRESS = "engagements.updated.event";

    @ConfigProperty(name = "engagement.file.name", defaultValue = "engagement.json")
    String engagementFileName;
    @ConfigProperty(name = "engagement.file.branch", defaultValue = "master")
    String engagementFileBranch;
    @ConfigProperty(name = "engagement.file.commit.message", defaultValue = "updated by omp backend")
    String engagementFileCommitMessage;

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

        // set id to persisted id
        engagement.setMongoId(persisted.getMongoId());

        // set engagement id in case it was updated before the consumer refreshed
        engagement.setProjectId(persisted.getProjectId());

        // update in db
        repository.update(engagement);

        return engagement;

    }

    // Status comes from gitlab so it does not need to to be sync'd
    public Engagement updateStatus(String customerName, String projectName) {

        Status status = gitApi.getStatus(customerName, projectName);

        Optional<Engagement> optional = get(customerName, projectName);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("no engagement found.  use POST to create resource.");
        }

        Engagement persisted = optional.get();
        persisted.setStatus(status);

        // update in db
        repository.update(persisted);

        sendEngagementEvent(Arrays.asList(persisted));

        return persisted;
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

        if (LOGGER.isDebugEnabled()) {
            repository.listAll().stream().forEach(
                    engagement -> LOGGER.debug("E {} {}", engagement.getCustomerName(), engagement.getProjectName()));
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
     * Used by the {@link GitSyncService} to delete all {@link Engagement} from the
     * data store before re-populating from Git.
     */
    void deleteAll() {
        long count = repository.deleteAll();
        LOGGER.info("removed '" + count + "' engagements from the data store.");
    }

    /**
     * Persists the {@link List} of {@link Engagement} into the data store.
     * 
     * @param engagementList
     */
    void insertEngagementListInRepository(List<Engagement> engagementList) {
        repository.persist(engagementList);
    }

    /**
     * Updates the {@link List} of {@link Engagement} in the data store.
     * 
     * @param engagementList
     */
    void updateEngagementListInRepository(List<Engagement> engagementList) {
        repository.update(engagementList);
    }

    /**
     * Removes all {@link Engagement} from the data store and inserts the given
     * {@link List} of {@link Engagement}.
     * 
     * @param engagementList
     */
    void refreshFromEngagementList(List<Engagement> engagementList) {

        // remove all from database
        deleteAll();

        // insert
        insertEngagementListInRepository(engagementList);

        // send event to socket with modified engagements
        sendEngagementEvent(engagementList);

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
        engagement.setLaunch(Launch.builder().launchedDateTime(LocalDateTime.now())
                .launchedBy(engagement.getLastUpdateByName()).build());

        // update db
        Engagement updated = update(engagement.getCustomerName(), engagement.getProjectName(), engagement);

        // sync change(s) to git
        sendEngagementsModifiedEvent();

        return updated;

    }

    /**
     * Consumes a {@link BackendEvent}. If the force flag on the event is true or
     * there are no {@link Engagement} currently in the database, the {@link List}
     * of {@link Engagement}s in the event will be inserted into the database.
     * Please note that the database will be purged before the insert happens.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.DB_REFRESH_ADDRESS)
    void consumeDbRefreshRequestedEvent(BackendEvent event) {

        if (!event.isForceUpdate() && getAll().size() > 0) {
            LOGGER.debug(
                    "engagements already exist in db and force is not set.  doing nothing for db refresh request.");
            return;
        }

        LOGGER.debug("purging existing engagements from db and inserting from event list {}",
                event.getEngagementList());
        // refresh the db
        refreshFromEngagementList(event.getEngagementList());

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

        LOGGER.debug("consuming process time elapsed event.");

        sendEngagementsModifiedEvent();
    }

    /**
     * If any {@link Engagement}s in the database have been modified, it creates a
     * {@link BackendEvent} and places it on the {@link EventBus} for processing.
     */
    void sendEngagementsModifiedEvent() {

        List<Engagement> modifiedList = getModifiedEngagements();

        if (modifiedList.size() == 0) {
            LOGGER.debug("no modified engagements to process");
            return;
        }

        LOGGER.debug("emitting db engagements modified event");
        BackendEvent event = BackendEvent.createUpdateEngagementsInGitRequestedEvent(modifiedList);
        eventBus.sendAndForget(event.getEventType().getEventBusAddress(), event);

    }

    /**
     * Publishes the message to the {@link EventBus} so that all registered
     * consumers receive the event.
     * 
     * @param message
     */
    void sendEngagementEvent(List<Engagement> engagementList) {

        LOGGER.debug("emitting engagements updated event");
        eventBus.publish(ENGAGEMENTS_UPDATED_ADDRESS, jsonb.toJson(engagementList));

    }

    /**
     * Consumes the {@link BackendEvent}, converts the {@link List} of modified
     * {@link Engagement} to a JSON {@link String}, and sends it to be broadcast to
     * all the web socket sessions.
     * 
     * @param event
     */
    @ConsumeEvent(value = ENGAGEMENTS_UPDATED_ADDRESS, local = false)
    void consumeEngagementsUpdatedEvent(String message) {

        LOGGER.debug("consumed engagements updated event, {}", message);
        // send to socket
        socket.broadcast(message);

    }

}