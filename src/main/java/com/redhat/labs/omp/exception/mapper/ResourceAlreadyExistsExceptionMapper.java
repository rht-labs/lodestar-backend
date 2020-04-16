package com.redhat.labs.omp.exception.mapper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;

import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;

@Provider
public class ResourceAlreadyExistsExceptionMapper implements ExceptionMapper<ResourceAlreadyExistsException> {

    @Override
    public Response toResponse(ResourceAlreadyExistsException exception) {

        int status = HttpStatus.SC_CONFLICT;
        JsonObject model = Json.createObjectBuilder().add("error", exception.getMessage()).add("code", status).build();

        return Response.status(status).entity(model.toString()).build();

    }

}
