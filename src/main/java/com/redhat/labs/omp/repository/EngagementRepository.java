package com.redhat.labs.omp.repository;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.omp.model.Engagement;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class EngagementRepository implements PanacheMongoRepository<Engagement> {

    public Engagement findByEngagementId(Integer engagementId) {
        return find("engagementId", engagementId).firstResult();
    }

    public Engagement findByCustomerNameAndProjectName(String customerName, String projectName) {
        return find("customerName = ?1 and projectName = ?2", customerName, projectName).firstResult();
    }

}
