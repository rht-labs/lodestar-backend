package com.redhat.labs.lodestar.model.event;

public enum EventType {

    DB_REFRESH_REQUESTED(Constants.DB_REFRESH_REQUESTED_ADDRESS), 
    PUSH_TO_GIT_REQUESTED(Constants.PUSH_TO_GIT_REQUESTED_ADDRESS),
    UPDATE_ENGAGEMENTS_IN_DB_REQUESTED(Constants.UPDATE_ENGAGEMENTS_IN_DB_REQUESTED_ADDRESS),
    UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED(Constants.UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED_ADDRESS),
    SET_NULL_UUID_REQUESTED(Constants.SET_NULL_UUID_REQUESTED_ADDRESS),
    DELETE_ENGAGEMENT_IN_GIT_REQUESTED(Constants.DELETE_ENGAGEMENT_IN_GIT_REQUESTED_ADDRESS);

    private String eventBusAddress;

    EventType(String eventBusAddress) {
        this.eventBusAddress = eventBusAddress;
    }

    public String getEventBusAddress() {
        return this.eventBusAddress;
    }

    public class Constants {

        public static final String DB_REFRESH_REQUESTED_ADDRESS = "db.refresh.requested.event";
        public static final String PUSH_TO_GIT_REQUESTED_ADDRESS = "push.to.git.requested.event";
        public static final String UPDATE_ENGAGEMENTS_IN_DB_REQUESTED_ADDRESS = "update.engagements.in.db.requested.event";
        public static final String UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED_ADDRESS = "update.engagements.in.git.requested.event";
        public static final String SET_NULL_UUID_REQUESTED_ADDRESS = "set.null.uuid.requested.event";
        public static final String DELETE_ENGAGEMENT_IN_GIT_REQUESTED_ADDRESS = "delete.engagement.in.git.requested";

    }

} 