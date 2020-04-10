package com.redhat.labs.omp.repository;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.omp.model.ActiveSync;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ActiveSyncRepository implements PanacheMongoRepository<ActiveSync> {

}
