package com.redhat.labs.omp.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigMapTestResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMapTestResource.class);
	private String PATH = "/cm-test/cm-test.json";
	
	@GET
	public Response getCM() {
		
		java.nio.file.Path path = Paths.get(PATH);
        
        if(Files.isReadable(path)) {
            try {
                String fileContents = new String(Files.readAllBytes(path));
                
                LOGGER.warn(fileContents);
                return Response.ok(fileContents).build();
            } catch (IOException e) {
                LOGGER.error("Found but unable to read file {}", path);
            }
        } else {
            LOGGER.warn("Unable to locate version manifest file at {}", path);
        }
        return Response.status(HttpStatus.SC_NOT_FOUND).build();
	}

}
