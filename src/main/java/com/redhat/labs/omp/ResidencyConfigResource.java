package com.redhat.labs.omp;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/residency/config")
@RequestScoped
public class ResidencyConfigResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Response getResidency() {
		// TODO implement get
		return Response.ok("{ \"config\": \"dyi\"}").build();
	}
}
