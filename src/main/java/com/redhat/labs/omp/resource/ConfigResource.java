package com.redhat.labs.omp.resource;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.labs.omp.service.ConfigService;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ConfigResource {

	@Inject
	JsonWebToken jwt;

	@Inject
	ConfigService configService;

	@GET
	@PermitAll
	public String fetchConfigData() {
		return configService.getConfigData();
	}

}
