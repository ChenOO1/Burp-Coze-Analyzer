package burp.coze.ui;

import burp.api.montoya.MontoyaApi;
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