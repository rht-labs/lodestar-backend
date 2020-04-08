package com.redhat.labs.omp.model.git.api;

import com.redhat.labs.omp.model.Engagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitApiEngagement {

    private Integer id;
    private String customerName;
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

    public static GitApiEngagement from(Engagement engagement) {
        return GitApiEngagement.builder()
                .id(engagement.getEngagementId())
                .customerName(engagement.getCustomerName())
                .projectName(engagement.getProjectName())
                .description(engagement.getDescription())
                .location(engagement.getLocation())
                .startDate(engagement.getStartDate())
                .endDate(engagement.getEndDate())
                .archiveDate(engagement.getArchiveDate())
                .engagementLeadName(engagement.getEngagementLeadName())
                .engagementLeadEmail(engagement.getEngagementLeadEmail())
                .technicalLeadName(engagement.getTechnicalLeadName())
                .technicalLeadEmail(engagement.getTechnicalLeadEmail())
                .customerContactName(engagement.getCustomerContactName())
                .customerContactEmail(engagement.getCustomerContactEmail())
                .ocpCloudProviderName(engagement.getOcpCloudProviderName())
                .ocpCloudProviderRegion(engagement.getOcpCloudProviderRegion())
                .ocpVersion(engagement.getOcpVersion())
                .ocpSubDomain(engagement.getOcpSubDomain())
                .ocpPersistentStorageSize(engagement.getOcpPersistentStorageSize())
                .ocpClusterSize(engagement.getOcpClusterSize())
                .build();
    }

}
