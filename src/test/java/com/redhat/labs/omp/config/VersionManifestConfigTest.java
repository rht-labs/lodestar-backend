package com.redhat.labs.omp.config;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.labs.omp.model.VersionManifest;
import com.redhat.labs.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
public class VersionManifestConfigTest {

	@Inject
	VersionManifestConfig vmc;
	
	@Test
	public void testYaml() throws JsonProcessingException {
		
		VersionManifest vm = vmc.getVersionData();

		Assertions.assertEquals(1, vm.getContainers().size());
		Assertions.assertEquals(2, vm.getApplications().size());
		Assertions.assertEquals("omp-backend-container", vm.getContainers().get(0).getApplication());
		Assertions.assertEquals("master-abcdef", vm.getContainers().get(0).getVersion());
		Assertions.assertEquals("ball", vm.getApplications().get(0).getApplication());
		Assertions.assertEquals("v2.0", vm.getApplications().get(0).getVersion());
	}
}
