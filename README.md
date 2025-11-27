# Agent Weave MCP Server

Spring AI MCP (Model Context Protocol) server with HTTP Sync Stateless protocol.

## Prerequisites

- **Java 17 or higher**
- **Maven 3.6+**

Verify installations:
```bash
java -version
mvn -version
```

## Installation

1. Clone or navigate to the project directory:
```bash
cd agent-weave-mcp
```

2. No additional installation required - dependencies are managed by Maven.

## Build

Build the project using Maven:

```bash
mvn clean package
```

This will:
- Compile the source code
- Run tests
- Create a JAR file at `target/agent-weave-mcp-1.0.0.jar`

To skip tests during build:
```bash
mvn clean package -DskipTests
```

## Start Server

### Option 1: Using JAR file (Recommended)

```bash
java -jar target/agent-weave-mcp-1.0.0.jar
```

### Option 2: Using Maven

```bash
mvn spring-boot:run
```

The server will start on **http://localhost:9091**

You should see:
```
Tomcat started on port 9091 (http) with context path ''
Started AgentWeaveMcpApplication
Registered tools: 3
```

## MCP Endpoint

The MCP server endpoint is available at:
```
http://localhost:9091/mcp
```

## Call Hello Tool

### JSON Request Format

Send a POST request to `http://localhost:9091/mcp` with the following JSON body:

**Request Headers:**
- `Accept: application/json, text/event-stream`
- `Content-Type: application/json`

**Request Payload:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "hello",
    "arguments": {},
    "_meta": {
      "progressToken": 2
    }
  },
  "jsonrpc": "2.0",
  "id": 2
}
```

### Example: Using cURL

```bash
curl -X POST http://localhost:9091/mcp \
  -H "Accept: application/json, text/event-stream" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/call",
    "params": {
      "name": "hello",
      "arguments": {},
      "_meta": {
        "progressToken": 2
      }
    },
    "jsonrpc": "2.0",
    "id": 2
  }'
```

### Example: Using PowerShell

```powershell
$body = '{"method":"tools/call","params":{"name":"hello","arguments":{},"_meta":{"progressToken":2}},"jsonrpc":"2.0","id":2}'

$headers = @{
    "Accept" = "application/json, text/event-stream"
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "http://localhost:9091/mcp" `
    -Method Post `
    -Body $body `
    -Headers $headers
```

### Expected Response

```json
{
  "jsonrpc": "2.0",
  "id": "1",
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

## List Available Tools

To see all available tools:

**Request Headers:**
- `Accept: application/json, text/event-stream`
- `Content-Type: application/json`

**Request Payload:**
```json
{
  "method": "tools/list",
  "params": {},
  "jsonrpc": "2.0",
  "id": 2
}
```

### cURL Example

```bash
curl -X POST http://localhost:9091/mcp \
  -H "Accept: application/json, text/event-stream" \
  -H "Content-Type: application/json" \
  -d '{
    "method": "tools/list",
    "params": {},
    "jsonrpc": "2.0",
    "id": 2
  }'
```

## Configuration

The server uses **STATELESS** protocol (HTTP Sync Stateless) configured in `src/main/resources/application.properties`:

```properties
spring.ai.mcp.server.protocol=STATELESS
```

This provides:
- **HTTP-based** transport
- **Synchronous** request-response handling
- **Stateless** operation (no session state maintained)

## Available Tools

1. **hello** - Returns "hello world" string
2. **readOutlookEmails** - Read emails from Outlook mailbox
3. **readOutlookEmailById** - Read a specific email by ID

## Troubleshooting

### Port Already in Use
If port 9091 is already in use, change it in `src/main/resources/application.properties`:
```properties
server.port=9092
```

### Server Not Starting
- Verify Java version: `java -version` (should be 17+)
- Check Maven installation: `mvn -version`
- Review server logs for errors

### 400 Bad Request Error
- Ensure request body is valid JSON
- Verify `Content-Type: application/json` header is set
- Verify `Accept: application/json, text/event-stream` header is set
- Check JSON-RPC format is correct
- Ensure `_meta.progressToken` is included in params for tool calls
- Use MCP Inspector for interactive testing: `npx @modelcontextprotocol/inspector`

## Testing

Run unit tests:
```bash
mvn test
```

Test hello tool directly:
```bash
mvn test -Dtest=HelloToolTest
```
