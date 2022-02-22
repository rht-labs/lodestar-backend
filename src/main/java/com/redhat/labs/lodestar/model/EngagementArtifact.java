package com.redhat.labs.lodestar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * An extended Artifact that contains the engagement (project) name and customer name. Useful
 * for requesting data outside of the engagement 'form'
 * @author mcanoy
 *
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class EngagementArtifact extends Artifact {
    
    private String projectName;
    private String customerName;
    private String region;

}
