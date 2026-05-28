package org.openbeans.claude.netbeans.terminal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Tools", id = "org.openbeans.claude.netbeans.terminal.OpenTerminal")
@ActionRegistration(displayName = "#CTL_OpenTerminal")
@ActionReference(path = "Menu/Tools", position = 1260)
public final class OpenTerminalAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ClaudeTerminalTopComponent tc = ClaudeTerminalTopComponent.findInstance();
        if (tc == null) {
            return;
        }
        boolean wasOpened = tc.isOpened();
        tc.openSession(ClaudeProcessLauncher.resolveCwd(), null, ClaudeTerminalSession.Mode.SHELL);
        if (!wasOpened) {
            tc.open();
        }
        tc.requestActive();
    }
}
