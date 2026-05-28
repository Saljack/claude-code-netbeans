package org.openbeans.claude.netbeans.terminal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@TopComponent.Description(
        preferredID = ClaudeTerminalTopComponent.PREFERRED_ID,
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Tools", id = "org.openbeans.claude.netbeans.terminal.OpenClaudeTerminal")
@ActionReference(path = "Menu/Tools", position = 1250)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_OpenClaudeTerminal",
        preferredID = ClaudeTerminalTopComponent.PREFERRED_ID
)
public final class ClaudeTerminalTopComponent extends TopComponent {

    public static final String PREFERRED_ID = "ClaudeTerminalTC";

    private final JTabbedPane tabs = new JTabbedPane();
    private JPanel pseudoTabPlaceholder;

    public ClaudeTerminalTopComponent() {
        setLayout(new BorderLayout());
        setName(NbBundle.getMessage(ClaudeTerminalTopComponent.class, "CTL_ClaudeTerminalTC"));
        setToolTipText(NbBundle.getMessage(ClaudeTerminalTopComponent.class, "HINT_ClaudeTerminalTC"));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addChangeListener(e -> {
            int sel = tabs.getSelectedIndex();
            if (sel < 0 || pseudoTabPlaceholder == null) {
                return;
            }
            // Never let the "+" pseudo-tab become the selected tab: revert to the
            // last real tab.
            if (tabs.getComponentAt(sel) == pseudoTabPlaceholder) {
                int prev = sel - 1;
                tabs.setSelectedIndex(prev >= 0 ? prev : -1);
            }
        });
        add(tabs, BorderLayout.CENTER);
    }

    private void ensurePseudoTab() {
        if (pseudoTabPlaceholder != null && tabs.indexOfComponent(pseudoTabPlaceholder) >= 0) {
            return;
        }
        pseudoTabPlaceholder = new JPanel();
        int idx = tabs.getTabCount();
        tabs.addTab("", pseudoTabPlaceholder);
        tabs.setTabComponentAt(idx, buildNewTabButton());
    }

    private int pseudoTabIndex() {
        return pseudoTabPlaceholder != null ? tabs.indexOfComponent(pseudoTabPlaceholder) : -1;
    }

    private int realTabCount() {
        int n = tabs.getTabCount();
        return pseudoTabIndex() >= 0 ? n - 1 : n;
    }

    private JButton buildNewTabButton() {
        JButton newTab = new JButton(new GreenPlusIcon(12));
        newTab.setMargin(new Insets(0, 6, 0, 6));
        newTab.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        newTab.setFocusable(false);
        newTab.setContentAreaFilled(false);
        newTab.setToolTipText(NbBundle.getMessage(ClaudeTerminalTopComponent.class, "TT_NewClaudeTab"));
        newTab.addActionListener(e -> duplicateCurrent());
        return newTab;
    }

    /**
     * Opens a new tab using the currently selected tab's cwd and mode (claude
     * stays claude, shell stays shell). If no tab is selected, opens a default
     * claude tab.
     */
    void duplicateCurrent() {
        Component sel = tabs.getSelectedComponent();
        if (sel instanceof ClaudeTerminalSession session) {
            openSession(session.getCwd(), null, session.getMode());
        } else {
            openSession(ClaudeProcessLauncher.resolveCwd(), null, ClaudeTerminalSession.Mode.CLAUDE);
        }
    }

    /**
     * Opens a new shell tab using the given session's cwd. Used by the tab
     * popup menu.
     */
    void openShellLike(ClaudeTerminalSession session) {
        openSession(session.getCwd(), null, ClaudeTerminalSession.Mode.SHELL);
    }

    /**
     * Closes the tab hosting the given session and disposes its child process.
     */
    void closeSession(ClaudeTerminalSession session) {
        int idx = tabs.indexOfComponent(session);
        if (idx >= 0) {
            tabs.remove(idx);
            session.dispose();
        }
    }

    public static ClaudeTerminalTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win instanceof ClaudeTerminalTopComponent) {
            return (ClaudeTerminalTopComponent) win;
        }
        return null;
    }

    @Override
    protected void componentOpened() {
        ensurePseudoTab();
        if (realTabCount() == 0) {
            openSession(ClaudeProcessLauncher.resolveCwd(), null);
        }
    }

    @Override
    protected void componentClosed() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component c = tabs.getComponentAt(i);
            if (c instanceof ClaudeTerminalSession session) {
                session.dispose();
            }
        }
        tabs.removeAll();
        pseudoTabPlaceholder = null;
    }

    /**
     * Adds a new tab running claude in the given cwd. If label is null, uses
     * the cwd's basename. Selects the newly added tab.
     */
    public void openSession(File cwd, String label) {
        openSession(cwd, label, ClaudeTerminalSession.Mode.CLAUDE);
    }

    /**
     * Adds a new tab running either claude or the user's default shell in the given cwd.
     */
    public void openSession(File cwd, String label, ClaudeTerminalSession.Mode mode) {
        if (cwd == null) {
            cwd = ClaudeProcessLauncher.resolveCwd();
        }
        String title = label != null && !label.isEmpty() ? label : safeName(cwd);
        if (mode == ClaudeTerminalSession.Mode.SHELL) {
            title = title + " (sh)";
        }
        ClaudeTerminalSession session = new ClaudeTerminalSession(cwd, mode);
        ensurePseudoTab();
        int pseudoIdx = pseudoTabIndex();
        int idx = pseudoIdx >= 0 ? pseudoIdx : tabs.getTabCount();
        tabs.insertTab(title, null, session, null, idx);
        tabs.setTabComponentAt(idx, new ClosableTabHeader(this, tabs, session, title));
        tabs.setSelectedIndex(idx);
        session.start();
    }

    private static String safeName(File cwd) {
        String name = cwd.getName();
        return name.isEmpty() ? "claude" : name;
    }
}
