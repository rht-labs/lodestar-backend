package com.redhat.labs.lodestar.model.search;

import java.util.Optional;

import org.bson.conversions.Bson;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class RangeSearchComponent implements BsonSearchComponent {

    private String start;
    private String end;

    public Optional<Bson> getBson() {
        return Optional.empty();
    }

}
