package com.redhat.labs.omp.exception.mapper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;

import com.redhat.labs.omp.exception.ResourceAlreadyExistsException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {

        int status = HttpStatus.SC_INTERNAL_SERVER_ERROR;

        JsonObject model = Json.createObjectBuilder().add("error", exception.getMessage()).add("code", status).build();

        if (exception instanceof ResourceAlreadyExistsException) {
            status = HttpStatus.SC_CONFLICT;
        } else if (exception instanceof ResourceNotFoundException) {
            status = HttpStatus.SC_NOT_FOUND;
        }

        return Response.status(status).entity(model.toString()).build();

    }

}
