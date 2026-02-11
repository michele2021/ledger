package com.accounting.mcp.server;

import com.accounting.mcp.protocol.*;
import com.accounting.mcp.tools.MCPTool;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles MCP protocol requests
 */
@Slf4j
@RequiredArgsConstructor
public class MCPRequestHandler {
    
    private final Map<String, MCPTool> tools;
    private final Gson gson;
    
    /**
     * Process an MCP request and return a response
     */
    public String handleRequest(String requestJson) {
        try {
            MCPRequest request = gson.fromJson(requestJson, MCPRequest.class);
            log.info("Handling MCP request: method={}, id={}", request.getMethod(), request.getId());
            
            Object result = switch (request.getMethod()) {
                case "initialize" -> handleInitialize(request);
                case "tools/list" -> handleListTools(request);
                case "tools/call" -> handleCallTool(request);
                case "ping" -> handlePing(request);
                default -> {
                    log.warn("Unknown method: {}", request.getMethod());
                    yield MCPResponse.error(request.getId(), -32601, "Method not found: " + request.getMethod());
                }
            };
            
            if (result instanceof MCPResponse) {
                return gson.toJson(result);
            } else {
                return gson.toJson(MCPResponse.success(request.getId(), result));
            }
            
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return gson.toJson(MCPResponse.error(null, -32603, "Internal error: " + e.getMessage()));
        }
    }
    
    private Object handleInitialize(MCPRequest request) {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");
        result.addProperty("serverInfo", "Accounting Ledger MCP Server");
        
        JsonObject capabilities = new JsonObject();
        capabilities.addProperty("tools", true);
        result.add("capabilities", capabilities);
        
        log.info("MCP server initialized");
        return result;
    }
    
    private Object handleListTools(MCPRequest request) {
        List<Tool> toolList = new ArrayList<>(tools.values().stream()
                .map(MCPTool::getDefinition)
                .toList());
        
        JsonObject result = new JsonObject();
        result.add("tools", gson.toJsonTree(toolList));
        
        log.info("Listed {} tools", toolList.size());
        return result;
    }
    
    private Object handleCallTool(MCPRequest request) {
        JsonObject params = request.getParams();
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.get("arguments").getAsJsonObject() : new JsonObject();
        
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);
        
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            return MCPResponse.error(request.getId(), -32602, "Tool not found: " + toolName);
        }
        
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }
    
    private Object handlePing(MCPRequest request) {
        JsonObject result = new JsonObject();
        result.addProperty("status", "ok");
        return result;
    }
}
