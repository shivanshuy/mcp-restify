package com.restify.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * JSON-RPC 2.0 response DTO for MCP protocol
 */
public class McpResponse {
    private String jsonrpc;
    private Object id;
    private Object result;
    private Error error;

    @JsonGetter("jsonrpc")
    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    @JsonGetter("id")
    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    @JsonGetter("result")
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @JsonGetter("error")
    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private int code;
        private String message;
        private String data;

        @JsonGetter("code")
        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        @JsonGetter("message")
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @JsonGetter("data")
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}

