package com.redhat.labs.omp.socket;

import java.io.IOException;
import java.security.Principal;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

@WebFilter(urlPatterns = "/engagements/events")
public class SocketAuthenticationFilter implements Filter {

    public static Logger LOGGER = LoggerFactory.getLogger(SocketAuthenticationFilter.class);

    @Inject
    private JWTAuthContextInfo authContextInfo;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        LOGGER.debug("socket authentication filter called with request {} and response {}", request, response);

        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;

        // get token from request
        String token = servletRequest.getParameter("access-token");
        LOGGER.debug("token from request parameter {}", token);

        if (token == null || token.trim().isEmpty()) {

            LOGGER.debug("token not found, returning error response.");
            returnForbiddenError(servletResponse, "An access token is required to connect");
            return;

        }

        try {

            // use jwt auth context to parse token
            JWTCallerPrincipalFactory factory = JWTCallerPrincipalFactory.instance();
            JWTCallerPrincipal callerPrincipal = factory.parse(token, authContextInfo);

            // add username to request if authenticated
            chain.doFilter(new AuthenticatedRequest(servletRequest, callerPrincipal.getName()), servletResponse);

        } catch (ParseException e) {

            LOGGER.debug("failed to parse token {}", token, e);
            returnForbiddenError(servletResponse, "Invalid access token");

        }

    }

    private void returnForbiddenError(HttpServletResponse response, String message) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, message);
    }

    private static class AuthenticatedRequest extends HttpServletRequestWrapper {

        private String username;

        public AuthenticatedRequest(HttpServletRequest request, String username) {
            super(request);
            this.username = username;
        }

        @Override
        public Principal getUserPrincipal() {
            return () -> username;
        }

    }

}
