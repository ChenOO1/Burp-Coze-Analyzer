# Burp-Coze-Analyzer Design Specification

## 1. Overview
Burp-Coze-Analyzer is a Burp Suite extension built using the Montoya API. It integrates with the Coze Workflow API to analyze HTTP requests for potential security vulnerabilities (like IDOR, logic flaws) and provides actionable, one-click modifications that can be sent directly to Burp's Repeater for testing.

## 2. Architecture & Components

### 2.1 Settings Management
- **Component**: `CozeSettingsPanel`
- **Functionality**: Provides a UI in Burp's Settings tab for users to configure:
  - Coze API Key (Bearer token)
  - Coze Workflow ID
- **Storage**: Uses `montoyaApi.persistence().preferences()` to save settings across Burp restarts.

### 2.2 Context Menu Integration
- **Component**: `CozeContextMenuProvider`
- **Functionality**: Adds a "Send to Coze Analyzer" option when right-clicking on HTTP requests in Proxy or Repeater.
- **Action**: Extracts the selected HTTP request and triggers the analysis workflow.

### 2.3 Coze API Client
- **Component**: `CozeApiClient`
- **Functionality**: 
  - Constructs the JSON payload required by the Coze Workflow API (`POST /v1/workflow/run`).
  - Sends the HTTP request details (URL, Headers, Body) to the workflow.
  - Parses the JSON response from Coze.

### 2.4 Analysis Results UI
- **Component**: `CozeAnalysisTab`
- **Functionality**: A custom Suite Tab registered with Burp.
- **Layout**:
  - **Top Section**: Displays the original request being analyzed.
  - **Bottom Section**: A table displaying the modification suggestions returned by Coze.
    - Columns: `Message` (Description), `Target` (body/url_params/headers), `Data` (The modified payload), `Action` (Apply button).

### 2.5 Request Modification & Repeater Integration
- **Component**: `RepeaterSender`
- **Functionality**: 
  - Triggered when the user clicks "Apply to Repeater" in the Analysis Tab.
  - Reads the `target` field from the Coze JSON response.
  - Modifies the original HTTP request based on the `target`:
    - `body`: Replaces the entire request body with the `data` field.
    - `url_params`: Replaces the URL query string with the `data` field.
    - `headers`: (Future proofing) Replaces specific headers.
  - Sends the newly constructed request to Burp Repeater using `montoyaApi.repeater().sendToRepeater()`.

## 3. Data Flow
1. User right-clicks request -> "Send to Coze Analyzer".
2. `CozeContextMenuProvider` captures `HttpRequest`.
3. `CozeApiClient` sends request data to Coze Workflow.
4. Coze Workflow returns JSON array of suggestions:
   ```json
   [
     {
       "target": "body",
       "data": "readDetail=...",
       "message": "修改建议1：..."
     }
   ]
   ```
5. `CozeAnalysisTab` updates its table with the parsed JSON.
6. User clicks "Apply" on a specific row.
7. `RepeaterSender` modifies the original `HttpRequest` based on `target` and `data`.
8. Modified request appears in a new Repeater tab.

## 4. Dependencies
- `com.google.code.gson:gson` (for JSON parsing and serialization)
- Java 21 `HttpClient` (for making requests to Coze API)

## 5. Error Handling
- Missing API Key/Workflow ID: Prompt user to configure settings.
- Coze API Errors (Network/Auth): Display error message in the Analysis Tab or Burp's Event Log.
- JSON Parsing Errors: Log to Burp's error output.
