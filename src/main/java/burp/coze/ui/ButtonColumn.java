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