#!/bin/bash
# Standalone test script for the MCP server
# This allows you to test the server independently

export LEDGER_BASE_URL=http://localhost:8080
export LEDGER_USERNAME=your-username-here
export LEDGER_PASSWORD=your-password-here

java -jar build/libs/ledger-mcp-server-1.0.0.jar
