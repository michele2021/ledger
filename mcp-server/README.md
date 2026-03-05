# Ledger MCP Server

This is a Model Context Protocol (MCP) server that enables Claude Desktop to interact with the Accounting Ledger application.

## Features

- **Execute Journal Templates**: Execute predefined journal templates (PAYMENT_RECEIVED, PAYMENT_RECEIVED_CORRECTION, CLIENT_FEE_PAYOUT, etc.)
- **Secure Communication**: Uses JWT token-based authentication with automatic token refresh
- **Stdio Protocol**: Communicates with Claude Desktop via stdin/stdout

## Building

Build the standalone JAR:

```bash
cd mcp-server
../gradlew shadowJar
```

The JAR will be created at: `build/libs/ledger-mcp-server-1.0.0.jar`

## Installation for Claude Desktop

1. Build the JAR as described above

2. Create the MCP server directory:
```bash
mkdir -p ~/.mcp/servers/ledger
```

3. Copy the JAR:
```bash
cp build/libs/ledger-mcp-server-1.0.0.jar ~/.mcp/servers/ledger/
```

Or use the install task:
```bash
../gradlew installMCP
```

4. Configure Claude Desktop by editing the MCP settings file:

**On Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
**On macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

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

Replace:
- `C:\\Users\\YourUsername` with your actual home directory path (use double backslashes on Windows)
- `your-username-here` with your ledger application username
- `your-password-here` with your ledger application password
- `http://localhost:8080` with your ledger application URL if different

5. Restart Claude Desktop

## Available Tools

### execute_journal_template

Execute a journal template in the accounting ledger system.

**Parameters:**
- `type` (required): Template type (e.g., "PAYMENT_RECEIVED", "PAYMENT_RECEIVED_CORRECTION", "CLIENT_FEE_PAYOUT")
- `accountCode` (required): Account code
- `amount` (required): Object with `quantity` (number) and `currencyCode` (string)
- `additionalData` (optional): Template-specific additional data
- `source` (optional): Source of the request (defaults to "MCP_CLAUDE")

**Usage examples in Claude:**

```
Please execute a payment received template for account DEFAULT with amount 150.00 EUR, 
bank account ItalyClients1, and discount of 10.
```

```
Please post a journal for account CMPZERO with description "test mcp journal post", with the following journal entries:
- CLEARING DEBIT 16 EUR
- REVENUE CREDIT 3 EUR
- UNAPPLIEDCASH CREDIT 13 EUR
```

## Configuration

The server reads configuration from environment variables:

- `LEDGER_BASE_URL`: Base URL of the ledger application (default: http://localhost:8080)
- `LEDGER_USERNAME`: Username for authentication (required)
- `LEDGER_PASSWORD`: Password for authentication (required)

The server automatically handles:
- Initial login via `/auth/login` endpoint
- Token refresh via `/auth/refresh-token` endpoint
- Automatic re-authentication on token expiry

## Logging

Logs are written to:
- `~/.mcp/servers/ledger/mcp-server.log` (file)
- stderr (for debugging)

## Development

To add new tools:

1. Create a new class implementing `MCPTool` in `com.accounting.mcp.tools`
2. Register the tool in `MCPServer` constructor
3. Rebuild and reinstall

## Troubleshooting

1. **Server not appearing in Claude Desktop**: Check the config file path and JSON syntax
2. **Authentication errors**: Verify the username and password are correct and the user has appropriate permissions
3. **Connection errors**: Ensure the ledger application is running and accessible at the configured URL
4. **Check logs**: Review `~/.mcp/servers/ledger/mcp-server.log` for detailed error messages
