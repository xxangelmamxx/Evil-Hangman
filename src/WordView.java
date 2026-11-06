import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/* The word as engraved letters over faint lines. Found letters rise out of the dark. */
class WordView extends JComponent {

    private static final double RISE_SECONDS = 0.7;

    private char[] pattern = new char[0];
    private double[] revealedAt = new double[0];
    private String answer;
    private double answerAt;
    private boolean won;

    WordView() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 92));
        setMinimumSize(new Dimension(300, 64));
    }

    void reset(int length) {
        pattern = new char[length];
        Arrays.fill(pattern, EvilHangman.BLANK);
        revealedAt = new double[length];
        answer = null;
        won = false;
        repaint();
    }

    void setPattern(String newPattern) {
        double now = Theme.now();
        for (int i = 0; i < pattern.length && i < newPattern.length(); i++) {
            if (pattern[i] == EvilHangman.BLANK && newPattern.charAt(i) != EvilHangman.BLANK) {
                revealedAt[i] = now + i * 0.06;
            }
            pattern[i] = newPattern.charAt(i);
        }
        repaint();
    }

    /* After a loss, the missing letters bleed in, in red. */
    void showAnswer(String answer) {
        this.answer = answer;
        this.answerAt = Theme.now();
        repaint();
    }

    void setWon(boolean won) {
        this.won = won;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int n = pattern.length;
        if (n == 0) {
            return;
        }
        Graphics2D g2 = Theme.smooth(g);
        double gap = 16;
        double slot = Math.min(58, (getWidth() - gap * (n - 1)) / n);
        double total = slot * n + gap * (n - 1);
        double x0 = (getWidth() - total) / 2;
        double lineY = getHeight() * 0.80;
        double now = Theme.now();

        g2.setFont(Theme.serif(Font.PLAIN, Math.min(56, slot * 1.05)));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < n; i++) {
            double x = x0 + i * (slot + gap);
            double cx = x + slot / 2;

            // The line under each letter.
            g2.setColor(won ? Theme.alpha(Theme.EMBER, 140) : Theme.DUSK);
            g2.fillRect((int) x, (int) lineY, (int) slot, 2);

            char c = pattern[i];
            String letter = null;
            float alpha = 1f;
            double rise = 0;
            Color color = won ? Theme.EMBER : Theme.BONE;
            Color glow = won ? Theme.alpha(Theme.EMBER, 140) : Theme.alpha(Theme.MOON, 60);

            if (c != EvilHangman.BLANK) {
                letter = String.valueOf(Character.toUpperCase(c));
                double t = (now - revealedAt[i]) / RISE_SECONDS;
                if (t < 1) {
                    t = Math.max(0, t);
                    alpha = (float) t;
                    rise = (1 - t) * (1 - t) * 14;
                }
            } else if (answer != null && i < answer.length()) {
                letter = String.valueOf(Character.toUpperCase(answer.charAt(i)));
                double t = Math.min(1, Math.max(0, (now - answerAt - i * 0.12) / 1.2));
                alpha = (float) t;
                color = Theme.BLOOD_HI;
                glow = Theme.alpha(Theme.BLOOD_HI, 120);
            }

            if (letter != null && alpha > 0) {
                Graphics2D lg = (Graphics2D) g2.create();
                lg.setComposite(AlphaComposite.SrcOver.derive(alpha));
                float lx = (float) (cx - fm.stringWidth(letter) / 2.0);
                float baseline = (float) (lineY - 10 + rise);
                Theme.drawGlowing(lg, letter, lx, baseline, color, glow, 3);
                lg.dispose();
            }
        }
        g2.dispose();
    }
}
