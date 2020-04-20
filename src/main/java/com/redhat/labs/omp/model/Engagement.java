package com.redhat.labs.omp.model;

import java.util.List;

import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
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
public class Engagement extends PanacheMongoEntityBase {

    // Mongo DB generated ID
    @BsonId
    private ObjectId mongoId;

    @NotBlank
    private String customerName;
    @NotBlank
    private String projectName;
    private Integer projectId;
    private String description;
    private String location;
    private String startDate;
    private String endDate;
    private String archiveDate;
    private String engagementLeadName;
    private String engagementLeadEmail;
    private String technicalLeadName;
    private String technicalLeadEmail;
    private String customerContactName;
    private String customerContactEmail;
    private String ocpCloudProviderName;
    private String ocpCloudProviderRegion;
    private String ocpVersion;
    private String ocpSubDomain;
    private String ocpPersistentStorageSize;
    private String ocpClusterSize;
    private Launch launch;
    private List<EngagementUser> engagementUsers;

    @JsonbTransient
    private FileAction action;
    @JsonbTransient
    private String lastUpdateByName;
    @JsonbTransient
    private String lastUpdateByEmail;

}
