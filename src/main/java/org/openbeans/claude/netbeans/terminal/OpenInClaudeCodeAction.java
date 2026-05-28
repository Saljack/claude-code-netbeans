package org.openbeans.claude.netbeans.terminal;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

@ActionID(category = "Project", id = "org.openbeans.claude.netbeans.terminal.OpenInClaudeCode")
@ActionRegistration(displayName = "#CTL_OpenInClaudeCode", lazy = false)
@ActionReferences({
    @ActionReference(path = "Projects/Actions", position = 1750),
    @ActionReference(path = "Shortcuts", name = "DAS-K")
})
public final class OpenInClaudeCodeAction extends AbstractAction implements ContextAwareAction {

    private final Lookup context;

    public OpenInClaudeCodeAction() {
        this(Utilities.actionsGlobalContext());
    }

    private OpenInClaudeCodeAction(Lookup context) {
        super(NbBundle.getMessage(OpenInClaudeCodeAction.class, "CTL_OpenInClaudeCode"));
        this.context = context;
        // Always enabled: the action figures out the target project at invocation
        // time, falling back to the project owning the currently active file when
        // no project is selected. This is what makes the keyboard shortcut work
        // regardless of focus.
        setEnabled(true);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new OpenInClaudeCodeAction(actionContext);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<? extends Project> projects = context.lookupAll(Project.class);
        if (!projects.isEmpty()) {
            for (Project p : projects) {
                openProject(p);
            }
            return;
        }
        Project fallback = findProjectOfActiveFile();
        if (fallback != null) {
            openProject(fallback);
        } else {
            StatusDisplayer.getDefault().setStatusText(
                    NbBundle.getMessage(OpenInClaudeCodeAction.class, "MSG_NoProjectForClaude"));
        }
    }

    private void openProject(Project project) {
        FileObject dir = project.getProjectDirectory();
        if (dir == null) {
            return;
        }
        File cwd = FileUtil.toFile(dir);
        if (cwd == null || !cwd.isDirectory()) {
            return;
        }
        String label = ProjectUtils.getInformation(project).getDisplayName();
        openTab(cwd, label);
    }

    private Project findProjectOfActiveFile() {
        for (DataObject dobj : context.lookupAll(DataObject.class)) {
            Project owner = FileOwnerQuery.getOwner(dobj.getPrimaryFile());
            if (owner != null) {
                return owner;
            }
        }
        for (FileObject fo : context.lookupAll(FileObject.class)) {
            Project owner = FileOwnerQuery.getOwner(fo);
            if (owner != null) {
                return owner;
            }
        }
        Lookup global = Utilities.actionsGlobalContext();
        for (DataObject dobj : global.lookupAll(DataObject.class)) {
            Project owner = FileOwnerQuery.getOwner(dobj.getPrimaryFile());
            if (owner != null) {
                return owner;
            }
        }
        TopComponent active = TopComponent.getRegistry().getActivated();
        if (active != null) {
            DataObject dobj = active.getLookup().lookup(DataObject.class);
            if (dobj != null) {
                Project owner = FileOwnerQuery.getOwner(dobj.getPrimaryFile());
                if (owner != null) {
                    return owner;
                }
            }
            FileObject fo = active.getLookup().lookup(FileObject.class);
            if (fo != null) {
                return FileOwnerQuery.getOwner(fo);
            }
        }
        return null;
    }

    private static void openTab(File cwd, String label) {
        ClaudeTerminalTopComponent tc = ClaudeTerminalTopComponent.findInstance();
        if (tc == null) {
            return;
        }
        boolean wasOpened = tc.isOpened();
        tc.openSession(cwd, label);
        if (!wasOpened) {
            tc.open();
        }
        tc.requestActive();
    }
}
