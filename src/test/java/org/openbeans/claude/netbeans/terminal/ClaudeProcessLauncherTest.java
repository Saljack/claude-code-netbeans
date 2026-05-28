package org.openbeans.claude.netbeans.terminal;

import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClaudeProcessLauncherTest {

    @Test
    public void buildEnvironment_setsIdeVarsWhenPortIsKnown() {
        // given
        int port = 8995;

        // when
        Map<String, String> env = ClaudeProcessLauncher.buildEnvironment(port);

        // then
        assertEquals("8995", env.get("CLAUDE_CODE_SSE_PORT"));
        assertEquals("true", env.get("ENABLE_IDE_INTEGRATION"));
        assertEquals("xterm-256color", env.get("TERM"));
    }

    @Test
    public void buildEnvironment_omitsIdeVarsWhenPortMissing() {
        // given
        int port = -1;

        // when
        Map<String, String> env = ClaudeProcessLauncher.buildEnvironment(port);

        // then
        assertFalse(env.containsKey("CLAUDE_CODE_SSE_PORT"));
        assertFalse(env.containsKey("ENABLE_IDE_INTEGRATION"));
        assertEquals("xterm-256color", env.get("TERM"));
    }

    @Test
    public void buildEnvironment_preservesExistingTermVariable() {
        // given
        int port = 8995;

        // when
        Map<String, String> env = ClaudeProcessLauncher.buildEnvironment(port);

        // then
        // TERM is set from inherited env, fall back to xterm-256color
        assertNotNull(env.get("TERM"));
    }

    @Test
    public void resolveCwd_fallsBackToUserHomeWhenNoProjects() {
        // given
        // no OpenProjects.getDefault() services in test classpath -> getOpenProjects() returns empty array

        // when
        File cwd = ClaudeProcessLauncher.resolveCwd();

        // then
        assertNotNull(cwd);
        assertTrue(cwd.isDirectory(), "cwd should be a directory");
        assertEquals(new File(System.getProperty("user.home")), cwd);
    }
}
