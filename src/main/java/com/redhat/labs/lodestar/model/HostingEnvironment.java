package com.redhat.labs.lodestar.model;

import javax.json.bind.annotation.JsonbProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HostingEnvironment {

    @JsonbProperty("id")
    private String id;

    @JsonbProperty("environment_name")
    private String environmentName;

    @JsonbProperty("additional_details")
    private String additionalDetails;

    @JsonbProperty("ocp_cloud_provider_name")
    private String ocpCloudProviderName;

    @JsonbProperty("ocp_cloud_provider_region")
    private String ocpCloudProviderRegion;

    @JsonbProperty("ocp_persistent_storage_size")
    private String ocpPersistentStorageSize;

    @JsonbProperty("ocp_sub_domain")
    private String ocpSubDomain;

    @JsonbProperty("ocp_version")
    private String ocpVersion;

    @JsonbProperty("ocp_cluster_size")
    private String ocpClusterSize;

}
