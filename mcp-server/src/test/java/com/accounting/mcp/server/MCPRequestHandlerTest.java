package com.accounting.mcp.server;

import com.accounting.mcp.client.LedgerApiClient;
import com.accounting.mcp.protocol.MCPRequest;
import com.accounting.mcp.tools.ExecuteJournalTemplateTool;
import com.accounting.mcp.tools.MCPTool;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MCPRequestHandlerTest {
    
    private MCPRequestHandler handler;
    private Gson gson;
    
    @BeforeEach
    void setUp() {
        gson = new Gson();
        
        // Create mock tools
        Map<String, MCPTool> tools = new HashMap<>();
        LedgerApiClient mockClient = new LedgerApiClient("http://localhost:8080", "test-user", "test-password");
        ExecuteJournalTemplateTool tool = new ExecuteJournalTemplateTool(mockClient);
        tools.put(tool.getName(), tool);
        
        handler = new MCPRequestHandler(tools, gson);
    }
    
    @Test
    void testHandleInitialize() {
        MCPRequest request = new MCPRequest();
        request.setId("1");
        request.setMethod("initialize");
        request.setParams(new JsonObject());
        
        String response = handler.handleRequest(gson.toJson(request));
        assertNotNull(response);
        assertTrue(response.contains("protocolVersion"));
        assertTrue(response.contains("serverInfo"));
    }
    
    @Test
    void testHandleListTools() {
        MCPRequest request = new MCPRequest();
        request.setId("2");
        request.setMethod("tools/list");
        request.setParams(new JsonObject());
        
        String response = handler.handleRequest(gson.toJson(request));
        assertNotNull(response);
        assertTrue(response.contains("execute_journal_template"));
    }
    
    @Test
    void testHandlePing() {
        MCPRequest request = new MCPRequest();
        request.setId("3");
        request.setMethod("ping");
        request.setParams(new JsonObject());
        
        String response = handler.handleRequest(gson.toJson(request));
        assertNotNull(response);
        assertTrue(response.contains("ok"));
    }
    
    @Test
    void testHandleUnknownMethod() {
        MCPRequest request = new MCPRequest();
        request.setId("4");
        request.setMethod("unknown/method");
        request.setParams(new JsonObject());
        
        String response = handler.handleRequest(gson.toJson(request));
        assertNotNull(response);
        assertTrue(response.contains("error"));
        assertTrue(response.contains("Method not found"));
    }
}
