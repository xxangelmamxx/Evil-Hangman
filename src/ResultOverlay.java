import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

/* The end of a round: the screen sinks into darkness and the verdict fades in. */
class ResultOverlay extends JPanel {

    private static final double FADE_SECONDS = 1.6;

    private final Verdict verdict = new Verdict();
    private final JLabel subtitle = label(Theme.serif(Font.ITALIC, 22), Theme.ASH);
    private final Verdict word = new Verdict();
    private final JLabel record = label(Theme.engraved(Font.PLAIN, 13, 0.12f), Theme.ASH);
    private double shownAt;

    ResultOverlay(Runnable onPlayAgain, Runnable onMenu) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        // Swallow clicks so the board underneath can't be used while this is up.
        addMouseListener(new MouseAdapter() { });

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        FlatButton again = FlatButton.primary("Again");
        again.addActionListener(e -> onPlayAgain.run());
        FlatButton flee = FlatButton.ghost("Flee");
        flee.addActionListener(e -> onMenu.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        buttons.add(again);
        buttons.add(flee);

        for (JComponent c : new JComponent[]{verdict, subtitle, word, record, buttons}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
        }
        column.add(verdict);
        column.add(Box.createVerticalStrut(4));
        column.add(subtitle);
        column.add(Box.createVerticalStrut(22));
        column.add(word);
        column.add(Box.createVerticalStrut(30));
        column.add(buttons);
        column.add(Box.createVerticalStrut(26));
        column.add(record);
        add(column);
    }

    private static JLabel label(Font font, Color color) {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    void present(boolean won, String answer, Stats stats) {
        if (won) {
            verdict.set("HE LIVES", Theme.engraved(Font.PLAIN, 76, 0.2f), Theme.BONE, Theme.alpha(Theme.EMBER, 120));
            subtitle.setText("...for now.");
            word.set(spaced(answer), Theme.engraved(Font.PLAIN, 34, 0.1f), Theme.EMBER, Theme.alpha(Theme.EMBER, 90));
        } else {
            verdict.set("HANGED", Theme.engraved(Font.PLAIN, 84, 0.2f), Theme.BLOOD_HI, Theme.alpha(Theme.BLOOD_HI, 170));
            subtitle.setText("The word was");
            word.set(spaced(answer), Theme.engraved(Font.PLAIN, 34, 0.1f), Theme.BONE, Theme.alpha(Theme.MOON, 50));
        }
        record.setText(stats.summary().toUpperCase());
        shownAt = Theme.now();
        setVisible(true);
        revalidate();
        repaint();
    }

    private static String spaced(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toUpperCase().toCharArray()) {
            sb.append(c).append(' ');
        }
        return sb.toString().trim();
    }

    private float fade() {
        return (float) Math.min(1, Math.max(0, (Theme.now() - shownAt) / FADE_SECONDS));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        float f = fade();
        g2.setColor(Theme.alpha(Theme.BG, (int) (244 * Math.min(1, f * 1.6))));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        // The text appears after the screen has darkened.
        float f = Math.max(0, (fade() - 0.35f) / 0.65f);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(f));
        super.paintChildren(g2);
        g2.dispose();
    }

    /* A single line of glowing text, sized to fit. */
    private static final class Verdict extends JComponent {
        private String text = " ";
        private Font font = Theme.serif(Font.PLAIN, 20);
        private Color color = Theme.BONE;
        private Color glow = Theme.alpha(Theme.MOON, 0);

        void set(String text, Font font, Color color, Color glow) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.glow = glow;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(font);
            return new Dimension(fm.stringWidth(text) + 40, fm.getAscent() + fm.getDescent() + 16);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            float x = (getWidth() - fm.stringWidth(text)) / 2f;
            float y = 8 + fm.getAscent();
            Theme.drawGlowing(g2, text, x, y, color, glow, 7);
            g2.dispose();
        }
    }
}
