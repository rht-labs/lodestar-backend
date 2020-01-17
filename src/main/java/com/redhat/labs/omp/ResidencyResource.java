package com.redhat.labs.omp;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/residency")
@RequestScoped
public class ResidencyResource {

	@POST
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response saveResidency() {
        return Response.ok().build(); 
    }
}
