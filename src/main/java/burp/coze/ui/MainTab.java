package burp.coze.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.coze.config.ConfigManager;
import burp.coze.model.CozeSuggestion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MainTab extends JPanel {
    private final MontoyaApi api;
    private HttpRequest currentRequest;
    private List<CozeSuggestion> currentSuggestions;

    // UI Components
    private JTextArea originalRequestArea;
    private JTextArea modifiedRequestArea;
    private JTable suggestionsTable;
    private DefaultTableModel tableModel;
    private JTextArea messageDetailArea;

    public MainTab(MontoyaApi api, ConfigManager configManager) {
        this.api = api;
        setLayout(new BorderLayout());

        // ==========================================
        // LEFT PANEL (Original Request / Modified Request)
        // ==========================================
        
        // Top Left: Original Request
        originalRequestArea = new JTextArea();
        originalRequestArea.setEditable(false);
        JScrollPane originalScroll = new JScrollPane(originalRequestArea);
        originalScroll.setBorder(BorderFactory.createTitledBorder("Original Request"));

        // Bottom Left: Modified Request Preview
        modifiedRequestArea = new JTextArea();
        modifiedRequestArea.setEditable(false);
        JScrollPane modifiedScroll = new JScrollPane(modifiedRequestArea);
        modifiedScroll.setBorder(BorderFactory.createTitledBorder("Modified Request Preview"));

        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, originalScroll, modifiedScroll);
        leftSplitPane.setResizeWeight(0.5);

        // ==========================================
        // RIGHT PANEL (Settings / Table / Message Details)
        // ==========================================
        
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Top Right: Settings
        SettingsPanel settingsPanel = new SettingsPanel(api, configManager);
        rightPanel.add(settingsPanel, BorderLayout.NORTH);

        // Center Right: Suggestions Table
        String[] columnNames = {"Target", "Message", "Data", "Action"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Action column is editable
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

        // Add selection listener to update details and preview
        suggestionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = suggestionsTable.getSelectedRow();
                if (selectedRow >= 0 && currentSuggestions != null) {
                    CozeSuggestion suggestion = currentSuggestions.get(selectedRow);
                    // Update Message Details
                    messageDetailArea.setText(suggestion.getMessage() + "\n\n[Data]:\n" + suggestion.getData());
                    // Update Modified Request Preview
                    updateModifiedPreview(suggestion);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(suggestionsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Coze Suggestions"));

        // Bottom Right: Message Details
        messageDetailArea = new JTextArea();
        messageDetailArea.setEditable(false);
        messageDetailArea.setLineWrap(true);
        messageDetailArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageDetailArea);
        messageScroll.setBorder(BorderFactory.createTitledBorder("Message Details"));

        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, messageScroll);
        rightSplitPane.setResizeWeight(0.7);
        
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);

        // ==========================================
        // MAIN SPLIT PANE
        // ==========================================
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, rightPanel);
        mainSplitPane.setResizeWeight(0.5); // 50/50 split
        mainSplitPane.setOneTouchExpandable(true);

        add(mainSplitPane, BorderLayout.CENTER);
    }

    public void updateResults(HttpRequest request, List<CozeSuggestion> suggestions) {
        this.currentRequest = request;
        this.currentSuggestions = suggestions;
        
        SwingUtilities.invokeLater(() -> {
            originalRequestArea.setText(request.toString());
            modifiedRequestArea.setText("");
            messageDetailArea.setText("");
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

    private void updateModifiedPreview(CozeSuggestion suggestion) {
        if (currentRequest == null) return;
        
        try {
            HttpRequest previewRequest = applyModification(currentRequest, suggestion.getTarget(), suggestion.getData());
            modifiedRequestArea.setText(previewRequest.toString());
        } catch (Exception ex) {
            modifiedRequestArea.setText("Error generating preview: " + ex.getMessage());
        }
    }

    private void applySuggestionToRepeater(int row) {
        if (currentRequest == null || currentSuggestions == null) return;

        CozeSuggestion suggestion = currentSuggestions.get(row);

        try {
            HttpRequest modifiedRequest = applyModification(currentRequest, suggestion.getTarget(), suggestion.getData());
            api.repeater().sendToRepeater(modifiedRequest, "Coze: " + suggestion.getTarget());
            JOptionPane.showMessageDialog(this, "Sent to Repeater successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            api.logging().logToError("Failed to apply suggestion: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error applying suggestion: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private HttpRequest applyModification(HttpRequest request, String target, String data) {
        if ("json_body".equalsIgnoreCase(target) || "data_body".equalsIgnoreCase(target) || "body".equalsIgnoreCase(target)) {
            return request.withBody(data);
        } else if ("url_params".equalsIgnoreCase(target)) {
            String path = request.path();
            int qIndex = path.indexOf('?');
            String newPath = (qIndex > -1 ? path.substring(0, qIndex) : path) + "?" + data;
            return request.withPath(newPath);
        } else {
            throw new IllegalArgumentException("Unknown target: " + target);
        }
    }
}