package com.redhat.labs.lodestar.rest.client;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWTRequestFactory  implements ClientHeadersFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(JWTRequestFactory.class);
    

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        
        if(LOGGER.isTraceEnabled()) {
            incomingHeaders.entrySet().forEach(e -> {
                LOGGER.trace(String.format("Header %s Value %s", e.getKey(), e.getValue()));
            });
        }

        MultivaluedMap<String, String> result = new MultivaluedMapImpl<String, String>();
        result.add("Authorization", incomingHeaders.getFirst("Authorization"));
        
        return result;
    }

}
