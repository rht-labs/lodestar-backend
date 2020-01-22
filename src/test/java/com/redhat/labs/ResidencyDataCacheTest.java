package com.redhat.labs;

import com.redhat.labs.mocks.MockResidencyDataCache;
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

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;


@QuarkusTest
class ResidencyDataCacheTest {
    /*private static HotRodServer hs;

//    @RegisterExtension
//    static InfinispanServerExtension server = new InfinispanServerExtension();

    @BeforeAll
    public static void init() {
//        Map<String, String> a = server.start();
//
//        RemoteCacheManager rcm = server.hotRodClient();
////        rcm.start();
//
//
////        System.out.println("******************\n\n\n" + rcm.getCacheNames() + "\n\n\n\n************");
//        rcm.administration().createCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, "org.infinispan.DIST_SYNC");
////        rcm.administration().createCache("myCache", (BasicConfiguration) null);

        TestResourceTracker.setThreadTestName("InfinispanServer");
//        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
//
//
//        builder.marshaller(JavaSerializationMarshaller.class)
//                .addJavaSerialWhiteList("org.infinispan.example.*", "org.infinispan.concrete.SomeClass");


        EmbeddedCacheManager ecm = TestCacheManagerFactory.createCacheManager(
                new GlobalConfigurationBuilder().nonClusteredDefault().defaultCacheName("default"),
                new ConfigurationBuilder());
        ecm.createCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME, new ConfigurationBuilder().indexing().build());
//        ecm.createCache("myCache", new ConfigurationBuilder().indexing().build());
        // Client connects to a non default port

        HotRodServerConfigurationBuilder hcb = new HotRodServerConfigurationBuilder();

        hs = HotRodTestingUtil.startHotRodServer(ecm, 11222);


//        hs.setMarshaller(new org.infinispan.commons.marshall.JavaSerializationMarshaller());


    }

    @AfterAll
    public static void teardown() {
        if (hs != null) {
            hs.stop();
        }
    }

*/
    @Inject
    MockResidencyDataCache residencyDataCache;

    @Test
    public void testPut() {

        residencyDataCache.store("a", "domedata");

        assertEquals("domedata", residencyDataCache.fetch("a"));
    }

}