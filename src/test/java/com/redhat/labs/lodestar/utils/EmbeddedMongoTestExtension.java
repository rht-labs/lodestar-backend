package com.redhat.labs.lodestar.utils;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.exceptions.DistributionException;
import de.flapdoodle.embed.process.runtime.Network;

public class EmbeddedMongoTestExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback{

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedMongoTestExtension.class);

    private final List<String> defaultDatabaseNames = new ArrayList<>(Arrays.asList("admin", "config", "local"));

    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    private MongoClient mongoClient;
    private int port = 12345;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

        for(String dbName : mongoClient.listDatabaseNames()) {

            // skip any of the default mongo dbs
            if(defaultDatabaseNames.contains(dbName)) {
                continue;
            }

            LOGGER.debug("dropping collections for database: {}", dbName);

            // get database
            MongoDatabase db = mongoClient.getDatabase(dbName);

            // drop each collection from db
            for (String collectionName : db.listCollectionNames()) {
                LOGGER.debug("...dropping collection {}", collectionName);
                db.getCollection(collectionName).drop();
            }

        }

    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

        LOGGER.debug("stopping mongo...");
        // stop mongo
        stopMongod();

    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

        LOGGER.debug("starting mongo...");
        // start mongo
        startMongod();

        LOGGER.debug("creating mongo client...");
        createMongoClient();

    }

    private void createMongoClient() {

        if (null == mongoClient) {
            mongoClient = MongoClients.create("mongodb://localhost:12345");
        }

    }

    private IMongodConfig createMongodConfig() throws UnknownHostException, IOException {
        return new MongodConfigBuilder().version(Version.Main.PRODUCTION).net(new Net(port, Network.localhostIsIPv6()))
                .build();
    }

    private void startMongod() throws DistributionException, UnknownHostException, IOException {

        mongodExe = starter.prepare(createMongodConfig());
        mongod = mongodExe.start();

    }

    private void stopMongod() {

        if (null != mongod) {
            mongod.stop();
        }

        if (null != mongodExe) {
            mongodExe.stop();
        }

    }

}