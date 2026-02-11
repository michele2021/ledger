# MCP Integration Guide for Ledger Application

This guide explains how to integrate the Ledger application with Claude Desktop using the Model Context Protocol (MCP).

## Overview

The MCP server acts as a bridge between Claude Desktop and your Ledger application:

```
Claude Desktop <--stdio--> MCP Server <--HTTP--> Ledger Application
```

## Architecture

### Components

1. **MCP Server** (`mcp-server/`): Standalone Java application that implements the MCP protocol
   - Communicates with Claude Desktop via stdin/stdout
   - Translates MCP tool calls to HTTP API requests
   - Handles authentication and error handling

2. **Ledger Application**: Your existing Dropwizard application
   - Exposes REST API endpoints
   - Uses JWT token-based authentication

### MCP Server Structure

```
mcp-server/
├── src/main/java/com/accounting/mcp/
│   ├── protocol/          # MCP protocol models (Request, Response, Tool, etc.)
│   ├── tools/             # MCP tool implementations
│   ├── client/            # HTTP client for Ledger API
│   └── server/            # MCP server and request handler
├── build.gradle           # Gradle build configuration
└── README.md             # MCP server documentation
```

## Building the MCP Server

### Prerequisites

- Java 21 or higher
- Gradle (included via wrapper)

### Build Steps

1. Navigate to the project root:
```bash
cd C:\ledger
```

2. Build the MCP server JAR:
```bash
gradlew :mcp-server:shadowJar
```

The JAR will be created at: `mcp-server/build/libs/ledger-mcp-server-1.0.0.jar`

3. (Optional) Install to MCP directory:
```bash
gradlew :mcp-server:installMCP
```

## Installing for Claude Desktop

### Step 1: Prepare the JAR

Copy the built JAR to a permanent location:

**Windows:**
```powershell
mkdir $env:USERPROFILE\.mcp\servers\ledger
copy mcp-server\build\libs\ledger-mcp-server-1.0.0.jar $env:USERPROFILE\.mcp\servers\ledger\
```

**macOS/Linux:**
```bash
mkdir -p ~/.mcp/servers/ledger
cp mcp-server/build/libs/ledger-mcp-server-1.0.0.jar ~/.mcp/servers/ledger/
```

### Step 2: Configure Claude Desktop

Edit the Claude Desktop configuration file:

**Windows:** `%APPDATA%\Claude\claude_desktop_config.json`  
**macOS:** `~/Library/Application Support/Claude/claude_desktop_config.json`

Add the following configuration:

```json
{
  "mcpServers": {
    "ledger": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\Users\\YourUsername\\.mcp\\servers\\ledger\\ledger-mcp-server-1.0.0.jar"
      ],
      "env": {
        "LEDGER_BASE_URL": "http://localhost:8080",
        "LEDGER_USERNAME": "your-username-here",
        "LEDGER_PASSWORD": "your-password-here"
      }
    }
  }
}
```

**Important:** 
- Replace `C:\\Users\\YourUsername` with your actual home directory (use double backslashes on Windows)
- Replace `your-username-here` with your ledger application username
- Replace `your-password-here` with your ledger application password
- Update `LEDGER_BASE_URL` if your application runs on a different host/port

### Step 3: Restart Claude Desktop

Close and reopen Claude Desktop for the changes to take effect.

## Available Tools

### execute_journal_template

Execute a predefined journal template in the ledger system.

**Parameters:**
- `type` (string, required): Template type
  - Examples: `PAYMENT_RECEIVED`, `PAYMENT_RECEIVED_CORRECTION`, `CLIENT_FEE_PAYOUT`, `JOURNAL_REVERSE`
- `accountCode` (string, required): Account code
- `amount` (object, required):
  - `quantity` (number): Amount value
  - `currencyCode` (string): Currency code (e.g., "EUR", "USD")
- `additionalData` (object, optional): Template-specific data
  - For `PAYMENT_RECEIVED`: `bankAccountCode`, `discountAmount`, `description`
  - For `JOURNAL_REVERSE`: `originalJournalId`
- `source` (string, optional): Source identifier (defaults to "MCP_CLAUDE")

**Example Usage in Claude:**

```
Execute a payment received template for account DEFAULT with amount 150.00 EUR,
bank account ItalyClients1, and discount of 10 EUR.
```

Claude will translate this to:
```json
{
  "type": "PAYMENT_RECEIVED",
  "accountCode": "DEFAULT",
  "amount": {
    "quantity": 150.00,
    "currencyCode": "EUR"
  },
  "additionalData": {
    "bankAccountCode": "ItalyClients1",
    "discountAmount": 10,
    "description": "Payment from DEFAULT, discount applied"
  },
  "source": "MCP_CLAUDE"
}
```

## Testing the Integration

### Test 1: Verify MCP Server Starts

Run the server standalone to check for errors:

**Windows:**
```powershell
cd mcp-server
.\run-standalone.bat
```

**macOS/Linux:**
```bash
cd mcp-server
chmod +x run-standalone.sh
./run-standalone.sh
```

The server should start and wait for input on stdin.

### Test 2: Send a Test Request

With the server running, send an initialize request:

```json
{"jsonrpc":"2.0","id":"1","method":"initialize","params":{}}
```

Expected response:
```json
{
  "jsonrpc": "2.0",
  "id": "1",
  "result": {
    "protocolVersion": "2024-11-05",
    "serverInfo": "Accounting Ledger MCP Server",
    "capabilities": {
      "tools": true
    }
  }
}
```

### Test 3: List Available Tools

Send a tools/list request:

