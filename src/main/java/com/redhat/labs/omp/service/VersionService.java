package com.redhat.labs.omp.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.redhat.labs.omp.config.VersionManifestConfig;
import com.redhat.labs.omp.model.Version;
import com.redhat.labs.omp.rest.client.OMPGitLabAPIService;

@ApplicationScoped
public class VersionService {

	@RestClient
	@Inject
	OMPGitLabAPIService gitApiService;
	
	@Inject
	VersionManifestConfig versionManifestConfig;
	
	public List<Version> getVersionManifest() {
		return versionManifestConfig.getVersionData();
	}
	
	public Version getGitApiVersion() {
		Version version = gitApiService.getVersion();
		version.setApplication("omp-git-api-container");
		
		if(version.getGitTag().startsWith("v")) {
			version.setVersion(version.getGitTag());
		} else {
			version.setVersion(version.getGitCommit());
		}
		
		return version;
	}
}
