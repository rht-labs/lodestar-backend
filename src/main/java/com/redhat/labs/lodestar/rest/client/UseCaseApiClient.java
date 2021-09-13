package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.UseCase;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.engagements.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Path("/api/v2/usecases")
public interface UseCaseApiClient {

    @GET
    Response getUseCases(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize);//TODO sort????

    @GET
    @Path("{uuid}")
    public UseCase getUseCase(@PathParam("uuid") String uuid);
}
