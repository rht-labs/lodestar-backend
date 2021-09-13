package com.redhat.labs.lodestar.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.labs.lodestar.util.DateFormatter;
import com.redhat.labs.lodestar.validation.ValidName;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.DiffIgnore;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Engagement extends PanacheMongoEntityBase {

    private String uuid;
    // Mongo DB generated ID
    @BsonId
    @JsonbTransient @DiffIgnore
    private ObjectId mongoId;
    @ValidName
    @JsonbProperty("customer_name")
    private String customerName;
    @ValidName
    private String name;
    @JsonbProperty("project_id") //Should this be sent to the FE?
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
    @DiffIgnore
    private List<HostingEnvironment> hostingEnvironments;
    @JsonbProperty("public_reference")
    private Boolean publicReference;
    @JsonProperty("additional_details")
    private String additionalDetails;
    private Launch launch;
    @JsonbProperty("engagement_users")
    @DiffIgnore
    private Set<EngagementUser> engagementUsers;

    @DiffIgnore
    private Status status;
    //can the user viewing this engagement write to it
    //only set when viewing an individual engagement and not a list
    //also checked before PUT/POST actions
    @DiffIgnore
    private Boolean writeable;
    
    @Deprecated
    /**
     * Deprecated - delivered in a separate api separately
     */
    @DiffIgnore
    private List<Commit> commits;
    @JsonbProperty("creation_details")
    @DiffIgnore
    private CreationDetails creationDetails;
    @JsonbProperty("last_update_by_name")
    @DiffIgnore
    private String lastUpdateByName;
    @JsonbProperty("last_update_by_email")
    @DiffIgnore
    private String lastUpdateByEmail;
    @DiffIgnore
    @JsonbProperty("last_update")
    private String lastUpdate;

    private String region;
    private String type;

    @JsonbProperty("engagement_categories")
    @DiffIgnore
    private List<Category> categories;
    @JsonbProperty("use_cases")
    private List<UseCase> useCases;
    @JsonbProperty("timezone")
    private String timezone;

    @DiffIgnore
    private List<Artifact> artifacts;

    @JsonbProperty("commit_message")
    @DiffIgnore
    private String commitMessage;

    private List<Score> scores;

    @JsonbProperty("billing_codes")
    private List<BillingCodes> billingCodes;

    @JsonbProperty("participant_count")
    @DiffIgnore
    private int participantCount;

    //Legacy - front end should switch to region
    @JsonbProperty("engagement_region")
    private String engagementRegion;

    //Legacy
    @JsonbProperty("project_name")
    private String projectName;

    //Legacy
    @JsonbProperty("engagement_type")
    private String engagementType;

    public void setEngagementRegion(String engagementRegion) {
        this.engagementRegion = engagementRegion;
        this.region = engagementRegion;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        this.name = projectName;
    }

    public void setName(String projectName) {
        this.projectName = projectName;
        this.name = projectName;
    }

    public void setEngagementType(String engagementType) {
        this.engagementType = engagementType;
        this.type = engagementType;
    }

    /**
     * The value return here is relative to the time entered. If the time entered was
     * (currentDate) Jan 1 2020  and this engagement started on Feb 1 and ended Feb 28
     * the returned value would be Active if it was launched.
     * 
     * Do not confuse this with the status field. This will tell you if the
     * engagement is started or over (or another state) The status field is more
     * detailed information like whether a cluster is up or down etc... Current
     * states can be UPCOMING, ACTIVE, PAST, UNKNOWN
     * 
     * @param currentDate - A time to compare against. Should be local to the user
     * @return
     */
    public EngagementState getEngagementCurrentState(LocalDateTime currentDate) {

        if (launch == null || endDate == null || startDate == null) { // not launched or irregularly launched
            return EngagementState.UPCOMING;
        }
        
        LocalDateTime endDateLocal = DateFormatter.getInstance().getDateTime(endDate);
        
        if(endDateLocal.isBefore(currentDate)) { //has reached end date
            if(archiveDate != null && DateFormatter.getInstance().getDateTime(archiveDate).isAfter(currentDate)) { //hasn't reached archive date
                return EngagementState.TERMINATING;
            }
            return EngagementState.PAST;
        }

        //has not reached end date
        return EngagementState.ACTIVE;
    }

    public enum EngagementState {
        // The state ANY can be in any of the other states
        UPCOMING, PAST, TERMINATING, ACTIVE, ANY;
    }

}
