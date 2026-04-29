package burp.coze;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.coze.api.CozeApiClient;
import burp.coze.ui.MainTab;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final CozeApiClient apiClient;
    private final MainTab mainTab;

    public ContextMenuProvider(MontoyaApi api, CozeApiClient apiClient, MainTab mainTab) {
        this.api = api;
        this.apiClient = apiClient;
        this.mainTab = mainTab;
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
                        mainTab.updateResults(reqRes.request(), suggestions);
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