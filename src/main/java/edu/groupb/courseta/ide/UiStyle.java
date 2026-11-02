package edu.groupb.courseta.ide;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/** Small, theme-aware Swing styles shared by both tool-window tabs. */
final class UiStyle {
    enum Tone { PRIMARY, SUCCESS, NEUTRAL }

    private UiStyle() { }

    static JButton button(String label, Tone tone) {
        return new StyledButton(label, tone);
    }

    static boolean dark() {
        Color background = UIManager.getColor("Panel.background");
        return background != null && background.getRed() * .299
            + background.getGreen() * .587 + background.getBlue() * .114 < 140;
    }

    static JPanel section(String title, JTextArea text) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));
        panel.add(label, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(text);
        // Match the active LAF instead of imposing a fixed light-colored frame.
        scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground") != null
            ? UIManager.getColor("Separator.foreground") : Color.GRAY));
        panel.add(scroll, BorderLayout.CENTER);
        panel.setMinimumSize(new Dimension(80, 90));
        return panel;
    }

    private static final class StyledButton extends JButton {
        private final Tone tone;

        StyledButton(String label, Tone tone) {
            super(label);
            this.tone = tone;
            setFont(getFont().deriveFont(Font.BOLD));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override public void updateUI() {
            // BasicButtonUI preserves keyboard activation and accessible button
            // semantics while leaving our background consistent across IDE LAFs.
            setUI(new BasicButtonUI());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        }

        @Override protected void paintComponent(Graphics graphics) {
            boolean dark = dark();
            Color fill = switch (tone) {
                case PRIMARY -> new Color(0x356AC3);
                case SUCCESS -> new Color(0x237A57);
                case NEUTRAL -> new Color(dark ? 0x343840 : 0xF0F2F5);
            };
            if (!isEnabled()) fill = new Color(dark ? 0x303238 : 0xE9EBEF);
            else if (getModel().isPressed()) fill = fill.darker();
            else if (getModel().isRollover()) fill = fill.darker();
            setForeground(tone == Tone.NEUTRAL
                ? new Color(dark ? 0xE6E8ED : 0x252C38) : Color.WHITE);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            if (hasFocus()) {
                g.setColor(new Color(dark ? 0xA9C7FF : 0x184FAD));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 9, 9);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
