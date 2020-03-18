package com.redhat.labs.omp.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigResourceTest {
	private static HotRodServer hs;
	
    @BeforeAll
    public static void init() {
        TestResourceTracker.setThreadTestName("InfinispanServer");

        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                new ConfigurationBuilder());
        ecm.createCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, new ConfigurationBuilder().indexing().build());

        ecm.getCache().put("a", "domedata");

        hs =  HotRodTestingUtil.startHotRodServer(ecm, 11222);
        hs.setMarshaller(new org.infinispan.commons.marshall.JavaSerializationMarshaller());

    }

	@Test
	public void getFileFromRepo() {
		given()
		.when()
		.get("/config")
		.then().statusCode(200).body("emoji", is("\uD83E\uDD8A"));
	}
}
