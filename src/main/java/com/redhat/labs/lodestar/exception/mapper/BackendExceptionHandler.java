package com.redhat.labs.lodestar.exception.mapper;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BackendExceptionHandler implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {

        Map<String, String> map = new HashMap<>();
        map.put("message", exception.getMessage());

        return Response.status(exception.getResponse().getStatus()).entity(map).build();

    }

}
