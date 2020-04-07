package com.redhat.labs.omp.model;

import javax.validation.constraints.NotNull;

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
public class Engagement extends PanacheMongoEntity {

    private Integer enagementId;
    @NotNull
    private String customerName;
    @NotNull
    private String projectName;
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

}
