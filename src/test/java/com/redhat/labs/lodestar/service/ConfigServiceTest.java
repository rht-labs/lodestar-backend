package com.redhat.labs.lodestar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.rest.client.LodeStarGitLabAPIService;

class ConfigServiceTest {

    LodeStarGitLabAPIService gitApi;

    ConfigService service;

    @BeforeEach
    void setup() {

        gitApi = Mockito.mock(LodeStarGitLabAPIService.class);

        service = new ConfigService();
        service.gitApi = gitApi;

    }

    @AfterEach
    void tearDown() {
        Mockito.reset(gitApi);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "v1" })
    void testGetConfigData(String version) {

        service.getConfigData(version);

        Mockito.verify(gitApi).getConfigFile();

    }

    @Test
    void testGetConfigDataV2() {

        service.getConfigData("v2");

        Mockito.verify(gitApi).getConfigFileV2();

    }

    @Test
    void testGetConfigDataUnknown() {

        Response r = service.getConfigData("unknown");

        assertNotNull(r);
        assertEquals(404, r.getStatus());

    }

}