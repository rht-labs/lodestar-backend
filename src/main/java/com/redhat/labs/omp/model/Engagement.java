package com.redhat.labs.omp.model;

import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;

import com.redhat.labs.omp.model.git.api.FileAction;
import com.redhat.labs.omp.model.git.api.GitApiEngagement;

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

    @NotBlank
    private String customerName;
    @NotBlank
    private String projectName;
    private Integer engagementId;
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
    @JsonbTransient
    private FileAction action;
    @JsonbTransient
    private String lastUpdateByName;
    @JsonbTransient
    private String lastUpdateByEmail;

    public static Engagement from(GitApiEngagement engagement) {

        return Engagement.builder().engagementId(engagement.getId()).customerName(engagement.getCustomerName())
                .projectName(engagement.getProjectName()).description(engagement.getDescription())
                .location(engagement.getLocation()).startDate(engagement.getStartDate())
                .endDate(engagement.getEndDate()).archiveDate(engagement.getArchiveDate())
                .engagementLeadName(engagement.getEngagementLeadName())
                .engagementLeadEmail(engagement.getEngagementLeadEmail())
                .technicalLeadName(engagement.getTechnicalLeadName())
                .technicalLeadEmail(engagement.getTechnicalLeadEmail())
                .customerContactName(engagement.getCustomerContactName())
                .customerContactEmail(engagement.getCustomerContactEmail())
                .ocpCloudProviderName(engagement.getOcpCloudProviderName())
                .ocpCloudProviderRegion(engagement.getOcpCloudProviderRegion()).ocpVersion(engagement.getOcpVersion())
                .ocpSubDomain(engagement.getOcpSubDomain())
                .ocpPersistentStorageSize(engagement.getOcpPersistentStorageSize())
                .ocpClusterSize(engagement.getOcpClusterSize()).build();

    }

}
