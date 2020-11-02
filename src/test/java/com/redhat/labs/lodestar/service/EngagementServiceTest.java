package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.GitlabProject;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.Launch;
import com.redhat.labs.lodestar.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
class EngagementServiceTest {

	
	@Inject
	EngagementService engagementService;
	
	@Test void testUpdateStatusInvalidProject() {
		Hook hook = Hook.builder().project(GitlabProject.builder().pathWithNamespace("/nope/nada/iac").nameWithNamespace("/ nope / nada / iac").build()).build();
		
		Exception ex = assertThrows(WebApplicationException.class, ()-> {
			engagementService.updateStatusAndCommits(hook);
		});
		
		assertEquals("no engagement found. unable to update from hook.", ex.getMessage());
	}
	
	@Test void testAlreadyLaunched() {
		Engagement engagement = Engagement.builder().launch(Launch.builder().build()).build();
		
		Exception ex = assertThrows(WebApplicationException.class, ()-> {
			engagementService.launch(engagement);
		});
		
		assertEquals("engagement has already been launched.", ex.getMessage());
	}
	
}
