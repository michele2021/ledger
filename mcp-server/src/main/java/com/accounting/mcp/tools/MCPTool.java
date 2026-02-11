package com.accounting.mcp.tools;

import com.accounting.mcp.protocol.Tool;
import com.accounting.mcp.protocol.ToolResult;
import com.google.gson.JsonObject;

/**
 * Interface for MCP tools
 */
public interface MCPTool {
    
    /**
     * Get the tool name
     */
    String getName();
    
    /**
     * Get the tool definition for MCP protocol
     */
    Tool getDefinition();
    
    /**
     * Execute the tool with given arguments
     */
    ToolResult execute(JsonObject arguments);
}
