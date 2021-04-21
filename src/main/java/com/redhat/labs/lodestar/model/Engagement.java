package com.redhat.labs.lodestar.model;

import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.labs.lodestar.validation.ValidName;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Engagement extends PanacheMongoEntityBase {

    private String uuid;
    // Mongo DB generated ID
    @BsonId
    @JsonbTransient
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
    @JsonbProperty("hosting_environments")
    private List<HostingEnvironment> hostingEnvironments;
    @JsonbProperty("public_reference")
    private Boolean publicReference;
    @JsonProperty("additional_details")
    private String additionalDetails;
    private Launch launch;
    @JsonbProperty("engagement_users")
    private Set<EngagementUser> engagementUsers;

    private Status status;
    private List<Commit> commits;
    @JsonbProperty("creation_details")
    private CreationDetails creationDetails;
    @JsonbProperty("last_update_by_name")
    private String lastUpdateByName;
    @JsonbProperty("last_update_by_email")
    private String lastUpdateByEmail;
    @JsonbProperty("last_update")
    private String lastUpdate;

    @JsonbProperty("engagement_region")
    private String region;
    @JsonbProperty("engagement_type")
    private String type;

    @JsonbProperty("engagement_categories")
    private List<Category> categories;
    @JsonbProperty("use_cases")
    private List<UseCase> useCases;
    @JsonbProperty("timezone")
    private String timezone;

    private List<Artifact> artifacts;

    @JsonbProperty("commit_message")
    private String commitMessage;

    private List<Score> scores;

}