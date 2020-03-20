package com.redhat.labs.omp.cache;

import io.quarkus.test.junit.QuarkusTest;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.test.HotRodTestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
class ResidencyDataCacheTest {
    private static HotRodServer hs;

    @Inject
    ResidencyDataCache residencyDataCache;

    @BeforeAll
    public static void init() {
        TestResourceTracker.setThreadTestName("InfinispanServer");

        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                new ConfigurationBuilder());
        ecm.createCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, new ConfigurationBuilder().indexing().build());

        ecm.createCache("omp", new ConfigurationBuilder().indexing().build());
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

    @Test
    public void testPut() {

        residencyDataCache.store("a", "domedata");

        assertEquals("domedata", residencyDataCache.fetch("a"));
    }

}