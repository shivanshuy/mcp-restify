package com.restify.mcp.controller;

import com.restify.mcp.dto.McpRequest;
import com.restify.mcp.dto.McpResponse;
import com.restify.mcp.service.McpToolService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for MCP (Model Context Protocol) server endpoint.
 * Handles JSON-RPC 2.0 requests for MCP tool calls.
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);
    private final McpToolService mcpToolService;
    private final ObjectMapper objectMapper;

    public McpController(McpToolService mcpToolService, ObjectMapper objectMapper) {
        this.mcpToolService = mcpToolService;
        this.objectMapper = objectMapper;
    }

    /**
     * Main MCP endpoint that handles JSON-RPC 2.0 requests.
     * Supports both regular JSON responses and streamable HTTP (chunked JSON) based on Accept header.
     * Supports methods: initialize, tools/call, tools/list
     *
     * @param request JSON-RPC 2.0 request
     * @param acceptHeader Accept header to determine response format
     * @return JSON-RPC 2.0 response (JSON or streamable HTTP)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object handleMcpRequest(
            @RequestBody McpRequest request,
            @RequestHeader(value = "Accept", defaultValue = "application/json") String acceptHeader) {
        logger.debug("Received MCP request: method={}, id={}, accept={}", request.getMethod(), request.getId(), acceptHeader);

        try {
            // Validate JSON-RPC version
            if (!"2.0".equals(request.getJsonrpc())) {
                McpResponse errorResponse = createErrorResponse(
                    request.getId(),
                    -32600,
                    "Invalid Request",
                    "jsonrpc must be '2.0'"
                );
                if (supportsStreamableHttp(acceptHeader)) {
                    return createStreamableResponse(errorResponse);
                } else {
                    return ResponseEntity.ok(errorResponse);
                }
            }

            // Handle different methods
            McpResponse response;
            switch (request.getMethod()) {
                case "initialize":
                    response = handleInitialize(request);
                    break;
                case "tools/call":
                    response = handleToolCall(request);
                    break;
                case "tools/list":
                    response = handleToolsList(request);
                    break;
                default:
                    response = createErrorResponse(
                        request.getId(),
                        -32601,
                        "Method not found",
                        "Method '" + request.getMethod() + "' is not supported"
                    );
            }

            // Return streamable HTTP or regular response based on Accept header
            if (supportsStreamableHttp(acceptHeader)) {
                logger.info("Creating streamable response for Accept: {}", acceptHeader);
                try {
                    // For streamable HTTP, create manual response and return as regular response with chunked encoding
                    Map<String, Object> manualResponse = new HashMap<>();
                    manualResponse.put("jsonrpc", "2.0");
                    manualResponse.put("id", response.getId());
                    if (response.getResult() != null) {
                        manualResponse.put("result", response.getResult());
                    }
                    if (response.getError() != null) {
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("code", response.getError().getCode());
                        errorMap.put("message", response.getError().getMessage());
                        if (response.getError().getData() != null) {
                            errorMap.put("data", response.getError().getData());
                        }
                        manualResponse.put("error", errorMap);
                    }
                    String jsonResponse = objectMapper.writeValueAsString(manualResponse);
                    logger.info("Streamable response JSON: {}", jsonResponse);
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Transfer-Encoding", "chunked")
                        .body(jsonResponse);
                } catch (Exception e) {
                    logger.error("Error creating streamable response", e);
                    // Fallback to regular response
                    return ResponseEntity.ok(response);
                }
            } else {
                logger.debug("Creating regular JSON response");
                ResponseEntity<McpResponse> jsonResponse = ResponseEntity.ok(response);
                return jsonResponse;
            }
        } catch (Exception e) {
            logger.error("Error processing MCP request", e);
            McpResponse errorResponse = createErrorResponse(
                request.getId(),
                -32603,
                "Internal error",
                e.getMessage()
            );
            if (supportsStreamableHttp(acceptHeader)) {
                try {
                    // For streamable HTTP, create manual response
                    Map<String, Object> manualResponse = new HashMap<>();
                    manualResponse.put("jsonrpc", "2.0");
                    manualResponse.put("id", errorResponse.getId());
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("code", errorResponse.getError().getCode());
                    errorMap.put("message", errorResponse.getError().getMessage());
                    if (errorResponse.getError().getData() != null) {
                        errorMap.put("data", errorResponse.getError().getData());
                    }
                    manualResponse.put("error", errorMap);
                    String jsonResponse = objectMapper.writeValueAsString(manualResponse);
                    return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Transfer-Encoding", "chunked")
                        .body(jsonResponse);
                } catch (Exception ex) {
                    logger.error("Error creating streamable error response", ex);
                    return ResponseEntity.ok(errorResponse);
                }
            } else {
                return ResponseEntity.ok(errorResponse);
            }
        }
    }

    /**
     * Check if the Accept header supports streamable HTTP (text/event-stream)
     */
    private boolean supportsStreamableHttp(String acceptHeader) {
        return acceptHeader != null && acceptHeader.contains("text/event-stream");
    }

    /**
     * Create a streamable HTTP response (chunked JSON)
     * For MCP streamable HTTP, we send the JSON response in chunks
     * Always uses manual Map response to avoid DTO serialization issues
     */
    private ResponseEntity<StreamingResponseBody> createStreamableResponse(McpResponse response) {
        try {
            // Always create manual response for streaming to avoid DTO serialization issues
            Map<String, Object> manualResponse = new HashMap<>();
            manualResponse.put("jsonrpc", "2.0");
            manualResponse.put("id", response.getId());
            
            if (response.getResult() != null) {
                manualResponse.put("result", response.getResult());
            }
            if (response.getError() != null) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("code", response.getError().getCode());
                errorMap.put("message", response.getError().getMessage());
                if (response.getError().getData() != null) {
                    errorMap.put("data", response.getError().getData());
                }
                manualResponse.put("error", errorMap);
            }
            
            String jsonResponse = objectMapper.writeValueAsString(manualResponse);
            logger.info("Streamable response JSON: {}", jsonResponse);
            
            if (jsonResponse == null || jsonResponse.trim().equals("{}") || jsonResponse.trim().isEmpty()) {
                logger.error("CRITICAL: Manual response also serializes to empty! Response data: {}", manualResponse);
                // Last resort: build JSON manually
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\"jsonrpc\":\"2.0\",\"id\":");
                if (response.getId() != null) {
                    if (response.getId() instanceof String) {
                        jsonBuilder.append("\"").append(response.getId()).append("\"");
                    } else {
                        jsonBuilder.append(response.getId());
                    }
                } else {
                    jsonBuilder.append("null");
                }
                if (response.getResult() != null) {
                    jsonBuilder.append(",\"result\":").append(objectMapper.writeValueAsString(response.getResult()));
                }
                if (response.getError() != null) {
                    jsonBuilder.append(",\"error\":{\"code\":").append(response.getError().getCode())
                        .append(",\"message\":\"").append(response.getError().getMessage()).append("\"");
                    if (response.getError().getData() != null) {
                        jsonBuilder.append(",\"data\":\"").append(response.getError().getData().replace("\"", "\\\"")).append("\"");
                    }
                    jsonBuilder.append("}");
                }
                jsonBuilder.append("}");
                jsonResponse = jsonBuilder.toString();
                logger.info("Using manually built JSON: {}", jsonResponse);
            }
            
            final String finalJsonResponse = jsonResponse;
            StreamingResponseBody stream = new StreamingResponseBody() {
                @Override
                public void writeTo(OutputStream outputStream) throws IOException {
                    logger.info("Writing to stream: {}", finalJsonResponse);
                    byte[] bytes = finalJsonResponse.getBytes(StandardCharsets.UTF_8);
                    outputStream.write(bytes);
                    outputStream.flush();
                    logger.debug("Written {} bytes to stream", bytes.length);
                }
            };
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Transfer-Encoding", "chunked")
                .body(stream);
        } catch (Exception e) {
            logger.error("Error creating streamable response", e);
            // Fallback: create error response as stream
            try {
                final String errorJson = "{\"jsonrpc\":\"2.0\",\"id\":" + 
                    (response != null && response.getId() != null ? 
                        (response.getId() instanceof String ? "\"" + response.getId() + "\"" : response.getId()) : "null") + 
                    ",\"error\":{\"code\":-32603,\"message\":\"Internal error\",\"data\":\"Error creating stream: " + 
                    e.getMessage().replace("\"", "\\\"").replace("\n", "\\n") + "\"}}";
                logger.info("Error fallback JSON: {}", errorJson);
                StreamingResponseBody errorStream = new StreamingResponseBody() {
                    @Override
                    public void writeTo(OutputStream outputStream) throws IOException {
                        outputStream.write(errorJson.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                };
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Transfer-Encoding", "chunked")
                    .body(errorStream);
            } catch (Exception ex) {
                logger.error("Error creating error stream", ex);
                // Last resort: return error JSON manually
                final String lastResortJson = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32603,\"message\":\"Internal error\",\"data\":\"Stream creation failed\"}}";
                StreamingResponseBody emptyStream = new StreamingResponseBody() {
                    @Override
                    public void writeTo(OutputStream outputStream) throws IOException {
                        outputStream.write(lastResortJson.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                };
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Transfer-Encoding", "chunked")
                    .body(emptyStream);
            }
        }
    }

    /**
     * Handle initialize method - MCP protocol initialization
     */
    private McpResponse handleInitialize(McpRequest request) {
        logger.debug("Handling initialize request");
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        
        Map<String, Object> capabilities = new HashMap<>();
        Map<String, Object> toolsCapability = new HashMap<>();
        capabilities.put("tools", toolsCapability);
        result.put("capabilities", capabilities);
        
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "mcp-restify");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        logger.debug("Initialize result: {}", result);
        return createSuccessResponse(request.getId(), result);
    }

    /**
     * Handle tools/call method - execute a specific tool
     */
    private McpResponse handleToolCall(McpRequest request) {
        if (request.getParams() == null || !request.getParams().has("name")) {
            return createErrorResponse(
                request.getId(),
                -32602,
                "Invalid params",
                "Missing 'name' parameter in params"
            );
        }

        String toolName = request.getParams().get("name").asText();
        var arguments = request.getParams().has("arguments") 
            ? request.getParams().get("arguments") 
            : objectMapper.createObjectNode();

        logger.debug("Calling tool: {}, with arguments: {}", toolName, arguments);

        try {
            Object result = mcpToolService.callTool(toolName, arguments);
            return createSuccessResponse(request.getId(), result);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(
                request.getId(),
                -32602,
                "Invalid params",
                e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error calling tool: " + toolName, e);
            return createErrorResponse(
                request.getId(),
                -32603,
                "Internal error",
                "Error executing tool: " + e.getMessage()
            );
        }
    }

    /**
     * Handle tools/list method - return list of available tools
     */
    private McpResponse handleToolsList(McpRequest request) {
        logger.debug("Listing available tools");
        try {
            var tools = mcpToolService.listTools();
            return createSuccessResponse(request.getId(), tools);
        } catch (Exception e) {
            logger.error("Error listing tools", e);
            return createErrorResponse(
                request.getId(),
                -32603,
                "Internal error",
                "Error listing tools: " + e.getMessage()
            );
        }
    }

    /**
     * Create a successful JSON-RPC response
     */
    private McpResponse createSuccessResponse(Object id, Object result) {
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        response.setResult(result);
        return response;
    }

    /**
     * Create an error JSON-RPC response
     */
    private McpResponse createErrorResponse(Object id, int code, String message, String data) {
        McpResponse response = new McpResponse();
        response.setJsonrpc("2.0");
        response.setId(id);
        
        var error = new McpResponse.Error();
        error.setCode(code);
        error.setMessage(message);
        error.setData(data);
        response.setError(error);
        
        return response;
    }
}

