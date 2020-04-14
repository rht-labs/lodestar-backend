package com.redhat.labs.omp.resource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

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
	public GitApiFile fetchConfigData() {
		return configService.getConfigData();
	}

}
