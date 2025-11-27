package com.restify.mcp.service;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory class for creating Microsoft Graph clients.
 * Creates stateless Graph clients on-demand using the provided access token.
 */
@Component
public class GraphClientFactory {

    private static final Logger log = LoggerFactory.getLogger(GraphClientFactory.class);

    /**
     * Creates a new GraphServiceClient instance with the provided access token.
     * This method creates a stateless client that can be used for a single request.
     *
     * @param accessToken The Microsoft Graph access token
     * @return A configured GraphServiceClient instance
     * @throws IllegalArgumentException if accessToken is null or empty
     */
    public GraphServiceClient<Request> createClient(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null or empty");
        }

        final String token = accessToken;
        IAuthenticationProvider authProvider = new IAuthenticationProvider() {
            @Override
            public java.util.concurrent.CompletableFuture<String> getAuthorizationTokenAsync(java.net.URL requestUrl) {
                return java.util.concurrent.CompletableFuture.completedFuture(token);
            }
        };
        
        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }
}

