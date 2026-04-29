# Burp-Coze-Analyzer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Burp Suite extension that sends HTTP requests to Coze Workflow API for security analysis and provides a UI to apply suggested modifications directly to Burp Repeater.

**Architecture:** The extension uses Montoya API for UI integration (Settings Panel, Context Menu, Custom Tab) and Repeater interaction. It uses Java's built-in `HttpClient` for API calls and `Gson` for JSON parsing. The architecture is modularized into UI components, API client, and data models.

**Tech Stack:** Java 21, Burp Montoya API, Gson, Java HttpClient.

---

### Task 1: Project Setup and Dependencies

**Files:**
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Update project name**

```kotlin
// settings.gradle.kts
rootProject.name = "burp-coze-analyzer"
```

- [ ] **Step 2: Add Gson dependency**

```kotlin
// build.gradle.kts
dependencies {
    implementation("net.portswigger.burp.extensions:montoya-api:2023.12.1")
    implementation("com.google.code.gson:gson:2.10.1")
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew build` (or `gradlew build` on Windows)
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts settings.gradle.kts
git commit -m "chore: setup project name and add gson dependency"
```

---

### Task 2: Data Models

**Files:**
- Create: `src/main/java/burp/coze/model/CozeSuggestion.java`

- [ ] **Step 1: Create CozeSuggestion model**

```java
package burp.coze.model;

public class CozeSuggestion {
    private String target;
    private String data;
    private String message;

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/burp/coze/model/CozeSuggestion.java
git commit -m "feat: add CozeSuggestion data model"
```

---

### Task 3: Settings Management

**Files:**
- Create: `src/main/java/burp/coze/config/ConfigManager.java`
- Create: `src/main/java/burp/coze/ui/SettingsPanel.java`

- [ ] **Step 1: Create ConfigManager**

```java
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
```

- [ ] **Step 2: Create SettingsPanel**

```java
package burp.coze.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.swing.SwingUtils;
import burp.coze.config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private final ConfigManager configManager;
    private JTextField apiKeyField;
    private JTextField workflowIdField;

    public SettingsPanel(MontoyaApi api, ConfigManager configManager) {
        this.configManager = configManager;
        setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // API Key
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Coze API Key (Bearer Token):"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        apiKeyField = new JTextField(configManager.getApiKey(), 40);
        formPanel.add(apiKeyField, gbc);

        // Workflow ID
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Coze Workflow ID:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        workflowIdField = new JTextField(configManager.getWorkflowId(), 40);
        formPanel.add(workflowIdField, gbc);

        // Save Button
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveSettings(api));
        formPanel.add(saveButton, gbc);

