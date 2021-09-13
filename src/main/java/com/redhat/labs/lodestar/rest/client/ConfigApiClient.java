package com.redhat.labs.lodestar.rest.client;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.config.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Path("/api/v1/configs/runtime")
public interface ConfigApiClient {

    @GET
    @Produces("application/json")
    Response getRuntimeConfig(@QueryParam("type") String type);

    @GET
    @Path("/rbac")
    @Produces("application/json")
    Map<String, List<String>> getPermission();

}
