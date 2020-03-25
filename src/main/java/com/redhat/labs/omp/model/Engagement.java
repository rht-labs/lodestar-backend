package com.redhat.labs.omp.model;

import lombok.Data;

@Data
public class Engagement {

    private int id;
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
    private String openShiftCloudProviderName;
    private String openShiftCloudProviderRegion;
    private String openShiftVersion;
    private String openShiftSubDomain;
    private String openShiftPersistentStorageSize;
    private String openShiftClusterSize;

}
