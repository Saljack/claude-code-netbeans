package org.openbeans.claude.netbeans.terminal;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;

final class ClosableTabHeader extends JPanel {

    private final ClaudeTerminalTopComponent owner;
    private final JTabbedPane tabs;
    private final ClaudeTerminalSession session;

    ClosableTabHeader(ClaudeTerminalTopComponent owner, JTabbedPane tabs, ClaudeTerminalSession session, String label) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        this.owner = owner;
        this.tabs = tabs;
        this.session = session;
        setOpaque(false);

        JLabel labelComp = new JLabel(label);
        add(labelComp);

        JButton close = new JButton("×");
        close.setMargin(new Insets(0, 4, 0, 4));
        close.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.setToolTipText(NbBundle.getMessage(ClosableTabHeader.class, "TT_CloseTab"));
        close.addActionListener(e -> closeThisTab());
        add(close);

        JPopupMenu popup = buildPopupMenu();
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectThisTab();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    selectThisTab();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    selectThisTab();
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        addMouseListener(handler);
        labelComp.addMouseListener(handler);
    }

    private JPopupMenu buildPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(new AbstractAction(
                NbBundle.getMessage(ClosableTabHeader.class, "MENU_DuplicateTab")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectThisTab();
                owner.duplicateCurrent();
            }
        }));
        popup.add(new JMenuItem(new AbstractAction(
                NbBundle.getMessage(ClosableTabHeader.class, "MENU_NewShellHere")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                owner.openShellLike(session);
            }
        }));
        popup.addSeparator();
        popup.add(new JMenuItem(new AbstractAction(
                NbBundle.getMessage(ClosableTabHeader.class, "MENU_CloseTab")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeThisTab();
            }
        }));
        return popup;
    }

    private void selectThisTab() {
        int idx = tabs.indexOfComponent(session);
        if (idx >= 0 && tabs.getSelectedIndex() != idx) {
            tabs.setSelectedIndex(idx);
        }
    }

    private void closeThisTab() {
        owner.closeSession(session);
    }
}
