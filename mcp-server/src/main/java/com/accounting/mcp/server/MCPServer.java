package com.accounting.mcp.server;

import com.accounting.mcp.client.LedgerApiClient;
import com.accounting.mcp.tools.ExecuteJournalTemplateTool;
import com.accounting.mcp.tools.MCPTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Main MCP Server that communicates via stdio with Claude Desktop
 */
@Slf4j
public class MCPServer {
    
    private final MCPRequestHandler requestHandler;

    public MCPServer(String ledgerBaseUrl, String username, String password) {
        // Create Gson without pretty printing - MCP requires compact JSON on single lines
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        
        // Initialize ledger API client
        LedgerApiClient ledgerClient = new LedgerApiClient(ledgerBaseUrl, username, password);
        
        // Register tools
        Map<String, MCPTool> tools = new HashMap<>();
        ExecuteJournalTemplateTool executeTemplateTool = new ExecuteJournalTemplateTool(ledgerClient);
        tools.put(executeTemplateTool.getName(), executeTemplateTool);
        
        this.requestHandler = new MCPRequestHandler(tools, gson);
        
        log.info("MCP Server initialized with {} tools", tools.size());
    }
    
    /**
     * Start the MCP server and listen for requests on stdin
     */
    public void start() {
        log.info("Starting MCP Server - listening on stdin...");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            log.info("Waiting for input on stdin...");
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    log.info("Received empty line, skipping");
                    continue;
                }
                
                log.info("Received request: {}", line);
                String response = requestHandler.handleRequest(line);
                
                // Write response to stdout - must be single line for MCP protocol
                // Notifications return null and don't need a response
                if (response != null) {
                    System.out.print(response);
                    System.out.print('\n');
                    System.out.flush();
                    log.info("Sent response (length={}): {}", response.length(), response.substring(0, Math.min(100, response.length())));
                } else {
                    log.info("No response needed (notification)");
                }
                log.info("Waiting for next request...");
            }
            log.info("stdin closed (readLine returned null) - exiting");
        } catch (IOException e) {
            log.error("Error reading from stdin", e);
        } catch (Exception e) {
            log.error("Unexpected error in main loop", e);
        }
        
        log.info("MCP Server stopped");
    }
    
    public static void main(String[] args) {
        // Read configuration from environment variables or args
        String ledgerBaseUrl = System.getenv("LEDGER_BASE_URL");
        String username = System.getenv("LEDGER_USERNAME");
        String password = System.getenv("LEDGER_PASSWORD");
        
        if (ledgerBaseUrl == null) {
            ledgerBaseUrl = "http://localhost:8080"; // default
        }
        
        if (username == null || password == null) {
            System.err.println("ERROR: LEDGER_USERNAME and LEDGER_PASSWORD environment variables must be set");
            System.exit(1);
        }
        
        MCPServer server = new MCPServer(ledgerBaseUrl, username, password);
        server.start();
    }
}
