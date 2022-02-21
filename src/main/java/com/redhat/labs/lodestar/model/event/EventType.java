package com.redhat.labs.lodestar.model.event;

public class EventType {

    private EventType() {
        throw new IllegalStateException("Utility class");
    }

    public static final String CREATE_ENGAGEMENT_EVENT_ADDRESS = "create.engagement.event";
    public static final String UPDATE_ENGAGEMENT_EVENT_ADDRESS = "update.engagement.event";
    public static final String DELETE_ENGAGEMENT_EVENT_ADDRESS = "delete.engagement.event";

    public static final String SET_UUID_EVENT_ADDRESS = "set.uuid.event";
    public static final String RETRY_CREATE_EVENT_ADDRESS = "retry.create.event";
    public static final String RETRY_UPDATE_EVENT_ADDRESS = "retry.update.event";
    public static final String RETRY_DELETE_EVENT_ADDRESS = "retry.delete.event";

    public static final String LOAD_DATABASE_EVENT_ADDRESS = "load.database.event";
    public static final String DELETE_AND_RELOAD_DATABASE_EVENT_ADDRESS = "delete.and.reload.database.event";
    public static final String DELETE_AND_RELOAD_ENGAGEMENT_EVENT_ADDRESS = "delete.and.reload.engagement.event";
    public static final String DELETE_ENGAGEMENT_FROM_DATABASE_EVENT_ADDRESS = "delete.engagement.from.database.event";
    public static final String GET_PAGE_OF_ENGAGEMENTS_EVENT_ADDRESS = "get.page.of.engagements.event";
    public static final String PERSIST_ENGAGEMENT_LIST_EVENT_ADDRESS = "persist.engagement.list.event";
    public static final String PERSIST_ENGAGEMENT_EVENT_ADDRESS = "persist.engagement.event";
    public static final String UPDATE_COMMITS_EVENT_ADDRESS = "update.commits.event";
    public static final String UPDATE_STATUS_EVENT_ADDRESS = "update.status.event";
    public static final String UPDATE_PARTICIPANTS_EVENT_ADDESS = "update.participants.event";
    public static final String UPDATE_ARTIFACTS_EVENT_ADDRESS = "update.artifacts.event";
    
    public static final String RELOAD_ACTIVITY_EVENT_ADDRESS = "reload.activity.event";
    public static final String RELOAD_ARTIFACTS_EVENT_ADDRESS = "reload.artifacts.event";
    public static final String RELOAD_PARTICIPANTS_EVENT_ADDRESS = "reload.participants.event";
    public static final String RELOAD_ENGAGEMENT_STATUS_EVENT_ADDRESS = "reload.engagement.status.event";
    public static final String RELOAD_HOSTING_EVENT_ADDRESS = "reload.hosting.event";
   
}