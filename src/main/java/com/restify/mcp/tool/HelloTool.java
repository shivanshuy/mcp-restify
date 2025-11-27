package com.restify.mcp.tool;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class HelloTool {

    /**
     * A simple hello tool that returns a greeting message.
     * @return "hello world" string
     */
    @McpTool(name = "hello", description = "A simple hello tool that returns hello world string.")
    public String hello() {
        return "hello world";
    }
}

