package org.openbeans.claude.netbeans.terminal;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

@ActionID(category = "Project", id = "org.openbeans.claude.netbeans.terminal.OpenTerminalHere")
@ActionRegistration(displayName = "#CTL_OpenTerminalHere", lazy = false)
@ActionReference(path = "Projects/Actions", position = 1751)
public final class OpenTerminalHereAction extends AbstractAction implements ContextAwareAction {

    private final Lookup context;

    public OpenTerminalHereAction() {
        this(Utilities.actionsGlobalContext());
    }

    private OpenTerminalHereAction(Lookup context) {
        super(NbBundle.getMessage(OpenTerminalHereAction.class, "CTL_OpenTerminalHere"));
        this.context = context;
        Collection<? extends Project> projects = context.lookupAll(Project.class);
        setEnabled(!projects.isEmpty());
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OpenTerminalHereAction(actionContext);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Project project : context.lookupAll(Project.class)) {
            FileObject dir = project.getProjectDirectory();
            if (dir == null) {
                continue;
            }
            File cwd = FileUtil.toFile(dir);
            if (cwd == null || !cwd.isDirectory()) {
                continue;
            }
            String label = ProjectUtils.getInformation(project).getDisplayName();
            ClaudeTerminalTopComponent tc = ClaudeTerminalTopComponent.findInstance();
            if (tc == null) {
                return;
            }
            boolean wasOpened = tc.isOpened();
            tc.openSession(cwd, label, ClaudeTerminalSession.Mode.SHELL);
            if (!wasOpened) {
                tc.open();
            }
            tc.requestActive();
        }
    }
}
