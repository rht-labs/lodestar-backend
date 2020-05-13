package com.redhat.labs.omp.model;

import java.util.List;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.redhat.labs.omp.validation.ValidName;

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
    @JsonbProperty("mongo_id")
    private ObjectId mongoId;
    @ValidName
    @JsonbProperty("customer_name")
    private String customerName;
    @ValidName
    @JsonbProperty("project_name")
    private String projectName;
    @JsonbProperty("project_id")
    private Integer projectId;
    private String description;
    private String location;
    @JsonbProperty("start_date")
    private String startDate;
    @JsonbProperty("end_date")
    private String endDate;
    @JsonbProperty("archive_date")
    private String archiveDate;
    @JsonbProperty("engagement_lead_name")
    private String engagementLeadName;
    @JsonbProperty("engagement_lead_email")
    private String engagementLeadEmail;
    @JsonbProperty("technical_lead_name")
    private String technicalLeadName;
    @JsonbProperty("technical_lead_email")
    private String technicalLeadEmail;
    @JsonbProperty("customer_contact_name")
    private String customerContactName;
    @JsonbProperty("customer_contact_email")
    private String customerContactEmail;
    @JsonbProperty("ocp_cloud_provider_name")
    private String ocpCloudProviderName;
    @JsonbProperty("ocp_cloud_provider_region")
    private String ocpCloudProviderRegion;
    @JsonbProperty("ocp_version")
    private String ocpVersion;
    @JsonbProperty("ocp_sub_domain")
    private String ocpSubDomain;
    @JsonbProperty("ocp_persistent_storage_size")
    private String ocpPersistentStorageSize;
    @JsonbProperty("ocp_cluster_size")
    private String ocpClusterSize;
    private Launch launch;
    @JsonbProperty("engagement_users")
    private List<EngagementUser> engagementUsers;

    @JsonbTransient
    private FileAction action;
    @JsonbTransient
    private String lastUpdateByName;
    @JsonbTransient
    private String lastUpdateByEmail;

}
