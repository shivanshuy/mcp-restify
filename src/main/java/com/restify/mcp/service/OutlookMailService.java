package com.restify.mcp.service;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stateless service class for Outlook mail operations.
 * This class contains the business logic for interacting with Microsoft Graph API
 * to read emails and user information. It does not contain MCP tool annotations.
 * All methods require an access token parameter to maintain statelessness.
 */
@Service
public class OutlookMailService {

    private static final Logger log = LoggerFactory.getLogger(OutlookMailService.class);

    @Value("${outlook.client-id:}")
    private String clientId;

    @Value("${outlook.client-secret:}")
    private String clientSecret;

    @Value("${outlook.tenant-id:}")
    private String tenantId;

    private final GraphClientFactory graphClientFactory;

    public OutlookMailService(GraphClientFactory graphClientFactory) {
        this.graphClientFactory = graphClientFactory;
    }

    /**
     * Read emails from Outlook mailbox. Can retrieve a list of emails with optional filtering.
     *
     * @param accessToken The Microsoft Graph access token
     * @param maxResults  Maximum number of emails to retrieve (default: 10)
     * @param folderId    Optional folder ID (default: inbox)
     * @return List of email messages
     * @throws IllegalArgumentException if access token is null or empty
     * @throws RuntimeException         if email retrieval fails
     */
    public List<Map<String, Object>> readEmails(String accessToken, Integer maxResults, String folderId) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token is required");
        }

        try {
            GraphServiceClient<Request> graphClient = graphClientFactory.createClient(accessToken);
            int limit = maxResults != null ? maxResults : 10;
            String mailFolder = folderId != null ? folderId : "inbox";

            MessageCollectionPage messages = graphClient
                    .me()
                    .mailFolders(mailFolder)
                    .messages()
                    .buildRequest()
                    .top(limit)
                    .orderBy("receivedDateTime desc")
                    .get();

            List<Map<String, Object>> emailList = new ArrayList<>();
            
            if (messages != null && messages.getCurrentPage() != null) {
                for (Message message : messages.getCurrentPage()) {
                    Map<String, Object> email = Map.of(
                            "id", message.id != null ? message.id : "",
                            "subject", message.subject != null ? message.subject : "",
                            "from", message.from != null && message.from.emailAddress != null 
                                    ? message.from.emailAddress.address : "",
                            "receivedDateTime", message.receivedDateTime != null 
                                    ? message.receivedDateTime.toString() : "",
                            "bodyPreview", message.bodyPreview != null ? message.bodyPreview : "",
                            "isRead", message.isRead != null ? message.isRead : false,
                            "hasAttachments", message.hasAttachments != null ? message.hasAttachments : false
                    );
                    emailList.add(email);
                }
            }

            return emailList;
        } catch (Exception e) {
            log.error("Error reading emails: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read emails: " + e.getMessage(), e);
        }
    }

    /**
     * Read a specific email from Outlook by its message ID.
     *
     * @param accessToken The Microsoft Graph access token
     * @param messageId    The ID of the message to retrieve
     * @return Email message details
     * @throws IllegalArgumentException if access token is null or empty
     * @throws RuntimeException         if email retrieval fails or message not found
     */
    public Map<String, Object> readEmailById(String accessToken, String messageId) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token is required");
        }

        try {
            GraphServiceClient<Request> graphClient = graphClientFactory.createClient(accessToken);
            Message message = graphClient
                    .me()
                    .messages(messageId)
                    .buildRequest()
                    .get();

            if (message == null) {
                throw new RuntimeException("Message not found: " + messageId);
            }

            return Map.of(
                    "id", message.id != null ? message.id : "",
                    "subject", message.subject != null ? message.subject : "",
                    "from", message.from != null && message.from.emailAddress != null 
                            ? message.from.emailAddress.address : "",
                    "to", message.toRecipients != null && !message.toRecipients.isEmpty()
                            ? message.toRecipients.stream()
                                    .map(r -> r.emailAddress != null ? r.emailAddress.address : "")
                                    .collect(Collectors.joining(", "))
                            : "",
                    "receivedDateTime", message.receivedDateTime != null 
                            ? message.receivedDateTime.toString() : "",
                    "body", message.body != null && message.body.content != null 
                            ? message.body.content : "",
                    "bodyPreview", message.bodyPreview != null ? message.bodyPreview : "",
                    "isRead", message.isRead != null ? message.isRead : false,
                    "hasAttachments", message.hasAttachments != null ? message.hasAttachments : false
            );
        } catch (Exception e) {
            log.error("Error reading email by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read email: " + e.getMessage(), e);
        }
    }

    /**
     * Get user profile information from Microsoft Graph.
     *
     * @param accessToken The Microsoft Graph access token
     * @return User profile information including id, displayName, mail, and userPrincipalName
     * @throws IllegalArgumentException if access token is null or empty
     * @throws RuntimeException         if user profile retrieval fails
     */
    public Map<String, Object> getUserProfile(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token is required");
        }

        try {
            GraphServiceClient<Request> graphClient = graphClientFactory.createClient(accessToken);
            User user = graphClient.me().buildRequest().get();
            return Map.of(
                    "id", user.id != null ? user.id : "",
                    "displayName", user.displayName != null ? user.displayName : "",
                    "mail", user.mail != null ? user.mail : "",
                    "userPrincipalName", user.userPrincipalName != null ? user.userPrincipalName : ""
            );
        } catch (Exception e) {
            log.error("Error getting user profile: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user profile: " + e.getMessage(), e);
        }
    }
}