        add(formPanel, BorderLayout.NORTH);
    }

    private void saveSettings(MontoyaApi api) {
        configManager.setApiKey(apiKeyField.getText().trim());
        configManager.setWorkflowId(workflowIdField.getText().trim());
        JOptionPane.showMessageDialog(this, "Settings saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/burp/coze/config/ConfigManager.java src/main/java/burp/coze/ui/SettingsPanel.java
git commit -m "feat: add configuration manager and settings UI"
```

---

### Task 4: Coze API Client

**Files:**
- Create: `src/main/java/burp/coze/api/CozeApiClient.java`

- [ ] **Step 1: Create CozeApiClient**

```java
package burp.coze.api;

import burp.api.montoya.logging.Logging;
import burp.coze.config.ConfigManager;
import burp.coze.model.CozeSuggestion;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/burp/coze/api/CozeApiClient.java
git commit -m "feat: add Coze API client for workflow execution"
```

---

### Task 5: Analysis UI and Repeater Integration

**Files:**
- Create: `src/main/java/burp/coze/ui/AnalysisTab.java`

- [ ] **Step 1: Create AnalysisTab**

```java
package burp.coze.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.coze.model.CozeSuggestion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AnalysisTab extends JPanel {
    private final MontoyaApi api;
    private HttpRequest currentRequest;
    private JTextArea requestArea;
    private DefaultTableModel tableModel;
    private JTable suggestionsTable;

    public AnalysisTab(MontoyaApi api) {
        this.api = api;
        setLayout(new BorderLayout());

        // Top: Original Request
        requestArea = new JTextArea();
        requestArea.setEditable(false);
        JScrollPane requestScroll = new JScrollPane(requestArea);
        requestScroll.setBorder(BorderFactory.createTitledBorder("Original Request"));
        requestScroll.setPreferredSize(new Dimension(800, 300));

        // Bottom: Suggestions Table
        String[] columnNames = {"Target", "Message", "Data", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Action column is editable (for button)
            }
        };
        suggestionsTable = new JTable(tableModel);
        
        // Setup Button Column
        Action applyAction = new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int modelRow = Integer.valueOf(e.getActionCommand());
                applySuggestionToRepeater(modelRow);
            }
        };
        new ButtonColumn(suggestionsTable, applyAction, 3);

        JScrollPane tableScroll = new JScrollPane(suggestionsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Coze Suggestions"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestScroll, tableScroll);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);
    }

    public void updateResults(HttpRequest request, List<CozeSuggestion> suggestions) {
        this.currentRequest = request;
        SwingUtilities.invokeLater(() -> {
            requestArea.setText(request.toString());
            tableModel.setRowCount(0); // Clear existing
            for (CozeSuggestion suggestion : suggestions) {
                tableModel.addRow(new Object[]{
                        suggestion.getTarget(),
                        suggestion.getMessage(),
                        suggestion.getData(),
                        "Apply to Repeater"
                });
            }
        });
    }

    private void applySuggestionToRepeater(int row) {
        if (currentRequest == null) return;

        String target = (String) tableModel.getValueAt(row, 0);
        String data = (String) tableModel.getValueAt(row, 2);

        HttpRequest modifiedRequest = currentRequest;

        try {
            if ("body".equalsIgnoreCase(target)) {
                modifiedRequest = currentRequest.withBody(data);
            } else if ("url_params".equalsIgnoreCase(target)) {
                // Simple replacement for query string. 
                // Note: A more robust implementation would parse and replace specific parameters.
                String path = currentRequest.path();
                int qIndex = path.indexOf('?');
                String newPath = (qIndex > -1 ? path.substring(0, qIndex) : path) + "?" + data;
                modifiedRequest = currentRequest.withPath(newPath);
            } else {
                api.logging().logToError("Unknown target: " + target);
                return;
            }

            api.repeater().sendToRepeater(modifiedRequest, "Coze: " + target);
            JOptionPane.showMessageDialog(this, "Sent to Repeater successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            api.logging().logToError("Failed to apply suggestion: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error applying suggestion: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
```

- [ ] **Step 2: Create ButtonColumn utility**

Create `src/main/java/burp/coze/ui/ButtonColumn.java`

```java
package burp.coze.ui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    private JTable table;
    private Action action;
    private JButton renderButton;
    private JButton editButton;
    private Object editorValue;
    private boolean isButtonColumnEditor;

    public ButtonColumn(JTable table, Action action, int column) {
        this.table = table;
        this.action = action;

        renderButton = new JButton();
        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value == null) {
            renderButton.setText("");
        } else {
            renderButton.setText(value.toString());
        }
        return renderButton;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (value == null) {
            editButton.setText("");
        } else {
            editButton.setText(value.toString());
        }
        this.editorValue = value;
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return editorValue;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();
        ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row);
        action.actionPerformed(event);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/burp/coze/ui/AnalysisTab.java src/main/java/burp/coze/ui/ButtonColumn.java
git commit -m "feat: add analysis tab and repeater integration"
```

---

### Task 6: Context Menu Integration

**Files:**
- Create: `src/main/java/burp/coze/ContextMenuProvider.java`

- [ ] **Step 1: Create ContextMenuProvider**

```java
package burp.coze;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.coze.api.CozeApiClient;
import burp.coze.ui.AnalysisTab;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final CozeApiClient apiClient;
    private final AnalysisTab analysisTab;

    public ContextMenuProvider(MontoyaApi api, CozeApiClient apiClient, AnalysisTab analysisTab) {
        this.api = api;
        this.apiClient = apiClient;
        this.analysisTab = analysisTab;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER)) {
            List<HttpRequestResponse> requestResponses = event.messageEditorRequestResponse().isPresent() ?
                    List.of(event.messageEditorRequestResponse().get().requestResponse()) :
                    event.selectedRequestResponses();

            if (!requestResponses.isEmpty()) {
                JMenuItem analyzeItem = new JMenuItem("Send to Coze Analyzer");
                analyzeItem.addActionListener(e -> {
                    HttpRequestResponse reqRes = requestResponses.get(0);
                    String requestContent = reqRes.request().toString();
                    
                    api.logging().logToOutput("Sending request to Coze...");
                    
                    apiClient.analyzeRequest(requestContent).thenAccept(suggestions -> {
                        api.logging().logToOutput("Received " + suggestions.size() + " suggestions from Coze.");
                        analysisTab.updateResults(reqRes.request(), suggestions);
                        // Optional: Switch to the Analysis tab programmatically if Montoya API supports it
                    }).exceptionally(ex -> {
                        api.logging().logToError("Error during analysis: " + ex.getMessage());
                        return null;
                    });
                });
                menuItems.add(analyzeItem);
            }
        }
        return menuItems;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/burp/coze/ContextMenuProvider.java
git commit -m "feat: add context menu provider"
```

---

### Task 7: Extension Initialization

**Files:**
- Modify: `src/main/java/Extension.java`

- [ ] **Step 1: Update Extension.java**

```java
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.coze.ContextMenuProvider;
import burp.coze.api.CozeApiClient;
import burp.coze.config.ConfigManager;
import burp.coze.ui.AnalysisTab;
import burp.coze.ui.SettingsPanel;

public class Extension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        montoyaApi.extension().setName("Burp Coze Analyzer");

        // Initialize Config
        ConfigManager configManager = new ConfigManager(montoyaApi.persistence().preferences());

        // Initialize API Client
        CozeApiClient apiClient = new CozeApiClient(configManager, montoyaApi.logging());

        // Initialize UI Components
        SettingsPanel settingsPanel = new SettingsPanel(montoyaApi, configManager);
        AnalysisTab analysisTab = new AnalysisTab(montoyaApi);

        // Register UI
        montoyaApi.userInterface().registerSuiteTab("Coze Settings", settingsPanel);
        montoyaApi.userInterface().registerSuiteTab("Coze Analysis", analysisTab);

        // Register Context Menu
        ContextMenuProvider contextMenuProvider = new ContextMenuProvider(montoyaApi, apiClient, analysisTab);
        montoyaApi.userInterface().registerContextMenuItemsProvider(contextMenuProvider);

        montoyaApi.logging().logToOutput("Burp Coze Analyzer loaded successfully.");
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew jar` (or `gradlew jar` on Windows)
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/Extension.java
git commit -m "feat: wire up all components in Extension initialization"
```
