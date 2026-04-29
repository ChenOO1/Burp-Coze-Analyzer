package burp.coze.api;

import burp.api.montoya.logging.Logging;
import burp.coze.config.ConfigManager;
import burp.coze.model.CozeSuggestion;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CozeApiClient {
    private static final String COZE_API_URL = "https://api.coze.cn/v1/workflow/run";
    private final ConfigManager configManager;
    private final Logging logging;
    private final HttpClient httpClient;
    private final Gson gson;

    public CozeApiClient(ConfigManager configManager, Logging logging) {
        this.configManager = configManager;
        this.logging = logging;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<List<CozeSuggestion>> analyzeRequest(String requestContent) {
        String apiKey = configManager.getApiKey();
        String workflowId = configManager.getWorkflowId();

        if (apiKey.isEmpty() || workflowId.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("API Key or Workflow ID is not configured."));
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("workflow_id", workflowId);
        
        JsonObject parameters = new JsonObject();
        parameters.addProperty("request", requestContent);
        payload.add("parameters", parameters);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(COZE_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logging.logToError("Coze API Error: " + response.statusCode() + " - " + response.body());
                        throw new RuntimeException("Coze API returned status " + response.statusCode());
                    }
                    return parseResponse(response.body());
                });
    }

    private List<CozeSuggestion> parseResponse(String responseBody) {
        try {
            // 1. Parse the outer response
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            if (jsonResponse.has("data")) {
                // 2. 'data' is a stringified JSON object
                String dataStr = jsonResponse.get("data").getAsString();
                JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();
                
                // 3. Inside 'data', 'output' is a stringified JSON array
                if (dataObj.has("output")) {
                    String outputJsonStr = dataObj.get("output").getAsString();
                    return gson.fromJson(outputJsonStr, new TypeToken<List<CozeSuggestion>>(){}.getType());
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logging.logToError("Failed to parse Coze response: " + e.getMessage());
            logging.logToError("Raw response: " + responseBody);
            throw new RuntimeException("Failed to parse Coze response", e);
        }
    }
}