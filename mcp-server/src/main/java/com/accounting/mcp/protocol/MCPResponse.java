package com.accounting.mcp.protocol;

import lombok.Data;

/**
 * Base MCP response following the JSON-RPC 2.0 specification
 */
@Data
public class MCPResponse<T> {
    private String jsonrpc = "2.0";
    private Object id;  // Can be string, number, or null per JSON-RPC 2.0
    private T result;
    private MCPError error;
    
    public static <T> MCPResponse<T> success(Object id, T result) {
        MCPResponse<T> response = new MCPResponse<>();
        response.setId(id);
        response.setResult(result);
        return response;
    }
    
    public static <T> MCPResponse<T> error(Object id, int code, String message) {
        MCPResponse<T> response = new MCPResponse<>();
        response.setId(id);
        MCPError error = new MCPError();
        error.setCode(code);
        error.setMessage(message);
        response.setError(error);
        return response;
    }
}
