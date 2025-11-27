package com.restify.mcp.service;

import com.restify.mcp.tool.HelloTool;
import com.restify.mcp.tool.OutlookMailTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Service to manage and invoke MCP tools.
 * Handles tool registration, discovery, and execution.
 */
@Service
public class McpToolService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolService.class);
    private final Map<String, ToolInfo> tools = new HashMap<>();
    private final ObjectMapper objectMapper;

    public McpToolService(HelloTool helloTool, OutlookMailTool outlookMailTool, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        registerTool(helloTool);
        registerTool(outlookMailTool);
        logger.info("Registered {} MCP tools", tools.size());
    }

    /**
     * Register a tool by scanning for @McpTool annotated methods
     */
    private void registerTool(Object toolInstance) {
        Class<?> toolClass = toolInstance.getClass();
        Method[] methods = toolClass.getDeclaredMethods();

        for (Method method : methods) {
            // Check for @McpTool annotation (from Spring AI MCP)
            if (method.isAnnotationPresent(org.springaicommunity.mcp.annotation.McpTool.class)) {
                org.springaicommunity.mcp.annotation.McpTool annotation = 
                    method.getAnnotation(org.springaicommunity.mcp.annotation.McpTool.class);
                
                String toolName = annotation.name();
                String description = annotation.description();
                
                ToolInfo toolInfo = new ToolInfo();
                toolInfo.setName(toolName);
                toolInfo.setDescription(description);
                toolInfo.setInstance(toolInstance);
                toolInfo.setMethod(method);
                
                // Extract parameter information
                List<Map<String, Object>> parameters = new ArrayList<>();
                Parameter[] params = method.getParameters();
                for (Parameter param : params) {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("name", param.getName());
                    paramInfo.put("type", param.getType().getSimpleName());
                    
                    if (param.isAnnotationPresent(org.springaicommunity.mcp.annotation.McpToolParam.class)) {
                        org.springaicommunity.mcp.annotation.McpToolParam paramAnnotation = 
                            param.getAnnotation(org.springaicommunity.mcp.annotation.McpToolParam.class);
                        paramInfo.put("description", paramAnnotation.description());
                    }
                    
                    parameters.add(paramInfo);
                }
                toolInfo.setParameters(parameters);
                
                tools.put(toolName, toolInfo);
                logger.debug("Registered tool: {} - {}", toolName, description);
            }
        }
    }

    /**
     * Call a tool by name with arguments
     */
    public Object callTool(String toolName, JsonNode arguments) throws Exception {
        ToolInfo toolInfo = tools.get(toolName);
        if (toolInfo == null) {
            throw new IllegalArgumentException("Tool '" + toolName + "' not found");
        }

        Method method = toolInfo.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        // Map JSON arguments to method parameters
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName();
            Class<?> paramType = param.getType();

            JsonNode argValue = arguments.has(paramName) ? arguments.get(paramName) : null;

            if (argValue == null || argValue.isNull()) {
                // Handle optional parameters - use default value if primitive, null otherwise
                if (paramType.isPrimitive()) {
                    throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
                }
                args[i] = null;
            } else {
                args[i] = convertJsonNodeToType(argValue, paramType);
            }
        }

        // Invoke the method
        Object result = method.invoke(toolInfo.getInstance(), args);
        
        // Format result according to MCP protocol
        return formatToolResult(result);
    }

    /**
     * Convert JsonNode to the required parameter type
     */
    private Object convertJsonNodeToType(JsonNode node, Class<?> targetType) {
        if (node.isNull()) {
            return null;
        }

        if (targetType == String.class) {
            return node.asText();
        } else if (targetType == Integer.class || targetType == int.class) {
            return node.asInt();
        } else if (targetType == Long.class || targetType == long.class) {
            return node.asLong();
        } else if (targetType == Double.class || targetType == double.class) {
            return node.asDouble();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return node.asBoolean();
        } else if (List.class.isAssignableFrom(targetType)) {
            return objectMapper.convertValue(node, List.class);
        } else if (Map.class.isAssignableFrom(targetType)) {
            return objectMapper.convertValue(node, Map.class);
        } else {
            // Try to convert using Jackson
            return objectMapper.convertValue(node, targetType);
        }
    }

    /**
     * Format tool result according to MCP protocol
     * MCP expects result.content array with text items
     */
    private Object formatToolResult(Object result) {
        Map<String, Object> mcpResult = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put("type", "text");
        
        if (result instanceof String) {
            contentItem.put("text", result);
        } else {
            // Convert complex objects to JSON string
            try {
                contentItem.put("text", objectMapper.writeValueAsString(result));
            } catch (Exception e) {
                contentItem.put("text", result.toString());
            }
        }
        
        content.add(contentItem);
        mcpResult.put("content", content);
        
        return mcpResult;
    }

    /**
     * List all available tools
     */
    public Object listTools() {
        List<Map<String, Object>> toolsList = new ArrayList<>();
        
        for (ToolInfo toolInfo : tools.values()) {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", toolInfo.getName());
            tool.put("description", toolInfo.getDescription());
            
            // Always include inputSchema, even if empty
            Map<String, Object> inputSchema = new HashMap<>();
            inputSchema.put("type", "object");
            
            if (toolInfo.getParameters() != null && !toolInfo.getParameters().isEmpty()) {
                inputSchema.put("properties", createPropertiesFromParameters(toolInfo.getParameters()));
                // Add required array for non-nullable parameters
                List<String> required = new ArrayList<>();
                for (Map<String, Object> param : toolInfo.getParameters()) {
                    String paramName = (String) param.get("name");
                    String paramType = (String) param.get("type");
                    // Primitive types are required
                    if (paramType != null && (paramType.equals("int") || paramType.equals("Integer") ||
                            paramType.equals("long") || paramType.equals("Long") ||
                            paramType.equals("double") || paramType.equals("Double") ||
                            paramType.equals("float") || paramType.equals("Float") ||
                            paramType.equals("boolean") || paramType.equals("Boolean"))) {
                        required.add(paramName);
                    }
                }
                if (!required.isEmpty()) {
                    inputSchema.put("required", required);
                }
            } else {
                inputSchema.put("properties", new HashMap<>());
            }
            
            tool.put("inputSchema", inputSchema);
            toolsList.add(tool);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolsList);
        return result;
    }

    /**
     * Create properties map from parameter list for inputSchema
     */
    private Map<String, Object> createPropertiesFromParameters(List<Map<String, Object>> parameters) {
        Map<String, Object> properties = new HashMap<>();
        for (Map<String, Object> param : parameters) {
            Map<String, Object> prop = new HashMap<>();
            prop.put("type", mapJavaTypeToJsonType((String) param.get("type")));
            if (param.containsKey("description")) {
                prop.put("description", param.get("description"));
            }
            properties.put((String) param.get("name"), prop);
        }
        return properties;
    }

    /**
     * Map Java type to JSON schema type
     */
    private String mapJavaTypeToJsonType(String javaType) {
        switch (javaType.toLowerCase()) {
            case "string":
                return "string";
            case "integer":
            case "int":
                return "integer";
            case "long":
                return "integer";
            case "double":
            case "float":
                return "number";
            case "boolean":
                return "boolean";
            default:
                return "string";
        }
    }

    /**
     * Internal class to store tool information
     */
    private static class ToolInfo {
        private String name;
        private String description;
        private Object instance;
        private Method method;
        private List<Map<String, Object>> parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getInstance() {
            return instance;
        }

        public void setInstance(Object instance) {
            this.instance = instance;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public List<Map<String, Object>> getParameters() {
            return parameters;
        }

        public void setParameters(List<Map<String, Object>> parameters) {
            this.parameters = parameters;
        }
    }
}

