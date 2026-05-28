package org.openbeans.claude.netbeans.terminal;

import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.pty4j.PtyProcess;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

public final class ClaudeTerminalSession extends JPanel {

    public enum Mode {
        CLAUDE, SHELL
    }

    private static final Logger LOGGER = Logger.getLogger(ClaudeTerminalSession.class.getName());
    private static final RequestProcessor RP = new RequestProcessor("ClaudeTerminalSession", 4, true);

    private final File cwd;
    private final Mode mode;
    private JediTermWidget widget;
    private PtyProcess process;
    private KeyEventDispatcher shiftEnterDispatcher;

    public ClaudeTerminalSession(File cwd) {
        this(cwd, Mode.CLAUDE);
    }

    public ClaudeTerminalSession(File cwd, Mode mode) {
        super(new BorderLayout());
        this.cwd = cwd;
        this.mode = mode;
    }

    public File getCwd() {
        return cwd;
    }

    public Mode getMode() {
        return mode;
    }

    public void start() {
        startWith(mode);
    }

    private void startWith(Mode launchMode) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> startWith(launchMode));
            return;
        }
        if (widget != null) {
            try {
                widget.close();
            } catch (Exception ignored) {
            }
        }
        removeAll();
        widget = new JediTermWidget(120, 32, new ClaudeTerminalSettings());
        add(widget, BorderLayout.CENTER);
        revalidate();
        repaint();
        final JediTermWidget bound = widget;
        RP.post(() -> {
            try {
                ClaudeProcessLauncher.Launched launched = launchMode == Mode.SHELL
                        ? ClaudeProcessLauncher.launchShell(cwd)
                        : ClaudeProcessLauncher.launch(cwd);
                this.process = launched.process;
                SwingUtilities.invokeLater(() -> {
                    bound.setTtyConnector(launched.connector);
                    bound.start();
                    bound.requestFocusInWindow();
                    installShiftEnterDispatcher(bound);
                });
                launched.process.onExit().thenRun(()
                        -> SwingUtilities.invokeLater(() -> onProcessExited(launchMode)));
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to launch terminal", ex);
                SwingUtilities.invokeLater(() -> showErrorPanel(ex.getMessage()));
            }
        });
    }

    private void installShiftEnterDispatcher(JediTermWidget bound) {
        // Claude Code interprets backslash followed by CR as a multi-line
        // continuation (the same trick its /terminal-setup wires up in
        // iTerm2/Terminal.app). Intercept Shift+Enter via the global
        // KeyboardFocusManager because JediTerm processes Enter at a layer
        // that bypasses Swing's InputMap and KeyListener dispatch.
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (shiftEnterDispatcher != null) {
            kfm.removeKeyEventDispatcher(shiftEnterDispatcher);
        }
        shiftEnterDispatcher = (KeyEvent e) -> {
            if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                return false;
            }
            if (!e.isShiftDown() || e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                return false;
            }
            if (widget == null || e.getComponent() == null) {
                return false;
            }
            if (!SwingUtilities.isDescendingFrom(e.getComponent(), widget)) {
                return false;
            }
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                TtyConnector connector = widget.getTtyConnector();
                if (connector != null) {
                    try {
                        connector.write("\\\r");
                    } catch (IOException ex) {
                        LOGGER.log(Level.FINE, "Failed to send Shift+Enter sequence", ex);
                    }
                }
            }
            // Consume all three event ids (PRESSED, RELEASED, TYPED) for the
            // Shift+Enter combo so JediTerm's default Enter handling does not
            // also fire.
            return true;
        };
        kfm.addKeyEventDispatcher(shiftEnterDispatcher);
    }

    private void uninstallShiftEnterDispatcher() {
        if (shiftEnterDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .removeKeyEventDispatcher(shiftEnterDispatcher);
            shiftEnterDispatcher = null;
        }
    }

    private void onProcessExited(Mode exitedMode) {
        if (exitedMode == Mode.CLAUDE) {
            // Claude exited (e.g. user pressed double-Esc): drop into a shell at
            // the same cwd instead of showing a restart panel.
            startWith(Mode.SHELL);
        } else {
            showRestartPanel();
        }
    }

    public void dispose() {
        uninstallShiftEnterDispatcher();
        if (process != null && process.isAlive()) {
            try {
                process.destroy();
            } catch (Exception ignored) {
            }
        }
        process = null;
        if (widget != null) {
            try {
                widget.close();
            } catch (Exception ignored) {
            }
            widget = null;
        }
        removeAll();
    }

    private void showRestartPanel() {
        if (process != null && process.isAlive()) {
            return;
        }
        removeAll();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        String exitedKey = mode == Mode.SHELL ? "LBL_ShellExited" : "LBL_ClaudeExited";
        panel.add(new JLabel(NbBundle.getMessage(ClaudeTerminalSession.class, exitedKey)));
        JButton restart = new JButton(NbBundle.getMessage(ClaudeTerminalSession.class, "LBL_RestartClaude"));
        restart.addActionListener((ActionEvent e) -> start());
        panel.add(restart);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showErrorPanel(String message) {
        removeAll();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        String key = mode == Mode.SHELL ? "LBL_ShellLaunchFailed" : "LBL_LaunchFailed";
        panel.add(new JLabel(NbBundle.getMessage(ClaudeTerminalSession.class, key, message == null ? "" : message)));
        JButton retry = new JButton(NbBundle.getMessage(ClaudeTerminalSession.class, "LBL_RestartClaude"));
        retry.addActionListener((ActionEvent e) -> start());
        panel.add(retry);
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
