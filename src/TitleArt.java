import javax.swing.*;
import java.awt.*;

/* "EVIL HANGMAN" in engraved capitals. "EVIL" glows blood red and flickers like a dying light. */
class TitleArt extends JComponent {

    private final Font font;

    TitleArt(float size) {
        font = Theme.engraved(Font.PLAIN, size, 0.14f);
        setOpaque(false);
        FontMetrics fm = getFontMetrics(font);
        setPreferredSize(new Dimension(fm.stringWidth("EVIL HANGMAN") + 30, fm.getAscent() + fm.getDescent() + 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int baseline = 8 + fm.getAscent();
        int x = 8;

        // Mostly steady, with an occasional stutter.
        double t = Theme.now();
        double stutter = Math.sin(t * 0.9) > 0.97 ? (Math.sin(t * 70) > 0 ? 0.35 : 1) : 1;
        float glow = (float) ((0.75 + 0.25 * Theme.wobble(t, 4)) * stutter);
        Color red = Theme.mix(Theme.BLOOD, Theme.BLOOD_HI, glow);
        Theme.drawGlowing(g2, "EVIL", x, baseline, red, Theme.alpha(Theme.BLOOD_HI, (int) (170 * glow)), 6);

        int hx = x + fm.stringWidth("EVIL ");
        Theme.drawGlowing(g2, "HANGMAN", hx, baseline, Theme.BONE, Theme.alpha(Theme.MOON, 40), 3);
        g2.dispose();
    }
}
