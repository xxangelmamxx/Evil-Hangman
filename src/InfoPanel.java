import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/*
 * The side of the game screen: one candle per life (snuffed out with a trail of smoke on each miss),
 * the letters that were wrong, and a whispered line of text.
 */
class InfoPanel extends JComponent {

    private int lives;
    private int maxLives;
    private double[] snuffedAt = new double[0];
    private String wrong = "";
    private String whisper = "";
    private Color whisperColor = Theme.ASH;
    private double whisperAt;

    InfoPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(290, 340));
        setMinimumSize(new Dimension(230, 300));
    }

    void start(EvilHangman game, String whisper) {
        maxLives = game.maxLives();
        lives = game.livesLeft();
        snuffedAt = new double[maxLives];
        update(game, whisper, Theme.ASH);
    }

    void update(EvilHangman game, String whisper, Color color) {
        double now = Theme.now();
        int newLives = game.livesLeft();
        for (int i = newLives; i < lives && i < snuffedAt.length; i++) {
            snuffedAt[i] = now;
        }
        lives = newLives;
        StringBuilder sb = new StringBuilder();
        for (char c : game.misses()) {
            sb.append(Character.toUpperCase(c)).append("  ");
        }
        wrong = sb.toString().trim();
        if (!whisper.equals(this.whisper)) {
            whisperAt = now;
        }
        this.whisper = whisper;
        this.whisperColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        int w = getWidth();
        int h = getHeight();
        double now = Theme.now();

        // Candles: one row, or two even rows when there are many.
        int rows = maxLives > 7 ? 2 : 1;
        int perRow = (maxLives + rows - 1) / Math.max(1, rows);
        double spacing = Math.min(40, (w - 20) / Math.max(1, perRow));
        double rowHeight = 92;
        double top = 34;
        for (int i = 0; i < maxLives; i++) {
            int row = i / perRow;
            int col = i % perRow;
            int inRow = Math.min(perRow, maxLives - row * perRow);
            double rowWidth = inRow * spacing;
            double cx = (w - rowWidth) / 2 + col * spacing + spacing / 2;
            double base = top + rowHeight * (row + 1);
            drawCandle(g2, i, cx, base, i < lives, snuffedAt.length > i ? snuffedAt[i] : 0, now);
        }

        double y = top + rowHeight * rows + 44;
        g2.setColor(Theme.EDGE);
        g2.fill(new Rectangle2D.Double(w / 2.0 - 40, y - 22, 80, 1));

        if (!wrong.isEmpty()) {
            g2.setFont(Theme.engraved(Font.PLAIN, 20, 0.05f));
            g2.setColor(new Color(0x6E3A36));
            y = drawCenteredWrapped(g2, wrong, w / 2.0, y + 8, w - 20);
        }

        // The whisper fades in each time it changes.
        float alpha = (float) Math.min(1, (now - whisperAt) / 0.6);
        Graphics2D wg = (Graphics2D) g2.create();
        wg.setComposite(AlphaComposite.SrcOver.derive(alpha));
        wg.setFont(Theme.serif(Font.ITALIC, 21));
        wg.setColor(whisperColor);
        drawCenteredWrapped(wg, whisper, w / 2.0, Math.max(y + 46, h - 40), w - 20);
        wg.dispose();
        g2.dispose();
    }

    private static void drawCandle(Graphics2D g2, int index, double cx, double base, boolean lit,
                                   double snuffed, double now) {
        double height = 38 + (index * 7 % 11);
        double width = 13;
        double topY = base - height;

        if (lit) {
            double flick = Theme.wobble(now * 1.7, index * 1.3);
            // Warm glow.
            float glowR = (float) (34 + flick * 4);
            g2.setPaint(new RadialGradientPaint((float) cx, (float) (topY - 8), glowR, new float[]{0f, 1f},
                    new Color[]{Theme.alpha(Theme.EMBER, 60), Theme.alpha(Theme.EMBER, 0)}));
            g2.fill(new Ellipse2D.Double(cx - glowR, topY - 8 - glowR, glowR * 2, glowR * 2));
        }

        // Wax.
        g2.setPaint(new GradientPaint((float) (cx - width / 2), 0, new Color(0xCFC5AD),
                (float) (cx + width / 2), 0, new Color(0x6E6656)));
        g2.fill(new RoundRectangle2D.Double(cx - width / 2, topY, width, height, 3, 3));
        // A drip.
        g2.fill(new RoundRectangle2D.Double(cx - width / 2 + 2, topY, 3.2, 9 + index % 4 * 3, 3, 3));
        if (lit) {
            g2.setColor(Theme.alpha(Theme.EMBER, 60));
            g2.fill(new Ellipse2D.Double(cx - width / 2, topY - 2, width, 4));
        }
        // Wick.
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(new Color(0x1A1612));
        g2.draw(new Line2D.Double(cx, topY, cx, topY - 4));

        if (lit) {
            double t = now * 1.7;
            double flick = Theme.wobble(t, index * 1.3);
            double fh = 15 + flick * 2.5;
            double fw = 7 + flick * 0.6;
            double lean = Theme.wobble(t * 0.8, index * 2.1) * 1.6;
            double fy = topY - 3;
            Path2D flame = new Path2D.Double();
            flame.moveTo(cx, fy);
            flame.curveTo(cx - fw, fy - fh * 0.25, cx - fw * 0.4 + lean, fy - fh * 0.7, cx + lean, fy - fh);
            flame.curveTo(cx + fw * 0.4 + lean, fy - fh * 0.7, cx + fw, fy - fh * 0.25, cx, fy);
            g2.setPaint(new GradientPaint((float) cx, (float) fy, new Color(0xFFF1C4),
                    (float) cx, (float) (fy - fh), new Color(0xE0862E)));
            g2.fill(flame);
            g2.setColor(Theme.alpha(new Color(0x3A5BBF), 120));
            g2.fill(new Ellipse2D.Double(cx - 2, fy - 4, 4, 4));
        } else if (snuffed > 0) {
            double since = now - snuffed;
            // The wick glows red for a moment...
            if (since < 1.6) {
                g2.setColor(Theme.alpha(Theme.BLOOD_HI, (int) (230 * (1 - since / 1.6))));
                g2.fill(new Ellipse2D.Double(cx - 1.6, topY - 5.5, 3.2, 3.2));
            }
            // ...and a thread of smoke curls up and fades.
            if (since < 3.2) {
                double fade = 1 - since / 3.2;
                for (int k = 0; k < 14; k++) {
                    double up = k * 4.2 + since * 12;
                    double sx = cx + Math.sin(k * 0.6 + since * 2.2) * (2 + k * 0.6);
                    double sy = topY - 6 - up;
                    double r = 1.6 + k * 0.35;
                    int a = (int) (70 * fade * (1 - k / 14.0));
                    g2.setColor(Theme.alpha(new Color(0x9A9AA4), a));
                    g2.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
                }
            }
        }
    }

    /* Draws centered text wrapped at spaces. Returns the baseline of the last line. */
    private static double drawCenteredWrapped(Graphics2D g2, String text, double cx, double y, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String attempt = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(attempt) > maxWidth && line.length() > 0) {
                Theme.drawCentered(g2, line.toString(), cx, y);
                y += fm.getHeight();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(attempt);
            }
        }
        Theme.drawCentered(g2, line.toString(), cx, y);
        return y;
    }
}
