package com.redhat.labs.omp.model;

public class Engagement {

    public int id;
    public String customerName;
    public String projectName;
    public String description;
    public String location;
    public String startDate;
    public String endDate;
    public String archiveDate;
    public String engagementLeadName;
    public String engagementLeadEmail;
    public String technicalLeadName;
    public String technicalLeadEmail;
    public String customerContactName;
    public String customerContactEmail;
    public String openShiftCloudProviderName;
    public String openShiftCloudProviderRegion;
    public String openShiftVersion;
    public String openShiftSubDomain;
    public String openShiftPersistentStorageSize;
    public String openShiftClusterSize;

    public Engagement () {}

    public String toString() {
        String engagement = "Engagement (%d) Customer: %s Project: %s Description: %s Location: %s Start Date: %s"
                + " End Date: %s Archive Date: %s + Engagement Lead %s (%s)";

        return String.format(engagement, id, customerName, projectName, description, location, startDate,
                endDate, archiveDate, engagementLeadName, engagementLeadEmail);
    }
}
