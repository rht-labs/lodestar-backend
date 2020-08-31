package com.redhat.labs.omp.model;

import javax.json.bind.annotation.JsonbTransient;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Category extends PanacheMongoEntity {

    private String name;

    @JsonbTransient
    public ObjectId getId() {
        return this.id;
    }

}
