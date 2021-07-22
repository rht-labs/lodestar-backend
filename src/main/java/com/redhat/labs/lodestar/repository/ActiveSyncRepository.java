package com.redhat.labs.lodestar.repository;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.lodestar.model.ActiveSync;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ActiveSyncRepository implements PanacheMongoRepository<ActiveSync> {

}
