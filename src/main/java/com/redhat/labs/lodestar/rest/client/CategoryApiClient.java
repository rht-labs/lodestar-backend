package com.redhat.labs.lodestar.rest.client;

import com.redhat.labs.lodestar.exception.mapper.ServiceResponseMapper;
import com.redhat.labs.lodestar.model.Category;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.engagements.api")
@RegisterProvider(value = ServiceResponseMapper.class, priority = 50)
@Path("/api/v2/categories")
public interface CategoryApiClient {

    @GET
    List<Category> getCategories(@QueryParam("engagementUuid") String engagementUuidOption);//, @BeanParam PageFilter pageFilter

    @GET
    @Path("suggest")
    Set<String> getCategorySuggestions(@QueryParam("partial") String partial);

    @GET
    @Path("rollup")
    Response getCategoryRollup(@QueryParam("region") List<String> region, @QueryParam("page") int page,
                               @QueryParam("pageSize") int pageSize);

    @POST
    @Path("{engagementUuid}")
    Response updateCategories(@PathParam("engagementUuid") String engagementUuid, @QueryParam("authorName") String authorName,
                              @QueryParam("authorEmail") String authorEmail, Set<String> categories);
}
