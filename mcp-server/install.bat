@echo off
echo ========================================
echo Ledger MCP Server Installation Script
echo ========================================
echo.

REM Build the JAR
echo [1/3] Building MCP server JAR...
cd ..
call gradlew.bat :mcp-server:shadowJar
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
cd mcp-server
echo Build successful!
echo.

REM Create installation directory
echo [2/3] Creating installation directory...
set INSTALL_DIR=%USERPROFILE%\.mcp\servers\ledger
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
echo Directory created: %INSTALL_DIR%
echo.

REM Copy JAR
echo [3/3] Installing JAR...
copy /Y build\libs\ledger-mcp-server-1.0.0.jar "%INSTALL_DIR%\"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to copy JAR!
    pause
    exit /b 1
)
echo JAR installed successfully!
echo.

REM Show next steps
echo ========================================
echo Installation Complete!
echo ========================================
echo.
echo JAR location: %INSTALL_DIR%\ledger-mcp-server-1.0.0.jar
echo.
echo Next steps:
echo 1. Configure Claude Desktop by editing:
echo    %APPDATA%\Claude\claude_desktop_config.json
echo.
echo 2. Add this configuration:
echo    {
echo      "mcpServers": {
echo        "ledger": {
echo          "command": "java",
echo          "args": [
echo            "-jar",
echo            "%INSTALL_DIR:\=\\%\\ledger-mcp-server-1.0.0.jar"
echo          ],
echo          "env": {
echo            "LEDGER_BASE_URL": "http://localhost:8080",
echo            "LEDGER_USERNAME": "your-username-here",
echo            "LEDGER_PASSWORD": "your-password-here"
echo          }
echo        }
echo      }
echo    }
echo.
echo 3. Restart Claude Desktop
echo.
echo See MCP_INTEGRATION_GUIDE.md for detailed instructions.
echo.
pause
