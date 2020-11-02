package com.redhat.labs.lodestar.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.event.BackendEvent;
import com.redhat.labs.lodestar.model.event.EventType;

import io.quarkus.vertx.ConsumeEvent;

@ApplicationScoped
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventService.class);

    @Inject
    EngagementService engagementService;

    /**
     * This method consumes a {@link BackendEvent}, which starts the process of
     * inserting any {@link Engagement} from GitLab that are not found in the
     * database.
     * 
     * @param event
     */
    @ConsumeEvent(value = EventType.Constants.DB_REFRESH_REQUESTED_ADDRESS, blocking = true)
    void consumeDbRefreshRequestedEvent(BackendEvent event) {

        LOGGER.debug("consumed database refresh requested event.");
        engagementService.syncGitToDatabase(false);

    }

    /**
     * Consumes a {@link BackendEvent} and updates the database using the
     * {@link List} of {@link Engagement}s that are contained in the event.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.UPDATE_ENGAGEMENTS_IN_DB_REQUESTED_ADDRESS)
    void consumeUpdateEngagementsInDbRequestedEvent(BackendEvent event) {
        engagementService.updateProcessedEngagementListInRepository(event.getEngagementList());
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
        engagementService.sendEngagementsModifiedEvent();

    }

    /**
     * Consumes the {@link BackendEvent} and triggers the processing of
     * {@link Engagement}s with a null UUID.
     * 
     * @param event
     */
    @ConsumeEvent(EventType.Constants.SET_NULL_UUID_REQUESTED_ADDRESS)
    void consumeSetNullUuidRequestedEvent(BackendEvent event) {
        LOGGER.debug("processing set uuid event.");
        engagementService.setNullUuids();
    }

}
