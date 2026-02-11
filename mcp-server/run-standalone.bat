@echo off
REM Standalone test script for the MCP server
REM This allows you to test the server independently

set LEDGER_BASE_URL=http://localhost:8080
set LEDGER_USERNAME=your-username-here
set LEDGER_PASSWORD=your-password-here

java -jar build\libs\ledger-mcp-server-1.0.0.jar
