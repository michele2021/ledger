#!/bin/bash

echo "========================================"
echo "Ledger MCP Server Installation Script"
echo "========================================"
echo

# Build the JAR
echo "[1/3] Building MCP server JAR..."
cd ..
./gradlew :mcp-server:shadowJar
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed!"
    exit 1
fi
cd mcp-server
echo "Build successful!"
echo

# Create installation directory
echo "[2/3] Creating installation directory..."
INSTALL_DIR="$HOME/.mcp/servers/ledger"
mkdir -p "$INSTALL_DIR"
echo "Directory created: $INSTALL_DIR"
echo

# Copy JAR
echo "[3/3] Installing JAR..."
cp build/libs/ledger-mcp-server-1.0.0.jar "$INSTALL_DIR/"
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to copy JAR!"
    exit 1
fi
echo "JAR installed successfully!"
echo

# Show next steps
echo "========================================"
echo "Installation Complete!"
echo "========================================"
echo
echo "JAR location: $INSTALL_DIR/ledger-mcp-server-1.0.0.jar"
echo
echo "Next steps:"
echo "1. Configure Claude Desktop by editing:"
echo "   ~/Library/Application Support/Claude/claude_desktop_config.json"
echo
echo "2. Add this configuration:"
cat << EOF
   {
     "mcpServers": {
       "ledger": {
         "command": "java",
         "args": [
           "-jar",
           "$INSTALL_DIR/ledger-mcp-server-1.0.0.jar"
         ],
         "env": {
           "LEDGER_BASE_URL": "http://localhost:8080",
           "LEDGER_USERNAME": "your-username-here",
           "LEDGER_PASSWORD": "your-password-here"
         }
       }
     }
   }
EOF
echo
echo "3. Restart Claude Desktop"
echo
echo "See MCP_INTEGRATION_GUIDE.md for detailed instructions."
echo
