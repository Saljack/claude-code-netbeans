package org.openbeans.claude.netbeans.terminal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * Painted "green plus" icon used as the new-tab button glyph. Painted in code
 * to avoid bundling a raster asset.
 */
final class GreenPlusIcon implements Icon {

    private static final Color GREEN = new Color(0x4C, 0xAF, 0x50);

    private final int size;

    GreenPlusIcon(int size) {
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(GREEN);
            int thickness = Math.max(2, size / 5);
            int padding = Math.max(2, size / 5);
            int armLength = size - 2 * padding;
            // horizontal bar
            g2.fillRoundRect(x + padding, y + (size - thickness) / 2, armLength, thickness, thickness, thickness);
            // vertical bar
            g2.fillRoundRect(x + (size - thickness) / 2, y + padding, thickness, armLength, thickness, thickness);
        } finally {
            g2.dispose();
        }
    }
}
