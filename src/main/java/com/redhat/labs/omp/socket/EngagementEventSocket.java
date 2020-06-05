package com.redhat.labs.omp.socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ServerEndpoint("/engagements/events")
public class EngagementEventSocket {

    private static final Logger LOG = LoggerFactory.getLogger(EngagementEventSocket.class);

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        LOG.debug("session '{}' left", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session.getId());
        LOG.error("onError", throwable);
    }

    @OnMessage
    public void onMessage(String message) {
        // do nothing
    }

    public void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result -> {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }

}
