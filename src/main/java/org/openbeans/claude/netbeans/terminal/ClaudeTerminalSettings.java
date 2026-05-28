package org.openbeans.claude.netbeans.terminal;

import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

public final class ClaudeTerminalSettings extends DefaultSettingsProvider {

    public static final String KEY_THEME = "terminal.theme";
    public static final String KEY_FONT_FAMILY = "terminal.font.family";
    public static final String KEY_FONT_SIZE = "terminal.font.size";
    public static final String KEY_CLI_PATH = "terminal.cli.path";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";

    public static final String DEFAULT_THEME = THEME_DARK;
    public static final String DEFAULT_FONT_FAMILY = Font.MONOSPACED;
    public static final int DEFAULT_FONT_SIZE = 13;

    private static final TerminalColor DARK_BG = TerminalColor.rgb(30, 30, 30);
    private static final TerminalColor DARK_FG = TerminalColor.rgb(220, 220, 220);
    private static final TerminalColor LIGHT_BG = TerminalColor.rgb(255, 255, 255);
    private static final TerminalColor LIGHT_FG = TerminalColor.rgb(0, 0, 0);

    public static Preferences prefs() {
        return NbPreferences.forModule(ClaudeTerminalSettings.class);
    }

    public static String getTheme() {
        return prefs().get(KEY_THEME, DEFAULT_THEME);
    }

    public static String getFontFamily() {
        return prefs().get(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
    }

    public static int getFontSize() {
        return prefs().getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    public static String getCliPath() {
        return prefs().get(KEY_CLI_PATH, "");
    }

    public static Set<String> getMonospacedFontFamilies() {
        Set<String> result = new HashSet<>();
        result.add(Font.MONOSPACED);
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (looksMonospaced(family)) {
                result.add(family);
            }
        }
        return result;
    }

    private static final Set<String> KNOWN_MONOSPACED_HINTS = new HashSet<>(Arrays.asList(
            "mono", "consolas", "courier", "menlo", "monaco", "hack", "fira", "jetbrains",
            "source code", "ubuntu mono", "cascadia", "ibm plex mono", "noto sans mono",
            "dejavu sans mono", "liberation mono", "anonymous", "roboto mono", "inconsolata"
    ));

    private static boolean looksMonospaced(String family) {
        String lower = family.toLowerCase();
        for (String hint : KNOWN_MONOSPACED_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Font getTerminalFont() {
        String family = getFontFamily();
        int size = getFontSize();
        Font font = new Font(family, Font.PLAIN, size);
        if (font.getFamily().equals(Font.DIALOG) && !Font.MONOSPACED.equalsIgnoreCase(family)) {
            return new Font(Font.MONOSPACED, Font.PLAIN, size);
        }
        return font;
    }

    @Override
    public float getTerminalFontSize() {
        return (float) getFontSize();
    }

    @Override
    public TerminalColor getDefaultForeground() {
        return THEME_LIGHT.equals(getTheme()) ? LIGHT_FG : DARK_FG;
    }

    @Override
    public TerminalColor getDefaultBackground() {
        return THEME_LIGHT.equals(getTheme()) ? LIGHT_BG : DARK_BG;
    }

    @Override
    public TextStyle getDefaultStyle() {
        return new TextStyle(getDefaultForeground(), getDefaultBackground());
    }

    @Override
    public boolean audibleBell() {
        return false;
    }

    @Override
    public boolean useInverseSelectionColor() {
        return true;
    }

    @Override
    public boolean copyOnSelect() {
        return false;
    }
}
