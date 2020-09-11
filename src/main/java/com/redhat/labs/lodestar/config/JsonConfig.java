package com.redhat.labs.lodestar.config;

import javax.inject.Singleton;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyNamingStrategy;

import io.quarkus.jsonb.JsonbConfigCustomizer;

@Singleton
public class JsonConfig implements JsonbConfigCustomizer {

    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withFormatting(true).withPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CASE_WITH_UNDERSCORES);
    }

}
