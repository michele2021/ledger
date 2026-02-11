# Quick Start Guide

Get the MCP server running with Claude Desktop in 5 minutes.

## Prerequisites

- Java 21+ installed
- Ledger application running on `http://localhost:8080`
- Valid username and password for the Ledger application
- Claude Desktop installed

## Step 1: Build (30 seconds)

```bash
# From the ledger root directory
cd C:\ledger

# Build the MCP server
gradlew :mcp-server:shadowJar
```

## Step 2: Install (30 seconds)

**Windows:**
```powershell
cd mcp-server
.\install.bat
```

**macOS/Linux:**
```bash
cd mcp-server
chmod +x install.sh
./install.sh
```

## Step 3: Configure Claude Desktop (2 minutes)

1. Open the Claude Desktop config file:
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`

2. Add this configuration (replace `YOUR_USERNAME` and `YOUR_PASSWORD`):

**Windows:**
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
        "LEDGER_USERNAME": "YOUR_USERNAME",
        "LEDGER_PASSWORD": "YOUR_PASSWORD"
      }
    }
  }
}
```

**macOS/Linux:**
```json
{
  "mcpServers": {
    "ledger": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/YourUsername/.mcp/servers/ledger/ledger-mcp-server-1.0.0.jar"
      ],
      "env": {
        "LEDGER_BASE_URL": "http://localhost:8080",
        "LEDGER_USERNAME": "YOUR_USERNAME",
        "LEDGER_PASSWORD": "YOUR_PASSWORD"
      }
    }
  }
}
```

3. Save the file

## Step 4: Restart Claude Desktop (30 seconds)

Close and reopen Claude Desktop.

## Step 5: Test (1 minute)

In Claude Desktop, try:

```
What tools do you have available?
```

Claude should mention the ledger tool.

Then try:

```
Execute a payment received template for account DEFAULT 
with amount 150.00 EUR, bank account ItalyClients1, 
and discount of 10 EUR.
```

## Verification

Check if it worked:

1. **Claude responds with success**: Tool executed successfully
2. **Check Ledger logs**: Should show the API request
3. **Check MCP logs**: `~/.mcp/servers/ledger/mcp-server.log`

## Troubleshooting

### Tool not appearing?

1. Check config file syntax (use a JSON validator)
2. Check file paths are correct
3. Restart Claude Desktop
4. Check Claude Desktop logs

### Authentication error?

1. Verify username and password are correct
2. Check user has appropriate permissions
3. Test authentication with curl:
   ```bash
   # First login to get token
   curl -X POST http://localhost:8080/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"YOUR_USERNAME","password":"YOUR_PASSWORD"}'
   
   # Then use the token to test the API
   curl -X POST http://localhost:8080/accounting/journals/from-template \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
     -d '{"type":"PAYMENT_RECEIVED","accountCode":"TEST","amount":{"quantity":100,"currencyCode":"EUR"},"source":"TEST"}'
   ```

### Connection error?

1. Verify Ledger application is running
2. Check the URL is correct
3. Test with curl (see above)

## Next Steps

- Read `MCP_INTEGRATION_GUIDE.md` for detailed documentation
- Read `STRUCTURE.md` to understand the architecture
- Add more tools by following the extension guide

## Common Commands

**Rebuild after changes:**
```bash
gradlew :mcp-server:shadowJar
```

**Reinstall:**
```bash
gradlew :mcp-server:installMCP
```

**View logs:**
```bash
# Windows
type %USERPROFILE%\.mcp\servers\ledger\mcp-server.log

# macOS/Linux
tail -f ~/.mcp/servers/ledger/mcp-server.log
```

**Test standalone:**
```bash
# Windows
cd mcp-server
.\run-standalone.bat

# macOS/Linux
cd mcp-server
./run-standalone.sh
```

## Example Prompts for Claude

Once configured, try these prompts:

1. **List tools:**
   ```
   What tools do you have available?
   ```

2. **Execute payment template:**
   ```
   Execute a payment received for account ABC123 
   with 500 EUR to bank account MainBank
   ```

3. **Execute with discount:**
   ```
   Post a payment of 1000 USD for account XYZ789 
   with a 50 USD discount to bank account USBank
   ```

4. **Reverse a journal:**
   ```
   Execute a journal reverse template for journal ID 12345
   ```

## Support

For issues:
1. Check the logs
2. Review `MCP_INTEGRATION_GUIDE.md`
3. Test the API endpoint directly
4. Check Claude Desktop logs
