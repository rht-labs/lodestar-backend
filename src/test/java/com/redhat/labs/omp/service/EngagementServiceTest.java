package com.redhat.labs.omp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.labs.omp.exception.InvalidRequestException;
import com.redhat.labs.omp.exception.ResourceNotFoundException;
import com.redhat.labs.omp.model.Engagement;
import com.redhat.labs.omp.model.GitlabProject;
import com.redhat.labs.omp.model.Hook;
import com.redhat.labs.omp.model.Launch;
import com.redhat.labs.omp.model.event.BackendEvent;
import com.redhat.labs.utils.EmbeddedMongoTest;

import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
class EngagementServiceTest {

	
	@Inject
	EngagementService engagementService;
	
	@Test void testUpdateStatusInvalidProject() {
		Hook hook = Hook.builder().project(GitlabProject.builder().pathWithNamespace("/nope/nada/iac").nameWithNamespace("/ nope / nada / iac").build()).build();
		
		Exception ex = assertThrows(ResourceNotFoundException.class, ()-> {
			engagementService.updateStatusAndCommits(hook);
		});
		
		assertEquals("no engagement found. unable to update from hook.", ex.getMessage());
	}
	
	@Test void testAlreadyLaunched() {
		Engagement engagement = Engagement.builder().launch(Launch.builder().build()).build();
		
		Exception ex = assertThrows(InvalidRequestException.class, ()-> {
			engagementService.launch(engagement);
		});
		
		assertEquals("engagement has already been launched.", ex.getMessage());
	}
	
	@Test void consumeDbRefreshRequestedEventEmpty() {
		BackendEvent be = BackendEvent.builder().forceUpdate(false).build();
		
		engagementService.consumeDbRefreshRequestedEvent(be);
		
		assertFalse(be.isForceUpdate());
	}
}
