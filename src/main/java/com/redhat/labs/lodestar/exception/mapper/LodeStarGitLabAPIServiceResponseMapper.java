package com.redhat.labs.lodestar.exception.mapper;

import java.io.ByteArrayInputStream;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Priority(4000)
public class LodeStarGitLabAPIServiceResponseMapper implements 
             ResponseExceptionMapper<RuntimeException> {
  @Override
  public RuntimeException toThrowable(Response response) {
    int status = response.getStatus();

    String msg = getBody(response);

    return new WebApplicationException(msg, status);

  }

  private String getBody(Response response) {
      ByteArrayInputStream is = (ByteArrayInputStream) response.getEntity();
      byte[] bytes = new byte[is.available()];
      is.read(bytes,0,is.available());
      String body = new String(bytes);
      return body;
    }

}