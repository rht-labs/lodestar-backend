package com.redhat.labs.omp.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.redhat.labs.omp.model.git.api.GitApiFile;
import com.redhat.labs.omp.service.ConfigService;

@RequestScoped
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	ConfigService configService;

	@GET
	@SecurityRequirement(name = "jwt", scopes = {})
    @APIResponses(value = {
            @APIResponse(responseCode = "401", description = "Missing or Invalid JWT"),
            @APIResponse(responseCode = "200", description = "Configuration file data has been returned.")})
    @Operation(summary = "Returns configuration file data from git.")
	public GitApiFile fetchConfigData() {
		return configService.getConfigData();
	}

}
