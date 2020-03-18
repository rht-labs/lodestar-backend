package com.redhat.labs.omp.resources;

import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class EngagementResourceTest {
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

    @AfterAll
    public static void teardown() {
        if (hs != null) {
            hs.stop();
        }
    }


}