package com.redhat.labs.omp.exception.mapper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.http.HttpStatus;

import com.redhat.labs.omp.exception.InvalidRequestException;

public class InvalidRequestExceptionMapper implements ExceptionMapper<InvalidRequestException> {

    @Override
    public Response toResponse(InvalidRequestException exception) {

        int status = HttpStatus.SC_BAD_REQUEST;
        JsonObject model = Json.createObjectBuilder().add("error", exception.getMessage()).add("code", status).build();

        return Response.status(status).entity(model.toString()).build();

    }
}
