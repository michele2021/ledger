package com.accounting.mcp.protocol;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of a tool execution
 */
@Data
@Builder
public class ToolResult {
    private List<Content> content;
    private boolean isError;
    
    @Data
    @Builder
    public static class Content {
        private String type;
        private String text;
    }
    
    public static ToolResult success(String text) {
        return ToolResult.builder()
                .content(List.of(Content.builder()
                        .type("text")
                        .text(text)
                        .build()))
                .isError(false)
                .build();
    }
    
    public static ToolResult error(String text) {
        return ToolResult.builder()
                .content(List.of(Content.builder()
                        .type("text")
                        .text(text)
                        .build()))
                .isError(true)
                .build();
    }
}
