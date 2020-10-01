package com.redhat.labs.lodestar.model.event;

import java.util.List;

import com.redhat.labs.lodestar.model.Category;
import com.redhat.labs.lodestar.model.Engagement;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackendEvent {

    private EventType eventType;
    private List<Engagement> engagementList;
    private List<Category> categoryList;

    public static BackendEvent createDatabaseRefreshRequestedEvent() {
        return BackendEvent.builder().eventType(EventType.DB_REFRESH_REQUESTED).build();
    }

    public static BackendEvent createPushToGitRequestedEvent() {
        return BackendEvent.builder().eventType(EventType.PUSH_TO_GIT_REQUESTED).build();
    }

    public static BackendEvent createUpdateEngagementsInDbRequestedEvent(List<Engagement> engagementList) {
        return BackendEvent.builder().eventType(EventType.UPDATE_ENGAGEMENTS_IN_DB_REQUESTED)
                .engagementList(engagementList).build();
    }

    public static BackendEvent createUpdateEngagementsInGitRequestedEvent(List<Engagement> engagementList) {
        return BackendEvent.builder().eventType(EventType.UPDATE_ENGAGEMENTS_IN_GIT_REQUESTED)
                .engagementList(engagementList).build();
    }

    public static BackendEvent createSetNullUuidRequestedEvent() {
        return BackendEvent.builder().eventType(EventType.SET_NULL_UUID_REQUESTED).build();
    }

}