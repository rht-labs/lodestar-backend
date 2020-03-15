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


    @Test
    public void getFileFromRepo() {
        given()
                .when().get("/engagements/config")
                .then()
                .statusCode(200)
                .body("emoji", is("\uD83E\uDD8A"));
    }

    @Test
    public void testOpenEndpoint() {
        given()
          .when().get("/engagements/open")
          .then()
             .statusCode(200)
             .body("hello", is("world"));
    }

    @Test
    public void testSecureEndpoint() {
        given()
                .when().get("/engagements/secure")
                .then()
                .statusCode(204);
        // since we are not actually logged in as a user,
        // we expect no content (the endpoint normally returns
        // username), but the request to a secure endpoint
        // should still succeed with 204 due to the test
        // application.properties file disabling auth during testing.
    }



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

        ecm.getCache().put("a", "domedata");
        HotRodServerConfigurationBuilder hcb = new HotRodServerConfigurationBuilder();

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