package com.redhat.labs.lodestar.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.rest.client.ConfigApiClient;

@ApplicationScoped
public class ConfigService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

	@Inject
	@RestClient 
	ConfigApiClient configApiClient;
	
	//TODO cache this
	public List<String> getPermission(String engagementType) {
		LOGGER.debug("type []", engagementType);
		
		Map<String, List<String>> allPermissions = configApiClient.getPermission();
		
		if(allPermissions.containsKey(engagementType)) {
			return allPermissions.get(engagementType);
		}
		
		return Collections.emptyList();
	}
	
	public void inValidateCache() {
		//TODO
	}
	
	public Response getRuntimeConfig(Optional<String> type) {
		
		return configApiClient.getRuntimeConfig(type.isPresent() ? type.get() : null);
	}
}
