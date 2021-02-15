package com.redhat.labs.lodestar.model.event;

public class EventType {

    private EventType() {
        throw new IllegalStateException("Utility class");
    }
    
    public static final String CREATE_ENGAGEMENT_EVENT_ADDRESS = "create.engagement.event";
    public static final String UPDATE_ENGAGEMENT_EVENT_ADDRESS = "update.engagement.event";
    public static final String DELETE_ENGAGEMENT_EVENT_ADDRESS = "delete.engagement.event";
    public static final String REFRESH_DATABASE_EVENT_ADDRESS = "refresh.database.event";
    public static final String SET_UUID_EVENT_ADDRESS = "set.uuid.event";
    public static final String RETRY_CREATE_EVENT_ADDRESS = "retry.create.event";
    public static final String RETRY_UPDATE_EVENT_ADDRESS = "retry.update.event";
    public static final String RETRY_DELETE_EVENT_ADDRESS = "retry.delete.event";

} 