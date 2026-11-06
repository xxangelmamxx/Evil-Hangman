import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/*
 * The gallows on a hill, silhouetted against a full moon with clouds drifting across it.
 * The figure swings gently on the rope, jolts on every miss, and each new body part draws itself in.
 */
class HangmanView extends JComponent {

    /* Rope, head, body, two arms, two legs, two X eyes, frown. */
    static final int PARTS = 10;

    // Everything is drawn in a 260 x 300 design space and scaled to fit.
    private static final double W = 260;
    private static final double H = 300;
    private static final double DRAW_SECONDS = 0.45;
    private static final double PIVOT_X = 168;
    private static final double PIVOT_Y = 40;

    private int parts;
    private double partStart = -10;
    private boolean won;
    private double swayDegrees = 1.6;
    private double jolt = -10;

    HangmanView() {
        setOpaque(false);
        setPreferredSize(new Dimension(320, 370));
        setMinimumSize(new Dimension(180, 210));
        new Timer(30, e -> {
            if (isShowing()) {
                repaint();
            }
        }).start();
    }

    /*
     * How many parts to show for this many wrong guesses. Each miss adds at least one part
     * (when there are no more lives than parts), and the full figure only appears on the losing guess.
     */
    static int partsFor(int wrong, int maxLives) {
        if (wrong <= 0) {
            return 0;
        }
        if (wrong >= maxLives) {
            return PARTS;
        }
        int parts = (int) Math.ceil(wrong * (double) PARTS / maxLives);
        return Math.min(parts, PARTS - 1);
    }

    void reset() {
        parts = 0;
        won = false;
        jolt = -10;
        repaint();
    }

    void setParts(int count) {
        count = Math.max(0, Math.min(PARTS, count));
        if (count > parts) {
            partStart = Theme.now();
            jolt = Theme.now();
        }
        parts = count;
        repaint();
    }

    void setWon(boolean won) {
        this.won = won;
        repaint();
    }

    /* How far the figure swings, in degrees. */
    void setSway(double degrees) {
        this.swayDegrees = degrees;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        double t = Theme.now();
        float progress = (float) Math.min(1, (t - partStart) / DRAW_SECONDS);
        progress = 1 - (1 - progress) * (1 - progress) * (1 - progress);

        // A miss yanks the rope: a sharp swing that settles.
        double sinceJolt = t - jolt;
        double joltAngle = sinceJolt < 2.5 ? Math.sin(sinceJolt * 9) * Math.exp(-sinceJolt * 2.2) * 7 : 0;
        double angle = Math.toRadians(Math.sin(t * 1.1) * swayDegrees + joltAngle);
        double shake = sinceJolt < 0.25 ? (1 - sinceJolt / 0.25) * 3 : 0;

        g2.translate(Math.sin(t * 90) * shake, Math.cos(t * 77) * shake);
        paintScene(g2, getWidth(), getHeight(), parts, progress, won, angle, t);
        g2.dispose();
    }

    /* Paints the whole scene into a w x h area. Also used for the window icon. */
    static void paintScene(Graphics2D g2, double w, double h, int parts, float lastProgress,
                           boolean won, double angle, double time) {
        double scale = Math.min(w / W, h / H);
        g2.translate((w - W * scale) / 2, (h - H * scale) / 2);
        g2.scale(scale, scale);

        paintMoon(g2, time);
        paintHill(g2);

        // Gallows.
        beam(g2, 62, 268, 62, 30, 11);
        beam(g2, 54, 36, 184, 36, 10);
        beam(g2, 62, 84, 108, 36, 7);

        AffineTransform before = g2.getTransform();
        g2.rotate(angle, PIVOT_X, PIVOT_Y);
        for (int i = 0; i < parts; i++) {
            float p = (i == parts - 1) ? lastProgress : 1f;
            drawPart(g2, i, p, parts, won, time);
        }
        g2.setTransform(before);
    }

