package com.redhat.labs.lodestar.model;

import java.time.LocalDateTime;
import java.util.UUID;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ActiveSync extends PanacheMongoEntity {

    private UUID uuid;
    private LocalDateTime lastUpdated;

}
