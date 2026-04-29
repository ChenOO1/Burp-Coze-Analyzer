package burp.coze.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.coze.model.CozeSuggestion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AnalysisPanel extends JPanel {
    private final MontoyaApi api;
    private HttpRequest currentRequest;
    private JTextArea requestArea;
    private DefaultTableModel tableModel;
    private JTable suggestionsTable;

    public AnalysisPanel(MontoyaApi api) {
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
            if ("json_body".equalsIgnoreCase(target) || "data_body".equalsIgnoreCase(target) || "body".equalsIgnoreCase(target)) {
                // Both json_body and data_body replace the entire request body.
                // The distinction helps the LLM format the data correctly.
                modifiedRequest = currentRequest.withBody(data);
            } else if ("url_params".equalsIgnoreCase(target)) {
                // Simple replacement for query string. 
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