    private static void paintMoon(Graphics2D g2, double time) {
        double cx = 168;
        double cy = 112;
        double r = 66;
        // Halo, kept inside the design area so it never gets cut off at the edges.
        double halo = r * 1.38;
        g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) halo,
                new float[]{0f, 0.5f, 1f},
                new Color[]{Theme.alpha(Theme.MOON, 70), Theme.alpha(Theme.MOON, 22), Theme.alpha(Theme.MOON, 0)}));
        g2.fill(new Ellipse2D.Double(cx - halo, cy - halo, halo * 2, halo * 2));
        // Disc.
        g2.setPaint(new RadialGradientPaint((float) (cx - 18), (float) (cy - 20), (float) (r * 1.3f),
                new float[]{0f, 1f}, new Color[]{new Color(0xE6E0CF), new Color(0xA9A291)}));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        // Craters.
        g2.setColor(Theme.alpha(new Color(0x7D7768), 55));
        g2.fill(new Ellipse2D.Double(cx - 34, cy - 30, 26, 20));
        g2.fill(new Ellipse2D.Double(cx + 14, cy + 6, 30, 24));
        g2.fill(new Ellipse2D.Double(cx - 18, cy + 26, 16, 12));
        g2.fill(new Ellipse2D.Double(cx + 22, cy - 38, 12, 10));

        // Clouds drifting across.
        Shape clip = g2.getClip();
        g2.clip(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        for (int i = 0; i < 3; i++) {
            double speed = 5 + i * 2.5;
            double span = 300;
            double x = ((time * speed + i * 110) % span) - 110;
            double y = cy - 34 + i * 30;
            // Each cloud is a few soft, overlapping puffs.
            for (int k = 0; k < 6; k++) {
                double px = x + k * 17;
                double py = y + Math.sin(k * 1.7 + i) * 4;
                float pr = (float) (13 + (k * 5 + i * 3) % 9);
                g2.setPaint(new RadialGradientPaint((float) px, (float) py, pr, new float[]{0f, 1f},
                        new Color[]{Theme.alpha(new Color(0x0C0D12), 120), Theme.alpha(new Color(0x0C0D12), 0)}));
                g2.fill(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
            }
        }
        g2.setClip(clip);
    }

    private static void paintHill(Graphics2D g2) {
        // A mound that fades into the dark at its foot, so it has no hard edges.
        Path2D hill = new Path2D.Double();
        hill.moveTo(0, 300);
        hill.curveTo(10, 284, 40, 266, 110, 264);
        hill.curveTo(170, 262, 230, 270, 260, 300);
        hill.closePath();
        g2.setPaint(new GradientPaint(0, 262, new Color(0x050506), 0, 300, Theme.alpha(new Color(0x050506), 0)));
        g2.fill(hill);
        g2.setPaint(new GradientPaint(0, 262, Theme.alpha(Theme.MOON, 30), 0, 290, Theme.alpha(Theme.MOON, 0)));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new CubicCurve2D.Double(0, 300, 10, 284, 40, 266, 110, 264));
        g2.draw(new CubicCurve2D.Double(110, 264, 170, 262, 230, 270, 260, 300));
        // Dead grass along the crest.
        g2.setColor(new Color(0x050506));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 18; i++) {
            double gx = 34 + i * 10.6;
            double gy = 266 + Math.pow((gx - 120) / 120, 2) * 14;
            g2.draw(new Line2D.Double(gx, gy + 3, gx + Math.sin(i) * 3, gy - 4 - (i % 3) * 2));
        }
    }

    /* A dark beam with a faint moonlit edge. */
    private static void beam(Graphics2D g2, double x1, double y1, double x2, double y2, float width) {
        g2.setStroke(new BasicStroke(width + 2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(Theme.alpha(Theme.MOON, 34));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(new Color(0x0B0A0B));
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void limb(Graphics2D g2, double x1, double y1, double x2, double y2, float p) {
        Line2D line = new Line2D.Double(x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p);
        g2.setStroke(new BasicStroke(7.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(Theme.MOON, 36));
        g2.draw(line);
        g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.SILHOUETTE);
        g2.draw(line);
    }

    private static void drawPart(Graphics2D g2, int index, float p, int parts, boolean won, double time) {
        switch (index) {
            case 0: { // Rope with a noose loop
                g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(Theme.ROPE);
                g2.draw(new Line2D.Double(168, 40, 168, 40 + 34 * p));
                if (p >= 1f) {
                    g2.setStroke(new BasicStroke(2.6f));
                    g2.draw(new Ellipse2D.Double(158, 72, 20, 26));
                    g2.fill(new RoundRectangle2D.Double(163.5, 68, 9, 9, 3, 3));
                }
                break;
            }
            case 1: { // Head
                double r = 22 * p;
                Ellipse2D head = new Ellipse2D.Double(168 - r, 96 - r, r * 2, r * 2);
                g2.setColor(Theme.alpha(Theme.MOON, 40));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(head);
                g2.setColor(Theme.SILHOUETTE);
                g2.fill(head);
                if (p >= 1f && parts < 8) {
                    // Two pale points of light for eyes, until the X eyes replace them.
                    double blink = Math.sin(time * 0.7) > 0.985 ? 0.2 : 1;
                    Color eye = won ? Theme.EMBER : Theme.alpha(Theme.MOON, (int) (230 * blink));
                    glowDot(g2, 160, 92, eye);
                    glowDot(g2, 176, 92, eye);
                }
                break;
            }
            case 2:
                limb(g2, 168, 118, 168, 188, p);
                break;
            case 3:
                limb(g2, 168, 134, 146, 172, p);
                break;
            case 4:
                limb(g2, 168, 134, 190, 172, p);
                break;
            case 5:
                limb(g2, 168, 188, 151, 238, p);
                break;
            case 6:
                limb(g2, 168, 188, 185, 238, p);
                break;
            case 7:
                drawX(g2, 160, 92, p);
                break;
            case 8:
                drawX(g2, 176, 92, p);
                break;
            case 9: // A thin red line of a mouth
                g2.setColor(Theme.BLOOD_HI);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(160, 102, 16, 10, 20, 140 * p, Arc2D.OPEN));
                break;
            default:
                break;
        }
    }

    private static void glowDot(Graphics2D g2, double x, double y, Color c) {
        g2.setPaint(new RadialGradientPaint((float) x, (float) y, 7f, new float[]{0f, 1f},
                new Color[]{Theme.alpha(c, c.getAlpha() / 3), Theme.alpha(c, 0)}));
        g2.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
        g2.setColor(c);
        g2.fill(new Ellipse2D.Double(x - 1.8, y - 1.8, 3.6, 3.6));
    }

    private static void drawX(Graphics2D g2, double cx, double cy, float p) {
        double r = 4;
        g2.setPaint(new RadialGradientPaint((float) cx, (float) cy, 9f, new float[]{0f, 1f},
                new Color[]{Theme.alpha(Theme.BLOOD_HI, (int) (110 * p)), Theme.alpha(Theme.BLOOD_HI, 0)}));
        g2.fill(new Ellipse2D.Double(cx - 9, cy - 9, 18, 18));
        g2.setColor(Theme.BLOOD_HI);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float first = Math.min(1f, p * 2);
        float second = Math.max(0f, p * 2 - 1);
        g2.draw(new Line2D.Double(cx - r, cy - r, cx - r + 2 * r * first, cy - r + 2 * r * first));
        if (second > 0) {
            g2.draw(new Line2D.Double(cx + r, cy - r, cx + r - 2 * r * second, cy - r + 2 * r * second));
        }
    }

    /* A small square image of the scene, used as the window and dock icon. */
    static Image icon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Theme.smooth(img.getGraphics());
        g2.setColor(Theme.BG);
        g2.fill(new RoundRectangle2D.Double(0, 0, size, size, size * 0.22, size * 0.22));
        g2.clip(new RoundRectangle2D.Double(0, 0, size, size, size * 0.22, size * 0.22));
        paintScene(g2, size, size * 1.02, 1, 1f, false, 0, 0);
        g2.dispose();
        return img;
    }
}
