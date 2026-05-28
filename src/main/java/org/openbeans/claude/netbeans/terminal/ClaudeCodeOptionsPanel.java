package org.openbeans.claude.netbeans.terminal;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle;

final class ClaudeCodeOptionsPanel extends JPanel {

    private final ClaudeCodeOptionsPanelController controller;
    private JComboBox<String> themeCombo;
    private JLabel fontLabel;
    private JButton fontButton;
    private JTextField cliPathField;
    private JButton cliPathBrowse;

    private String currentFontFamily;
    private int currentFontSize;

    ClaudeCodeOptionsPanel(ClaudeCodeOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new GridBagLayout());

        themeCombo = new JComboBox<>(new String[]{
            ClaudeTerminalSettings.THEME_DARK,
            ClaudeTerminalSettings.THEME_LIGHT
        });
        themeCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (ClaudeTerminalSettings.THEME_DARK.equals(value)) {
                    setText(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsValue_ThemeDark"));
                } else if (ClaudeTerminalSettings.THEME_LIGHT.equals(value)) {
                    setText(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsValue_ThemeLight"));
                }
                return this;
            }
        });
        themeCombo.addActionListener(e -> controller.markChanged());

        fontLabel = new JLabel();
        fontButton = new JButton(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsLabel_ChooseFont"));
        fontButton.addActionListener(e -> openFontChooser());

        cliPathField = new JTextField();
        cliPathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { controller.markChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { controller.markChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { controller.markChanged(); }
        });
        cliPathBrowse = new JButton(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsLabel_Browse"));
        cliPathBrowse.addActionListener(e -> browseForCli());

        addRow(0, "OptionsLabel_Theme", themeCombo);

        // Font label + Choose button on the same row
        JPanel fontRow = new JPanel(new GridBagLayout());
        GridBagConstraints fl = new GridBagConstraints();
        fl.gridx = 0;
        fl.fill = GridBagConstraints.HORIZONTAL;
        fl.weightx = 1;
        fl.insets = new Insets(0, 0, 0, 8);
        fontRow.add(fontLabel, fl);
        GridBagConstraints fb = new GridBagConstraints();
        fb.gridx = 1;
        fb.fill = GridBagConstraints.NONE;
        fb.anchor = GridBagConstraints.EAST;
        fontRow.add(fontButton, fb);
        addRow(1, "OptionsLabel_Font", fontRow);

        // CLI path field + browse button
        JPanel cliRow = new JPanel(new GridBagLayout());
        GridBagConstraints cf = new GridBagConstraints();
        cf.gridx = 0;
        cf.fill = GridBagConstraints.HORIZONTAL;
        cf.weightx = 1;
        cf.insets = new Insets(0, 0, 0, 8);
        cliRow.add(cliPathField, cf);
        GridBagConstraints cb = new GridBagConstraints();
        cb.gridx = 1;
        cb.fill = GridBagConstraints.NONE;
        cb.anchor = GridBagConstraints.EAST;
        cliRow.add(cliPathBrowse, cb);
        addRow(2, "OptionsLabel_CliPath", cliRow);

        GridBagConstraints note = new GridBagConstraints();
        note.gridx = 0;
        note.gridy = 3;
        note.gridwidth = 2;
        note.anchor = GridBagConstraints.WEST;
        note.insets = new Insets(12, 4, 4, 4);
        add(new JLabel(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsLabel_Note")), note);

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 4;
        filler.gridwidth = 2;
        filler.weightx = 1;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.BOTH;
        add(new JPanel(), filler);
    }

    private void browseForCli() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "OptionsLabel_CliBrowseTitle"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String existing = cliPathField.getText();
        if (existing != null && !existing.isEmpty()) {
            File f = new File(existing);
            if (f.exists()) {
                chooser.setSelectedFile(f);
            }
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            cliPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openFontChooser() {
        PropertyEditor editor = PropertyEditorManager.findEditor(Font.class);
        if (editor == null || !editor.supportsCustomEditor()) {
            return;
        }
        editor.setValue(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
        Component custom = editor.getCustomEditor();
        DialogDescriptor dd = new DialogDescriptor(
                custom,
                NbBundle.getMessage(ClaudeCodeOptionsPanel.class, "FontChooser_Title"));
        Object result = DialogDisplayer.getDefault().notify(dd);
        if (result == DialogDescriptor.OK_OPTION) {
            Object value = editor.getValue();
            if (value instanceof Font) {
                Font chosen = (Font) value;
                currentFontFamily = chosen.getFamily();
                currentFontSize = chosen.getSize();
                updateFontLabel();
                controller.markChanged();
            }
        }
    }

    private void updateFontLabel() {
        fontLabel.setText(currentFontFamily + ", " + currentFontSize + " pt");
        fontLabel.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
    }

    private void addRow(int row, String labelKey, JComponent field) {
        GridBagConstraints labelC = new GridBagConstraints();
        labelC.gridx = 0;
        labelC.gridy = row;
        labelC.anchor = GridBagConstraints.WEST;
        labelC.insets = new Insets(4, 4, 4, 8);
        add(new JLabel(NbBundle.getMessage(ClaudeCodeOptionsPanel.class, labelKey)), labelC);

        GridBagConstraints fieldC = new GridBagConstraints();
        fieldC.gridx = 1;
        fieldC.gridy = row;
        fieldC.fill = GridBagConstraints.HORIZONTAL;
        fieldC.weightx = 1;
        fieldC.insets = new Insets(4, 4, 4, 4);
        add(field, fieldC);
    }

    void load() {
        themeCombo.setSelectedItem(ClaudeTerminalSettings.getTheme());
        currentFontFamily = ClaudeTerminalSettings.getFontFamily();
        currentFontSize = ClaudeTerminalSettings.getFontSize();
        updateFontLabel();
        cliPathField.setText(ClaudeTerminalSettings.getCliPath());
    }

    void store() {
        Preferences p = ClaudeTerminalSettings.prefs();
        Object theme = themeCombo.getSelectedItem();
        if (theme != null) {
            p.put(ClaudeTerminalSettings.KEY_THEME, theme.toString());
        }
        if (currentFontFamily != null && !currentFontFamily.isEmpty()) {
            p.put(ClaudeTerminalSettings.KEY_FONT_FAMILY, currentFontFamily);
        }
        if (currentFontSize > 0) {
            p.putInt(ClaudeTerminalSettings.KEY_FONT_SIZE, currentFontSize);
        }
        String cli = cliPathField.getText();
        if (cli == null) {
            cli = "";
        }
        p.put(ClaudeTerminalSettings.KEY_CLI_PATH, cli.trim());
    }

    boolean valid() {
        return currentFontFamily != null && currentFontSize > 0;
    }
}