```json
{"jsonrpc":"2.0","id":"2","method":"tools/list","params":{}}
```

You should see the `execute_journal_template` tool in the response.

### Test 4: Test in Claude Desktop

1. Open Claude Desktop
2. Look for the MCP server indicator (hammer icon or similar)
3. Ask Claude: "What tools do you have available?"
4. Claude should mention the ledger tool
5. Try executing a template: "Execute a payment received for account TEST with 100 EUR"

## Troubleshooting

### MCP Server Not Appearing in Claude Desktop

**Check:**
1. Configuration file path is correct
2. JSON syntax is valid (use a JSON validator)
3. File paths use correct separators (double backslash on Windows)
4. Claude Desktop was restarted after configuration

**View Claude Desktop logs:**
- Windows: `%APPDATA%\Claude\logs\`
- macOS: `~/Library/Logs/Claude/`

### Authentication Errors

**Symptoms:** Tool execution fails with 401 Unauthorized

**Solutions:**
1. Verify username and password are correct in `claude_desktop_config.json`
2. Check that the user has appropriate permissions in the Ledger application
3. Ensure the Ledger application's `/auth/login` and `/auth/refresh-token` endpoints are working
4. Test authentication manually:
   ```bash
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"your-username","password":"your-password"}'
   ```

### Connection Errors

**Symptoms:** Tool execution fails with connection refused or timeout

**Solutions:**
1. Verify Ledger application is running
2. Check `LEDGER_BASE_URL` is correct
3. Test the endpoint manually:
   ```bash
   # First get a token
   TOKEN=$(curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"your-username","password":"your-password"}' | jq -r '.accessToken')
   
   # Then test the API
   curl -X POST http://localhost:8080/accounting/journals/from-template \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{"type":"PAYMENT_RECEIVED","accountCode":"TEST","amount":{"quantity":100,"currencyCode":"EUR"},"source":"TEST"}'
   ```

### Check MCP Server Logs

Logs are written to: `~/.mcp/servers/ledger/mcp-server.log`

**Windows:**
```powershell
type $env:USERPROFILE\.mcp\servers\ledger\mcp-server.log
```

**macOS/Linux:**
```bash
tail -f ~/.mcp/servers/ledger/mcp-server.log
```

## Extending the MCP Server

### Adding New Tools

1. Create a new tool class implementing `MCPTool`:

```java
package com.accounting.mcp.tools;

import com.accounting.mcp.client.LedgerApiClient;
import com.accounting.mcp.protocol.Tool;
import com.accounting.mcp.protocol.ToolResult;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MyNewTool implements MCPTool {
    
    private final LedgerApiClient ledgerClient;
    
    @Override
    public String getName() {
        return "my_new_tool";
    }
    
    @Override
    public Tool getDefinition() {
        // Define tool schema
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        // ... add properties
        
        return Tool.builder()
                .name(getName())
                .description("Description of what this tool does")
                .inputSchema(schema)
                .build();
    }
    
    @Override
    public ToolResult execute(JsonObject arguments) {
        try {
            // Implement tool logic
            return ToolResult.success("Success message");
        } catch (Exception e) {
            return ToolResult.error("Error: " + e.getMessage());
        }
    }
}
```

2. Register the tool in `MCPServer.java`:

```java
MyNewTool myNewTool = new MyNewTool(ledgerClient);
tools.put(myNewTool.getName(), myNewTool);
```

3. Rebuild and reinstall:

```bash
gradlew :mcp-server:shadowJar
gradlew :mcp-server:installMCP
```

4. Restart Claude Desktop

### Adding New API Client Methods

Add methods to `LedgerApiClient.java` for new endpoints:

```java
public String myNewEndpoint(JsonObject payload) throws Exception {
    ensureValidToken();
    
    String url = baseUrl + "/accounting/my-endpoint";
    
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
            .timeout(Duration.ofSeconds(30))
            .build();
    
    HttpResponse<String> response = httpClient.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return response.body();
    } else if (response.statusCode() == 401) {
        // Token might be invalid, try to refresh and retry once
        refreshAccessToken();
        return myNewEndpoint(payload); // Retry once
    } else {
        throw new RuntimeException("API request failed: " + response.body());
    }
}
```

## Security Considerations

1. **Credential Storage**: Username and password are stored in Claude Desktop's config file. Ensure this file has appropriate permissions (readable only by your user).

2. **Token Management**: The MCP server automatically manages JWT tokens:
   - Access tokens are stored in memory only
   - Tokens are automatically refreshed before expiry
   - Automatic re-authentication if refresh fails

3. **Network Security**: If the Ledger application is exposed over a network, use HTTPS and consider additional authentication layers.

4. **Input Validation**: The MCP server passes user input to the Ledger API. Ensure the Ledger application validates all inputs.

5. **Audit Logging**: Consider logging all MCP tool executions for audit purposes.

## Production Deployment

For production use:

1. **Use HTTPS**: Configure the Ledger application to use HTTPS
2. **Secure Credentials**: Use environment variables or secure credential storage instead of plain text in config files
3. **Token Expiry**: Configure appropriate token expiry times (default: 15 minutes)
4. **Monitor Logs**: Set up log monitoring for both MCP server and Ledger application
5. **Rate Limiting**: Consider implementing rate limiting on the Ledger API
6. **Error Handling**: Ensure sensitive information is not leaked in error messages

## Support

For issues or questions:
1. Check the MCP server logs
2. Check the Ledger application logs
3. Review Claude Desktop logs
4. Test the Ledger API endpoints directly with curl
