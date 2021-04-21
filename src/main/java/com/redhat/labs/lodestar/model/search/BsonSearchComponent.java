package com.redhat.labs.lodestar.model.search;

import java.util.Optional;

import org.bson.conversions.Bson;

public interface BsonSearchComponent {

    public Optional<Bson> getBson();

}
