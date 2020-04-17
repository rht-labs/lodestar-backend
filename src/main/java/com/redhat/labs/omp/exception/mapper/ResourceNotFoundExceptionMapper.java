package com.redhat.labs.omp.exception.mapper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;

import com.redhat.labs.omp.exception.ResourceNotFoundException;

@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(ResourceNotFoundException exception) {

        int status = HttpStatus.SC_NOT_FOUND;
        JsonObject model = Json.createObjectBuilder().add("error", exception.getMessage()).add("code", status).build();

        return Response.status(status).entity(model.toString()).build();

    }

}
