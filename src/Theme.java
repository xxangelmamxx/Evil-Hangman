import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;

/* Colors, fonts and drawing helpers shared by every screen. Night, moonlight, candles and blood. */
final class Theme {

    static final Color BG = new Color(0x07080B);
    static final Color BG_LIFT = new Color(0x17181F);
    static final Color STONE = new Color(0x141318);
    static final Color STONE_HI = new Color(0x1F1D24);
    static final Color EDGE = new Color(0x2E2A31);
    static final Color BONE = new Color(0xDCD3C1);
    static final Color ASH = new Color(0x8A8276);
    static final Color DUSK = new Color(0x4A4540);
    static final Color BLOOD = new Color(0x8E1B1B);
    static final Color BLOOD_HI = new Color(0xC7342E);
    static final Color EMBER = new Color(0xE0A458);
    static final Color MOON = new Color(0xD9D3C2);
    static final Color SILHOUETTE = new Color(0x040405);
    static final Color ROPE = new Color(0x5E4E3C);

    private static final String SERIF_FAMILY = pick("Baskerville", "Baskerville Old Face", "Book Antiqua",
            "Palatino Linotype", "Georgia", Font.SERIF);

    private Theme() {
    }

    private static String pick(String... families) {
        Set<String> available = new HashSet<>();
        try {
            available.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()));
        } catch (RuntimeException ignored) {
            // Use the logical font at the end of the list.
        }
        for (String family : families) {
            if (available.contains(family)) {
                return family;
            }
        }
        return families[families.length - 1];
    }

    static Font serif(int style, double size) {
        return new Font(SERIF_FAMILY, style, (int) Math.round(size));
    }

    /* Serif with extra letter spacing, for engraved-looking capitals. */
    static Font engraved(int style, double size, float tracking) {
        return serif(style, size).deriveFont(Collections.singletonMap(TextAttribute.TRACKING, tracking));
    }

    /* A copy of the graphics context with antialiasing turned on. Dispose it when done. */
    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1 - t;
        return new Color(Math.round(a.getRed() * u + b.getRed() * t),
                Math.round(a.getGreen() * u + b.getGreen() * t),
                Math.round(a.getBlue() * u + b.getBlue() * t),
                Math.round(a.getAlpha() * u + b.getAlpha() * t));
    }

    /* Seconds since some fixed point, for animations. */
    static double now() {
        return System.nanoTime() / 1e9;
    }

    /* Smooth pseudo-random wobble between -1 and 1, for flicker and flames. */
    static double wobble(double t, double seed) {
        return (Math.sin(t * 2.3 + seed) * 0.5 + Math.sin(t * 5.7 + seed * 1.7) * 0.3
                + Math.sin(t * 13.1 + seed * 3.1) * 0.2);
    }

    /* Draws text centered on (cx, baseline). */
    static void drawCentered(Graphics2D g2, String text, double cx, double baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) baseline);
    }

    /* Draws text with a soft glow of the given color behind it. */
    static void drawGlowing(Graphics2D g2, String text, float x, float baseline, Color color, Color glow,
                            int radius) {
        Composite before = g2.getComposite();
        for (int r = radius; r >= 1; r--) {
            g2.setColor(alpha(glow, Math.max(4, glow.getAlpha() / (radius + 2))));
            for (int a = 0; a < 8; a++) {
                double ang = a * Math.PI / 4;
                g2.drawString(text, (float) (x + Math.cos(ang) * r), (float) (baseline + Math.sin(ang) * r));
            }
        }
        g2.setComposite(before);
        g2.setColor(color);
        g2.drawString(text, x, baseline);
    }
}
