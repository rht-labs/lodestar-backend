package com.redhat.labs.lodestar.resource;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Tag("integration")
class EngagementResourceTest {

	@Nested
	class ConfigResource extends ConfigResourceTest {
	}

	@Nested
	class CustomerSuggestion extends CustomerSuggestionTest {
	}

	@Nested
	class EngagementResourceCreate extends EngagementResourceCreateTest {
	}

	@Nested
	class EngagementResourceDelete extends EngagementResourceDeleteTest {
	}

	@Nested
	class EngagementResourceGet extends EngagementResourceGetTest {
	}

	@Nested
	class EngagementResourceV2Get extends EngagementResourceV2GetTest {
	}
	
	@Nested
	class EngagementResourceHead extends EngagementResourceHeadTest {
	}

	@Nested
	class EngagementResourceJwt extends EngagementResourceJwtTest {
	}

	@Nested
	class EngagementResourceUpdate extends EngagementResourceUpdateTest {
	}

	@Nested
	class StatusResource extends StatusResourceTest {
	}

	@Nested
	class VersionResource extends VersionResourceTest {
	}
	
	@Nested
	class ActivityResource extends ActivityResourceTest {
	}

}
