import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.coze.ContextMenuProvider;
import burp.coze.api.CozeApiClient;
import burp.coze.config.ConfigManager;
import burp.coze.ui.MainTab;

public class Extension implements BurpExtension {
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        montoyaApi.extension().setName("Burp Coze Analyzer");

        // Initialize Config
        ConfigManager configManager = new ConfigManager(montoyaApi.persistence().preferences());

        // Initialize API Client
        CozeApiClient apiClient = new CozeApiClient(configManager, montoyaApi.logging());

        // Initialize Main UI Component (which contains both Analysis and Settings)
        MainTab mainTab = new MainTab(montoyaApi, configManager);

        // Register UI (Only one tab now)
        montoyaApi.userInterface().registerSuiteTab("Coze Analyzer", mainTab);

        // Register Context Menu
        ContextMenuProvider contextMenuProvider = new ContextMenuProvider(montoyaApi, apiClient, mainTab);
        montoyaApi.userInterface().registerContextMenuItemsProvider(contextMenuProvider);

        montoyaApi.logging().logToOutput("Burp Coze Analyzer loaded successfully.");
    }
}