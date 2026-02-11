package com.accounting.mcp.protocol;

import lombok.Data;

/**
 * MCP error object
 */
@Data
public class MCPError {
    private int code;
    private String message;
    private Object data;
}
