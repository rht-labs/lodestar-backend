package com.redhat.labs.omp.repository;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.FileAction;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    public Engagement findByEngagementId(Integer engagementId) {
        return find("engagementId", engagementId).firstResult();
    }

    public Engagement findByCustomerNameAndProjectName(String customerName, String projectName) {
        return find("customerName=?1 and projectName=?2", customerName, projectName).firstResult();
    }

    public List<Engagement> findByModifiedAndAction(FileAction action) {
        return find("action", action).list();
    }

    public List<Engagement> findByModified() {
        return find("action is not null").list();
    }

}
