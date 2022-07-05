package com.redhat.labs.lodestar.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffIgnore;

@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementAttribute {

    private String uuid;
    @DiffIgnore
    private String created;
    @DiffIgnore
    private String updated;
    @DiffIgnore
    private String engagementUuid;

    public void generateId() {
        if (null == uuid) {
            uuid = UUID.randomUUID().toString();
        }
    }

    public void setUpdated() {
        String dateTime = getNowAsString();
        updated = dateTime;
    }

    public void setCreatedAndUpdated() {
        String dateTime = getNowAsString();
        created = dateTime;
        updated = dateTime;
    }

    private String getNowAsString() {
        return LocalDateTime.now(ZoneId.of("Z")).toString();
    }

}