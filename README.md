# 🚀 MCP Restify

<div align="center">

**A Spring Boot REST API server implementing the Model Context Protocol (MCP) with streamable HTTP support**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Build](#-build)
- [Start Server](#-start-server)
- [MCP Endpoint Documentation](#-mcp-endpoint-documentation)
  - [Initialize](#initialize)
  - [List Tools](#list-tools)
  - [Call Tool](#call-tool)
- [Request Headers](#-request-headers)
- [Response Format](#-response-format)
- [Available Tools](#-available-tools)
- [Examples](#-examples)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [Testing](#-testing)

---

## 🎯 Overview

**MCP Restify** is a production-ready Spring Boot application that exposes a RESTful API endpoint implementing the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/). It provides a stateless HTTP interface for AI agents to interact with tools and services.

### Key Highlights

- ✅ **JSON-RPC 2.0** compliant
- ✅ **Streamable HTTP** support (chunked responses)
- ✅ **Stateless** operation (cloud-ready)
- ✅ **Tool Discovery** via `@McpTool` annotations
- ✅ **Outlook Integration** for email operations

---

## ✨ Features

- 🔌 **RESTful MCP Server** - HTTP-based MCP protocol implementation
- 📡 **Streamable HTTP** - Support for chunked JSON responses
- 🛠️ **Tool Management** - Automatic discovery and registration of MCP tools
- 📧 **Outlook Integration** - Read emails from Microsoft Outlook
- 🔍 **JSON-RPC 2.0** - Full protocol compliance
- 🚀 **Production Ready** - Built on Spring Boot with comprehensive error handling

---

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17 or higher**
- **Maven 3.6+**

### Verify Installations

```bash
java -version
# Should show: openjdk version "17" or higher

mvn -version
# Should show: Apache Maven 3.6.0 or higher
```

---

## 🔧 Installation

1. **Clone or navigate to the project directory:**

```bash
cd mcp-restify
```

2. **No additional installation required** - All dependencies are managed by Maven and will be downloaded automatically during the build process.

---

## 🏗️ Build

Build the project using Maven:

```bash
mvn clean package
```

This command will:
- ✅ Clean previous build artifacts
- ✅ Compile all source code
- ✅ Run unit tests
- ✅ Package the application into a JAR file

**Output:** `target/mcp-restify-1.0.0.jar`

### Build Options

**Skip tests during build:**
```bash
mvn clean package -DskipTests
```

**Build with verbose output:**
```bash
mvn clean package -X
```

---

## 🚀 Start Server

### Option 1: Using Maven (Development)

```bash
mvn spring-boot:run
```

### Option 2: Using JAR File (Production)

```bash
java -jar target/mcp-restify-1.0.0.jar
```

### Option 3: Run with Custom Profile

```bash
java -jar target/mcp-restify-1.0.0.jar --spring.profiles.active=prod
```

### Server Status

The server will start on **http://localhost:9092**

You should see output similar to:
```
Tomcat started on port 9092 (http) with context path ''
Started McpRestifyApplication in X.XXX seconds
Registered MCP tools: 3
```

---

## 📡 MCP Endpoint Documentation

The MCP server exposes a single REST endpoint that handles all MCP protocol methods:

```
POST http://localhost:9092/mcp
```

All requests must follow the **JSON-RPC 2.0** specification.

---

### Request Headers

Every request to the MCP endpoint must include the following headers:

| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `Content-Type` | `application/json` | ✅ Yes | Specifies the request body format |
| `Accept` | `application/json, text/event-stream` | ✅ Yes | Specifies acceptable response formats |

**Example Headers:**
```
Content-Type: application/json
Accept: application/json, text/event-stream
```

> **Note:** When `Accept: text/event-stream` is included, the server will return a streamable HTTP response with `Transfer-Encoding: chunked`.

---

### Initialize

Initialize the MCP protocol connection and retrieve server capabilities.

**Request Payload:**
```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "mcp-client",
      "version": "1.0.0"
    }
  },
  "id": 1
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {}
    },
    "serverInfo": {
      "name": "mcp-restify",
      "version": "1.0.0"
    }
  }
}
```

---

### List Tools

Retrieve a list of all available MCP tools.

**Request Payload:**
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list",
  "params": {},
  "id": 2
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "hello",
        "description": "A simple hello tool that returns hello world string.",
        "inputSchema": {
          "type": "object",
          "properties": {},
          "required": []
        }
      },
      {
        "name": "readOutlookEmails",
        "description": "Read emails from Outlook mailbox. Can retrieve a list of emails with optional filtering.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "maxResults": {
              "type": "integer",
              "description": "Maximum number of emails to retrieve (default: 10)"
            },
            "folderId": {
              "type": "string",
              "description": "Mail folder ID (default: 'inbox')"
            }
          },
          "required": ["maxResults"]
        }
      },
      {
        "name": "readOutlookEmailById",
        "description": "Read a specific email from Outlook by its message ID.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "messageId": {
              "type": "string",
              "description": "The ID of the message to retrieve"
            }
          },
          "required": ["messageId"]
        }
      }
    ]
  }
}
```

---

### Call Tool

Execute a specific MCP tool with provided arguments.

**Request Payload:**
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "hello",
    "arguments": {},
    "_meta": {
      "progressToken": 2
    }
  },
  "id": 3
}
```

