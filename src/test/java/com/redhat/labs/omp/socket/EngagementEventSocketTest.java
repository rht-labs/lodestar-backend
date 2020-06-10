package com.redhat.labs.omp.socket;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.jwt.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.labs.utils.EmbeddedMongoTest;
import com.redhat.labs.utils.TokenUtils;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@EmbeddedMongoTest
@QuarkusTest
public class EngagementEventSocketTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/engagements/events")
    URI uri;

    @Inject
    EngagementEventSocket socket;

    /*
     * - No Token - Invalid Token - Valid Token
     */

    @Test
    public void testWebsocketNoToken() {

        try {
            ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri);
        } catch (DeploymentException | IOException e) {

            Throwable[] suppressed = e.getSuppressed();
            if (suppressed.length == 1) {
                Throwable t = suppressed[0].getCause();
                Assertions.assertEquals("Invalid handshake response getStatus: 403 Forbidden",
                        t.getMessage());
            } else {
                fail("failed with exception " + e.getMessage());
            }
        }

    }

    @Test
    public void testWebsocketExpiredToken() throws Exception {

        // create new token
        HashMap<String, Long> timeClaims = new HashMap<>();
        timeClaims.put(Claims.exp.name(), 1l);
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        // expire token
        TimeUnit.SECONDS.sleep(1);

        // add token to query param
        URI tokenUri = UriBuilder.fromUri(uri).queryParam("access-token", token).build();

        try {
            ContainerProvider.getWebSocketContainer().connectToServer(Client.class, tokenUri);
        } catch (DeploymentException | IOException e) {

            Throwable[] suppressed = e.getSuppressed();
            if (suppressed.length == 1) {
                Throwable t = suppressed[0].getCause();
                Assertions.assertEquals("Invalid handshake response getStatus: 403 Forbidden",
                        t.getMessage());
            } else {
                fail("failed with exception " + e.getMessage());
            }
        }

    }
    
    @Test
    public void testWebsocketEvents() throws Exception {

        // create new token
        HashMap<String, Long> timeClaims = new HashMap<>();
        String token = TokenUtils.generateTokenString("/JwtClaimsReader.json", timeClaims);

        // add token to query param
        URI tokenUri = UriBuilder.fromUri(uri).queryParam("access-token", token).build();

        // get message from socket
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, tokenUri)) {
            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
            socket.broadcast("testing");
            Assertions.assertEquals("testing", MESSAGES.poll(10, TimeUnit.SECONDS));
        }

    }

    @ClientEndpoint
    public static class Client {

        @OnOpen
        public void open(Session session) {
            MESSAGES.add("CONNECT");
        }

        @OnMessage
        void message(String msg) {
            MESSAGES.add(msg);
        }

    }

}
