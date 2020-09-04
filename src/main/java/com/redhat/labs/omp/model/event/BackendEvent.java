package com.redhat.labs.omp.model.event;

import java.util.List;

import com.redhat.labs.omp.model.Category;
import com.redhat.labs.omp.model.Engagement;

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

    public static BackendEvent createInsertCategoriesInDbRequestedEvent(List<Engagement> engagementList) {
        return BackendEvent.builder().eventType(EventType.INSERT_CATEGORIES_IN_DB_REQUESTED)
                .engagementList(engagementList).build();
    }

    public static BackendEvent createPurgeAndInsertCategoriesInDbRequestedEvent(List<Engagement> engagementList) {
        return BackendEvent.builder().eventType(EventType.PURGE_AND_INSERT_CATEGORIES_IN_DB_REQUESTED)
                .engagementList(engagementList).build();
    }

    public static BackendEvent createDecrementCategoryCountsEvent(List<Category> categoryList) {
        return BackendEvent.builder().eventType(EventType.DECREMENT_CATEGORY_COUNTS_REQUESTED)
                .categoryList(categoryList).build();
    }

}