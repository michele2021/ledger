package com.accounting.mcp.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * HTTP client to communicate with the ledger application
 */
@Slf4j
public class LedgerApiClient {
    
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final Gson gson;
    
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiry;
    
    public LedgerApiClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.gson = new Gson();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Login to get access and refresh tokens
     */
    public void login() throws Exception {
        String url = baseUrl + "/auth/login";
        
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(credentials)))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        log.debug("Logging in to: {}", url);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Login response status: {}", response.statusCode());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
            this.accessToken = responseBody.get("idToken").getAsString();
            this.refreshToken = responseBody.get("refreshToken").getAsString();
            
            // Assume token expires in 15 minutes if not specified
            long expiresInSeconds = responseBody.has("expiresIn") 
                ? responseBody.get("expiresIn").getAsLong() 
                : 900;
            this.tokenExpiry = Instant.now().plusSeconds(expiresInSeconds);
            
            log.debug("Login successful, token expires at: {}", tokenExpiry);
        } else {
            throw new RuntimeException("Login failed with status " + response.statusCode() + ": " + response.body());
        }
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Cannot connect to ledger API at " + url + ". Please ensure the server is running.", e);
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Refresh the access token using the refresh token
     */
    public void refreshAccessToken() throws Exception {
        String url = baseUrl + "/auth/refresh-token";

        JsonObject payload = new JsonObject();
        payload.addProperty("refreshToken", refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .timeout(Duration.ofSeconds(30))
                .build();

        log.info("Refreshing access token at: {}", url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
            this.accessToken = responseBody.get("accessToken").getAsString();

            // Update refresh token if provided
            if (responseBody.has("refreshToken")) {
                this.refreshToken = responseBody.get("refreshToken").getAsString();
            }

            // Update token expiry
            long expiresInSeconds = responseBody.has("expiresIn")
                    ? responseBody.get("expiresIn").getAsLong()
                    : 900;
            this.tokenExpiry = Instant.now().plusSeconds(expiresInSeconds);

            log.info("Token refreshed successfully, expires at: {}", tokenExpiry);
        } else {
            throw new RuntimeException("Token refresh failed with status " + response.statusCode() + ": " + response.body());
        }
    }
    
    /**
     * Ensure we have a valid access token, refreshing or logging in if needed
     */
    private void ensureValidToken() throws Exception {
        if (accessToken == null || tokenExpiry == null || Instant.now().isAfter(tokenExpiry)) {
            log.debug("Token expired or missing, logging in...");
            login();
        } else if (Instant.now().isAfter(tokenExpiry.minusSeconds(60))) {
            // Refresh if token expires in less than 60 seconds
            log.info("Token expiring soon, refreshing...");
            try {
                refreshAccessToken();
            } catch (Exception e) {
                log.warn("Token refresh failed, attempting re-login: {}", e.getMessage());
                login();
            }
        }
    }
    
    /**
     * Execute a journal template
     */
    public String executeTemplate(JsonObject payload) throws Exception {
        ensureValidToken();
        
        String url = baseUrl + "/accounting/journals/from-template";
        log.debug("Sending request to: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else if (response.statusCode() == 401) {
            // Token might be invalid, try to refresh and retry once
            log.warn("Received 401, attempting to refresh token and retry...");
            refreshAccessToken();
            return executeTemplate(payload); // Retry once
        } else {
            throw new RuntimeException("API request failed with status " + response.statusCode() + ": " + response.body());
        }
    }
    
    /**
     * Post a custom journal
     */
    public String postJournal(JsonObject payload) throws Exception {
        ensureValidToken();
        
        String url = baseUrl + "/accounting/journals";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        log.info("Sending request to: {}", url);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else if (response.statusCode() == 401) {
            // Token might be invalid, try to refresh and retry once
            log.warn("Received 401, attempting to refresh token and retry...");
            refreshAccessToken();
            return postJournal(payload); // Retry once
        } else {
            throw new RuntimeException("API request failed with status " + response.statusCode() + ": " + response.body());
        }
    }
}
