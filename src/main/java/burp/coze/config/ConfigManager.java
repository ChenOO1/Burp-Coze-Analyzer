package burp.coze.config;

import burp.api.montoya.persistence.Preferences;

public class ConfigManager {
    private static final String KEY_API_KEY = "coze.api.key";
    private static final String KEY_WORKFLOW_ID = "coze.workflow.id";
    
    private final Preferences preferences;

    public ConfigManager(Preferences preferences) {
        this.preferences = preferences;
    }

    public String getApiKey() {
        return preferences.getString(KEY_API_KEY) != null ? preferences.getString(KEY_API_KEY) : "";
    }

    public void setApiKey(String apiKey) {
        preferences.setString(KEY_API_KEY, apiKey);
    }

    public String getWorkflowId() {
        return preferences.getString(KEY_WORKFLOW_ID) != null ? preferences.getString(KEY_WORKFLOW_ID) : "";
    }

    public void setWorkflowId(String workflowId) {
        preferences.setString(KEY_WORKFLOW_ID, workflowId);
    }
}