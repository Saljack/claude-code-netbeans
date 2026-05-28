package org.openbeans.claude.netbeans.terminal;

import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.core.util.TermSize;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.openbeans.claude.netbeans.ClaudeCodeStatusService;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

public final class ClaudeProcessLauncher {

    private static final Logger LOGGER = Logger.getLogger(ClaudeProcessLauncher.class.getName());

    private ClaudeProcessLauncher() {
    }

    public static final class Launched {
        public final PtyProcess process;
        public final TtyConnector connector;

        Launched(PtyProcess process, TtyConnector connector) {
            this.process = process;
            this.connector = connector;
        }
    }

    public static Launched launch() throws IOException {
        return launch(resolveCwd());
    }

    public static Launched launch(File cwd) throws IOException {
        return spawn(cwd, new String[]{resolveClaudeCommand()});
    }

    public static Launched launchShell(File cwd) throws IOException {
        return spawn(cwd, new String[]{resolveShellCommand()});
    }

    private static Launched spawn(File cwd, String[] command) throws IOException {
        Map<String, String> env = buildEnvironment();
        File resolved = which(command[0], env.get("PATH"));
        if (resolved == null) {
            throw new IOException("Could not find '" + command[0] + "' on PATH. "
                    + "Set the absolute path in Tools > Options > Miscellaneous > Claude Code.");
        }
        String[] resolvedCmd = command.clone();
        resolvedCmd[0] = resolved.getAbsolutePath();

        LOGGER.log(Level.INFO, "Spawning {0} in {1}", new Object[]{resolvedCmd[0], cwd.getAbsolutePath()});

        PtyProcess process = new PtyProcessBuilder(resolvedCmd)
                .setDirectory(cwd.getAbsolutePath())
                .setEnvironment(env)
                .setConsole(false)
                .setRedirectErrorStream(true)
                .setInitialColumns(120)
                .setInitialRows(32)
                .start();

        return new Launched(process, new ClaudeTtyConnector(process));
    }

    static File which(String name, String path) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        File asFile = new File(name);
        if (asFile.isAbsolute()) {
            return asFile.canExecute() ? asFile : null;
        }
        if (path == null) {
            return null;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (dir.isEmpty()) {
                continue;
            }
            File candidate = new File(dir, name);
            if (candidate.canExecute() && !candidate.isDirectory()) {
                return candidate;
            }
        }
        return null;
    }

    public static File resolveCwd() {
        Project[] open = OpenProjects.getDefault().getOpenProjects();
        if (open != null && open.length > 0) {
            FileObject dir = open[0].getProjectDirectory();
            if (dir != null) {
                File file = org.openide.filesystems.FileUtil.toFile(dir);
                if (file != null && file.isDirectory()) {
                    return file;
                }
            }
        }
        return new File(System.getProperty("user.home"));
    }

    static Map<String, String> buildEnvironment() {
        ClaudeCodeStatusService svc = Lookup.getDefault().lookup(ClaudeCodeStatusService.class);
        int port = (svc != null) ? svc.getServerPort() : -1;
        return buildEnvironment(port);
    }

    static Map<String, String> buildEnvironment(int port) {
        Map<String, String> env = new HashMap<>(System.getenv());
        if (port > 0) {
            env.put("CLAUDE_CODE_SSE_PORT", String.valueOf(port));
            env.put("ENABLE_IDE_INTEGRATION", "true");
        }
        env.putIfAbsent("TERM", "xterm-256color");
        env.put("PATH", augmentedPath(env.get("PATH")));
        return env;
    }

    static String augmentedPath(String currentPath) {
        // Desktop-launched NetBeans often inherits a minimal PATH that misses
        // user-local install dirs where `claude` typically lives. Prepend the
        // common ones if they exist on disk so the child process can find it.
        String home = System.getProperty("user.home");
        String[] candidates = new String[]{
            home + "/.local/bin",
            home + "/.npm-global/bin",
            home + "/.volta/bin",
            home + "/.bun/bin",
            home + "/.cargo/bin",
            home + "/bin",
            "/usr/local/bin",
            "/opt/homebrew/bin"
        };
        StringBuilder sb = new StringBuilder();
        for (String dir : candidates) {
            if (new File(dir).isDirectory()) {
                if (sb.length() > 0) {
                    sb.append(File.pathSeparator);
                }
                sb.append(dir);
            }
        }
        if (currentPath != null && !currentPath.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(currentPath);
        }
        return sb.toString();
    }

    static String resolveClaudeCommand() {
        String pref = ClaudeTerminalSettings.getCliPath();
        if (pref != null && !pref.isEmpty()) {
            return pref;
        }
        String override = System.getProperty("claude.code.cli");
        if (override != null && !override.isEmpty()) {
            return override;
        }
        return "claude";
    }

    static String resolveShellCommand() {
        String shell = System.getenv("SHELL");
        if (shell != null && !shell.isEmpty() && new File(shell).canExecute()) {
            return shell;
        }
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            String comspec = System.getenv("COMSPEC");
            return comspec != null && !comspec.isEmpty() ? comspec : "cmd.exe";
        }
        return "/bin/bash";
    }

    private static final class ClaudeTtyConnector extends ProcessTtyConnector {

        private final PtyProcess process;

        ClaudeTtyConnector(PtyProcess process) {
            super(process, StandardCharsets.UTF_8);
            this.process = process;
        }

        @Override
        public void resize(TermSize termSize) {
            process.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
        }

        @Override
        public String getName() {
            return "claude";
        }
    }
}
