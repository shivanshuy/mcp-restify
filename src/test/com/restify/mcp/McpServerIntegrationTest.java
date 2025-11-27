package com.restify.mcp;

import com.restify.mcp.tool.HelloTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HelloTool helloTool;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHelloToolDirect() {
        String result = helloTool.hello();
        assertEquals("hello world", result);
    }

    @Test
    void testMcpToolsList() throws Exception {
        String url = "http://localhost:" + port + "/mcp";
        
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/list");
        request.put("id", "1");
        request.put("params", new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = objectMapper.writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + response.getBody());
        
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getStatusCode().is2xxSuccessful(), 
            "Status should be 2xx. Actual: " + response.getStatusCode());
        
        Map<String, Object> responseMap = objectMapper.readValue(
            response.getBody(), Map.class);
        assertNotNull(responseMap.get("result"), 
            "Response should contain 'result'. Actual: " + responseMap);
        System.out.println("Tools list response: " + responseMap);
    }

    @Test
    void testMcpHelloToolCall() throws Exception {
        String url = "http://localhost:" + port + "/mcp";
        
        Map<String, Object> params = new HashMap<>();
        params.put("name", "hello");
        params.put("arguments", new HashMap<>());

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("id", "2");
        request.put("params", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = objectMapper.writeValueAsString(request);
        System.out.println("Request URL: " + url);
        System.out.println("Request Body: " + requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response Headers: " + response.getHeaders());
            System.out.println("Response Body: " + response.getBody());
            
            // If we get a response, parse it
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> responseMap = objectMapper.readValue(
                    response.getBody(), Map.class);
                
                // Check if response contains result or error
                if (responseMap.containsKey("result")) {
                    Object result = responseMap.get("result");
                    System.out.println("Result: " + result);
                    
                    // For tools/call, result might be a string or an object with content
                    if (result instanceof String) {
                        assertEquals("hello world", result, "Tool should return 'hello world'");
                    } else if (result instanceof Map) {
                        Map<String, Object> resultMap = (Map<String, Object>) result;
                        if (resultMap.containsKey("content")) {
                            System.out.println("Result contains content: " + resultMap.get("content"));
                        }
                    }
                } else if (responseMap.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                    System.out.println("Error in response: " + error);
                    fail("Tool call returned error: " + error);
                }
            } else {
                // If status is 2xx but body is null/empty, that's unexpected
                if (response.getStatusCode().is2xxSuccessful()) {
                    fail("Received 2xx status but empty response body");
                } else {
                    fail("Received " + response.getStatusCode() + " with empty response body");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.out.println("HTTP Error: " + e.getStatusCode());
            String errorBody = e.getResponseBodyAsString();
            System.out.println("Error Response Body: " + (errorBody != null ? errorBody : "(empty)"));
            System.out.println("Error Headers: " + e.getResponseHeaders());
            
            // If we get 400 with empty body, it might be a request format issue
            // The hello tool itself works (verified by direct test)
            // This suggests the MCP HTTP endpoint may require a different request format
            if (e.getStatusCode().value() == 400 && (errorBody == null || errorBody.isEmpty())) {
                System.out.println("\nNOTE: The hello tool works correctly when called directly.");
                System.out.println("The 400 error suggests the MCP HTTP endpoint may require");
                System.out.println("a different request format for STATELESS protocol.");
                System.out.println("Consider using MCP Inspector for interactive testing:");
                System.out.println("  npx @modelcontextprotocol/inspector");
            }
            throw e;
        }
    }
}

