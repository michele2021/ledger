package com.accounting.mcp.tools;

import com.accounting.mcp.client.LedgerApiClient;
import com.accounting.mcp.protocol.Tool;
import com.accounting.mcp.protocol.ToolResult;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Tool for executing journal templates in the ledger system
 */
@Slf4j
@RequiredArgsConstructor
public class ExecuteJournalTemplateTool implements MCPTool {
    
    private final LedgerApiClient ledgerClient;
    
    @Override
    public String getName() {
        return "execute_journal_template";
    }
    
    @Override
    public Tool getDefinition() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        // type property
        JsonObject typeProperty = new JsonObject();
        typeProperty.addProperty("type", "string");
        typeProperty.addProperty("description", "Template type (e.g., PAYMENT_RECEIVED, PAYMENT_RECEIVED_CORRECTION, CLIENT_FEE_PAYOUT)");
        properties.add("type", typeProperty);
        
        // accountCode property
        JsonObject accountCodeProperty = new JsonObject();
        accountCodeProperty.addProperty("type", "string");
        accountCodeProperty.addProperty("description", "Account code");
        properties.add("accountCode", accountCodeProperty);
        
        // amount property
        JsonObject amountProperty = new JsonObject();
        amountProperty.addProperty("type", "object");
        amountProperty.addProperty("description", "Amount with quantity and currency code");
        JsonObject amountProps = new JsonObject();
        JsonObject quantityProp = new JsonObject();
        quantityProp.addProperty("type", "number");
        amountProps.add("quantity", quantityProp);
        JsonObject currencyProp = new JsonObject();
        currencyProp.addProperty("type", "string");
        amountProps.add("currencyCode", currencyProp);
        amountProperty.add("properties", amountProps);
        properties.add("amount", amountProperty);
        
        // additionalData property
        JsonObject additionalDataProperty = new JsonObject();
        additionalDataProperty.addProperty("type", "object");
        additionalDataProperty.addProperty("description", "Additional template-specific data. PAYMENT_RECEIVED requires txReference field.");
        properties.add("additionalData", additionalDataProperty);
        
        // source property
        JsonObject sourceProperty = new JsonObject();
        sourceProperty.addProperty("type", "string");
        sourceProperty.addProperty("description", "Source of the request (default: MCP_CLAUDE)");
        properties.add("source", sourceProperty);
        
        schema.add("properties", properties);
        
        // Required fields as JSON array
        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("type");
        required.add("accountCode");
        required.add("amount");
        schema.add("required", required);
        
        return Tool.builder()
                .name(getName())
                .description("Execute a journal template in the accounting ledger system. " +
                        "Supports templates like PAYMENT_RECEIVED (requires txReference in additionalData), " +
                        "PAYMENT_RECEIVED_CORRECTION, CLIENT_FEE_PAYOUT, JOURNAL_REVERSE (requires journalId in additionalData), etc. " +
                        "Note: API returns 200 immediately but processes asynchronously - check ledger logs for actual errors.")
                .inputSchema(schema)
                .build();
    }
    
    @Override
    public ToolResult execute(JsonObject arguments) {
        try {
            log.info("Executing journal template: type={}", arguments.has("type") ? arguments.get("type").getAsString() : "unknown");
            log.debug("Template arguments: {}", arguments);
            
            // Build the request payload
            JsonObject payload = new JsonObject();
            payload.addProperty("type", arguments.get("type").getAsString());
            payload.addProperty("accountCode", arguments.get("accountCode").getAsString());
            payload.add("amount", arguments.get("amount"));
            
            if (arguments.has("additionalData")) {
                payload.add("additionalData", arguments.get("additionalData"));
            }
            
            if (arguments.has("source")) {
                payload.addProperty("source", arguments.get("source").getAsString());
            } else {
                // Default to FRONT_END_APP as required by the API
                payload.addProperty("source", "FRONT_END_APP");
            }

            // Execute the template via the ledger API
            String response = ledgerClient.executeTemplate(payload);
            
            return ToolResult.success("Journal template executed successfully. Response: " + response);
            
        } catch (Exception e) {
            log.error("Error executing journal template", e);
            return ToolResult.error("Failed to execute journal template: " + e.getMessage());
        }
    }
}
