package com.accounting.mcp.tools;

import com.accounting.mcp.client.LedgerApiClient;
import com.accounting.mcp.protocol.Tool;
import com.accounting.mcp.protocol.ToolResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Tool for posting custom journal entries with explicit debit/credit lines
 */
@Slf4j
@RequiredArgsConstructor
public class PostJournalTool implements MCPTool {

    private final LedgerApiClient ledgerClient;

    @Override
    public String getName() {
        return "post_journal";
    }

    @Override
    public Tool getDefinition() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();

        // bookingDate property
        JsonObject bookingDateProp = new JsonObject();
        bookingDateProp.addProperty("type", "string");
        bookingDateProp.addProperty("description", "Booking date in yyyy-MM-dd format");
        properties.add("bookingDate", bookingDateProp);

        // description property
        JsonObject descriptionProp = new JsonObject();
        descriptionProp.addProperty("type", "string");
        descriptionProp.addProperty("description", "Journal description");
        properties.add("description", descriptionProp);

        // journalLines property
        JsonObject journalLinesProp = new JsonObject();
        journalLinesProp.addProperty("type", "array");
        journalLinesProp.addProperty("description", "Array of journal lines (debit/credit entries)");

        JsonObject lineItemSchema = new JsonObject();
        lineItemSchema.addProperty("type", "object");

        JsonObject lineProps = new JsonObject();

        JsonObject registerTypeProp = new JsonObject();
        registerTypeProp.addProperty("type", "string");
        registerTypeProp.addProperty("description", "Register type (e.g., Payable, BankAccount, BankBalance, BankReceived, Fee, VAT_OUT, VAT_IN, Clearing, Receivable, Revenue, UnappliedCash)");
        lineProps.add("registerType", registerTypeProp);

        JsonObject lineTypeProp = new JsonObject();
        lineTypeProp.addProperty("type", "string");
        lineTypeProp.addProperty("description", "Line type: DEBIT or CREDIT");
        lineProps.add("lineType", lineTypeProp);

        JsonObject amountProp = new JsonObject();
        amountProp.addProperty("type", "number");
        amountProp.addProperty("description", "Amount value");
        lineProps.add("amount", amountProp);

        JsonObject currencyProp = new JsonObject();
        currencyProp.addProperty("type", "string");
        currencyProp.addProperty("description", "Currency code (EUR, GBP, USD)");
        lineProps.add("currency", currencyProp);

        lineItemSchema.add("properties", lineProps);

        JsonArray lineRequired = new JsonArray();
        lineRequired.add("registerType");
        lineRequired.add("lineType");
        lineRequired.add("amount");
        lineRequired.add("currency");
        lineItemSchema.add("required", lineRequired);

        journalLinesProp.add("items", lineItemSchema);
        properties.add("journalLines", journalLinesProp);

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("bookingDate");
        required.add("journalLines");
        schema.add("required", required);

        return Tool.builder()
                .name(getName())
                .description("Post a custom journal entry with explicit debit/credit lines to the accounting ledger. " +
                        "Each journal must have balanced debit and credit lines.")
                .inputSchema(schema)
                .build();
    }

    @Override
    public ToolResult execute(JsonObject arguments) {
        try {
            log.info("Posting custom journal entry");
            log.debug("Journal arguments: {}", arguments);

            if (!arguments.has("bookingDate") || arguments.get("bookingDate").getAsString().isBlank()) {
                return ToolResult.error("Missing required parameter: bookingDate (format: yyyy-MM-dd)");
            }

            if (!arguments.has("journalLines") || !arguments.get("journalLines").isJsonArray()) {
                return ToolResult.error("Missing required parameter: journalLines must be a non-empty array of debit/credit entries");
            }

            JsonArray journalLines = arguments.getAsJsonArray("journalLines");
            if (journalLines.isEmpty()) {
                return ToolResult.error("Missing required parameter: journalLines must be a non-empty array of debit/credit entries");
            }

            for (int i = 0; i < journalLines.size(); i++) {
                JsonObject line = journalLines.get(i).getAsJsonObject();
                if (!line.has("registerType") || !line.has("lineType") || !line.has("amount") || !line.has("currency")) {
                    return ToolResult.error("Journal line " + (i + 1) + " is missing required fields: registerType, lineType, amount, and currency are all required");
                }
            }

            JsonObject payload = new JsonObject();

            if (arguments.has("bookingDate")) {
                payload.addProperty("bookingDate", arguments.get("bookingDate").getAsString());
            }

            if (arguments.has("description")) {
                payload.addProperty("description", arguments.get("description").getAsString());
            }

            payload.add("journalLines", journalLines);

            String response = ledgerClient.postJournal(payload);

            return ToolResult.success("Journal posted successfully. Response: " + response);

        } catch (Exception e) {
            log.error("Error posting journal entry", e);
            return ToolResult.error("Failed to post journal entry: " + e.getMessage());
        }
    }
}
