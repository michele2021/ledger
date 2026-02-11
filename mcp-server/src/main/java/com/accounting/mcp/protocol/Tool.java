package com.accounting.mcp.protocol;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;

/**
 * MCP Tool definition
 */
@Data
@Builder
public class Tool {
    private String name;
    private String description;
    private JsonObject inputSchema;
}