**Request Parameters:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | ✅ Yes | The name of the tool to call |
| `arguments` | `object` | ✅ Yes | Tool-specific arguments (can be empty `{}`) |
| `_meta.progressToken` | `number` | ⚠️ Optional | Token for progress tracking |

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "hello world"
      }
    ]
  }
}
```

---

## 📤 Response Format

All responses follow the JSON-RPC 2.0 specification:

### Success Response

```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "result": <result_data>
}
```

### Error Response

```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "error": {
    "code": <error_code>,
    "message": "<error_message>",
    "data": "<additional_error_data>"
  }
}
```

### Error Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `-32600` | Invalid Request | The JSON sent is not a valid Request object |
| `-32601` | Method not found | The method does not exist / is not available |
| `-32602` | Invalid params | Invalid method parameter(s) |
| `-32603` | Internal error | Internal JSON-RPC error |

---

## 🛠️ Available Tools

| Tool Name | Description | Parameters |
|-----------|-------------|------------|
| `hello` | Returns a simple "hello world" greeting | None |
| `readOutlookEmails` | Read emails from Outlook mailbox | `maxResults` (integer), `folderId` (string, optional) |
| `readOutlookEmailById` | Read a specific email by message ID | `messageId` (string) |

---

## 💡 Examples

### Example 1: Initialize (cURL)

```bash
curl -X POST http://localhost:9092/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {
        "name": "mcp-client",
        "version": "1.0.0"
      }
    },
    "id": 1
  }'
```

### Example 2: List Tools (cURL)

```bash
curl -X POST http://localhost:9092/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list",
    "params": {},
    "id": 2
  }'
```

### Example 3: Call Hello Tool (cURL)

```bash
curl -X POST http://localhost:9092/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "hello",
      "arguments": {},
      "_meta": {
        "progressToken": 2
      }
    },
    "id": 3
  }'
```

### Example 4: PowerShell

```powershell
$headers = @{
    "Content-Type" = "application/json"
    "Accept" = "application/json, text/event-stream"
}

$body = @{
    jsonrpc = "2.0"
    method = "tools/call"
    params = @{
        name = "hello"
        arguments = @{}
        _meta = @{
            progressToken = 2
        }
    }
    id = 3
} | ConvertTo-Json -Depth 10

$response = Invoke-RestMethod -Uri "http://localhost:9092/mcp" `
    -Method Post `
    -Body $body `
    -Headers $headers

$response | ConvertTo-Json -Depth 10
```

### Example 5: Python

```python
import requests
import json

url = "http://localhost:9092/mcp"
headers = {
    "Content-Type": "application/json",
    "Accept": "application/json, text/event-stream"
}

payload = {
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
        "name": "hello",
        "arguments": {},
        "_meta": {
            "progressToken": 2
        }
    },
    "id": 3
}

response = requests.post(url, headers=headers, json=payload)
print(json.dumps(response.json(), indent=2))
```

### Example 6: JavaScript (Node.js)

```javascript
const fetch = require('node-fetch');

const url = 'http://localhost:9092/mcp';
const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json, text/event-stream'
};

const payload = {
    jsonrpc: '2.0',
    method: 'tools/call',
    params: {
        name: 'hello',
        arguments: {},
        _meta: {
            progressToken: 2
        }
    },
    id: 3
};

fetch(url, {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(payload)
})
.then(res => res.json())
.then(data => console.log(JSON.stringify(data, null, 2)))
.catch(err => console.error('Error:', err));
```

---

## ⚙️ Configuration

Configuration is managed through `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=9092
spring.application.name=mcp-restify

# Outlook/Microsoft Graph Configuration (Optional)
outlook.client-id=${OUTLOOK_CLIENT_ID:}
outlook.client-secret=${OUTLOOK_CLIENT_SECRET:}
outlook.tenant-id=${OUTLOOK_TENANT_ID:}

# Logging Configuration
logging.level.com.restify.mcp=DEBUG
logging.level.org.springframework=INFO
```

### Environment Variables

For Outlook integration, set the following environment variables:

```bash
export OUTLOOK_CLIENT_ID=your-client-id
export OUTLOOK_CLIENT_SECRET=your-client-secret
export OUTLOOK_TENANT_ID=your-tenant-id
```

---

## 🔍 Troubleshooting

### Port Already in Use

If port 9092 is already in use, change it in `application.properties`:

```properties
server.port=9093
```

### Server Not Starting

1. **Verify Java version:**
   ```bash
   java -version
   # Should be 17 or higher
   ```

2. **Check Maven installation:**
   ```bash
   mvn -version
   ```

3. **Review server logs** for specific error messages

4. **Check for port conflicts:**
   ```bash
   # Windows
   netstat -ano | findstr :9092
   
   # Linux/Mac
   lsof -i :9092
   ```

### 400 Bad Request Error

- ✅ Ensure request body is valid JSON
- ✅ Verify `Content-Type: application/json` header is set
- ✅ Verify `Accept: application/json, text/event-stream` header is set
- ✅ Check JSON-RPC format is correct (`jsonrpc: "2.0"` is required)
- ✅ Ensure `method` field is one of: `initialize`, `tools/list`, `tools/call`

### Empty Response

- ✅ Check server logs for errors
- ✅ Verify the tool name exists (use `tools/list` to check)
- ✅ Ensure tool arguments match the tool's input schema

### Testing with MCP Inspector

For interactive testing, use the MCP Inspector:

```bash
npx @modelcontextprotocol/inspector
```

Then configure it to connect to:
```
http://localhost:9092/mcp
```

---

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
# Test HelloTool
mvn test -Dtest=HelloToolTest

# Test MCP Server Integration
mvn test -Dtest=McpServerIntegrationTest
```

### Test with Coverage

```bash
mvn test jacoco:report
```

---

## 📚 Additional Resources

- [Model Context Protocol Documentation](https://modelcontextprotocol.io/)
- [JSON-RPC 2.0 Specification](https://www.jsonrpc.org/specification)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring AI MCP Documentation](https://docs.spring.io/spring-ai/reference/api/mcp.html)

---

## 📝 License

This project is licensed under the MIT License.

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

<div align="center">

**Made with ❤️ using Spring Boot**

</div>
