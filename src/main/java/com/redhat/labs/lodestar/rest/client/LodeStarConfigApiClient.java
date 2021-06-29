package com.redhat.labs.lodestar.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.exception.mapper.LodeStarGitLabAPIServiceResponseMapper;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.config.api")
@RegisterProvider(value = LodeStarGitLabAPIServiceResponseMapper.class, priority = 50)
@RegisterClientHeaders(JWTRequestFactory.class)
public interface LodeStarConfigApiClient {

    @GET
    @Path("/api/v1/configs/runtime")
    @Produces("application/json")
    Response getRuntimeConfig(@QueryParam("type") String type);

}
