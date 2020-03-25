package com.redhat.labs.omp.resources;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class EngagementResourceTest {
//    private static HotRodServer hs;

//    @BeforeAll
//    public static void init() {
//        TestResourceTracker.setThreadTestName("InfinispanServer");
//
//        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
//                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
//                new ConfigurationBuilder());
//        ecm.createCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, new ConfigurationBuilder().indexing().build());
//
//        ecm.getCache().put("a", "domedata");
//
//        hs =  HotRodTestingUtil.startHotRodServer(ecm, 11222);
//        hs.setMarshaller(new org.infinispan.commons.marshall.JavaSerializationMarshaller());
//
//    }
//
//    @AfterAll
//    public static void teardown() {
//        if (hs != null) {
//            hs.stop();
//        }
//    }
//
   
    @Test
    public void testThis() {
    	System.out.println("done.");
    }


}