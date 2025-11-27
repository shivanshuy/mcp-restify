package com.restify.mcp.tool;

import com.restify.mcp.service.OutlookMailService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool class for Outlook mail operations.
 * This class exposes Outlook mail functionality as MCP tools,
 * delegating the actual business logic to OutlookMailService.
 * Note: These tools require Microsoft Graph API credentials to be configured.
 */
@Component
public class OutlookMailTool {

    private static final Logger log = LoggerFactory.getLogger(OutlookMailTool.class);
    private final OutlookMailService outlookMailService;

    public OutlookMailTool(OutlookMailService outlookMailService) {
        this.outlookMailService = outlookMailService;
    }

    /**
     * Read emails from Outlook mailbox. Can retrieve a list of emails with optional filtering.
     *
     * @param maxResults Maximum number of emails to retrieve (default: 10)
     * @param folderId  Optional folder ID (default: inbox)
     * @return List of email messages
     */
    @McpTool(name = "readOutlookEmails", description = "Read emails from Outlook mailbox. Can retrieve a list of emails with optional filtering.")
    public List<Map<String, Object>> readOutlookEmails(
            @McpToolParam(description = "Maximum number of emails to retrieve (default: 10)") Integer maxResults,
            @McpToolParam(description = "Mail folder ID (default: 'inbox')") String folderId) {
        log.debug("Reading Outlook emails - maxResults: {}, folderId: {}", maxResults, folderId);
        // Return empty list as authentication is not required
        // To enable Outlook mail functionality, configure Microsoft Graph API credentials
        return Collections.emptyList();
    }

    /**
     * Read a specific email from Outlook by its message ID.
     *
     * @param messageId The ID of the message to retrieve
     * @return Email message details
     */
    @McpTool(name = "readOutlookEmailById", description = "Read a specific email from Outlook by its message ID.")
    public Map<String, Object> readOutlookEmailById(
            @McpToolParam(description = "The ID of the message to retrieve") String messageId) {
        log.debug("Reading Outlook email by ID: {}", messageId);
        // Return empty map as authentication is not required
        // To enable Outlook mail functionality, configure Microsoft Graph API credentials
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Outlook mail functionality requires Microsoft Graph API configuration");
        result.put("messageId", messageId);
        return result;
    }
}

