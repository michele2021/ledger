package com.accounting.mcp.protocol;

import com.google.gson.JsonObject;
import lombok.Data;

/**
 * Base MCP request following the JSON-RPC 2.0 specification
 */
@Data
public class MCPRequest {
    private String jsonrpc = "2.0";
    private Object id;  // Can be string, number, or null per JSON-RPC 2.0
    private String method;
    private JsonObject params;
}
