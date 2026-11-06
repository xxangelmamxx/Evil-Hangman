import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/*
 * The shared night-time backdrop behind every screen: a dim glow, drifting fog and a dark vignette
 * underneath, and candle flicker, film grain, a red "danger" vignette and lightning flashes on top.
 */
class ScenePanel extends JPanel {

    private static final int FOG_W = 1024;
    private static final int FOG_H = 420;
    private static BufferedImage fog;
    private static BufferedImage grain;

    private BufferedImage backdrop;
    private BufferedImage vignette;
    private BufferedImage bloodVignette;
    private float danger;
    private boolean lastLife;
    private double flashAt = -10;
    private final Random random = new Random();

    ScenePanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Theme.BG);
        new Timer(40, e -> {
            if (isShowing()) {
                repaint();
            }
        }).start();
    }

    /* 0 = calm, 1 = about to die. Tints the screen edges red. */
    void setDanger(float danger, boolean lastLife) {
        this.danger = Math.max(0f, Math.min(1f, danger));
        this.lastLife = lastLife;
    }

    /* A pale lightning flash across the whole screen. */
    void flash() {
        flashAt = Theme.now();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        ensureImages(w, h);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(backdrop, 0, 0, null);

        // Two layers of fog drifting at different speeds along the lower part of the screen.
        double t = Theme.now();
        drawFog(g2, w, h, t * 9, h * 0.38, 0.55f);
        drawFog(g2, w, h, -t * 5 + 300, h * 0.55, 0.35f);

        g2.drawImage(vignette, 0, 0, null);
        g2.dispose();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        double t = Theme.now();

        // Edges bleed red as lives run out; on the last life it pulses like a heartbeat.
        float blood = danger * 0.55f;
        if (lastLife) {
            double beat = Math.pow(Math.max(0, Math.sin(t * Math.PI * 1.25)), 8);
            blood = 0.5f + (float) beat * 0.4f;
        }
        if (blood > 0.01f) {
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, blood)));
            g2.drawImage(bloodVignette, 0, 0, null);
        }

        // Lightning flash.
        double since = t - flashAt;
        if (since < 0.45) {
            float a = (float) (since < 0.06 ? 0.22 : 0.22 * (1 - (since - 0.06) / 0.39));
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, a)));
            g2.setColor(new Color(0xBFC6D6));
            g2.fillRect(0, 0, w, h);
        }

        // Candle-light flicker: the whole room breathes darker and lighter.
        float dim = (float) (0.05 + 0.04 * Theme.wobble(t * 1.3, 2.0));
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, dim)));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        // Film grain.
        g2.setComposite(AlphaComposite.SrcOver);
        int ox = random.nextInt(256);
        int oy = random.nextInt(256);
        for (int y = -oy; y < h; y += 256) {
            for (int x = -ox; x < w; x += 256) {
                g2.drawImage(grain, x, y, null);
            }
        }
        g2.dispose();
    }

    private void drawFog(Graphics2D g2, int w, int h, double offset, double top, float alpha) {
        int fw = (int) (w * 1.3);
        int fh = (int) (h * 0.62);
        int x = (int) (-(((offset % fw) + fw) % fw));
        Composite before = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.drawImage(fog, x, (int) top, fw, fh, null);
        g2.drawImage(fog, x + fw, (int) top, fw, fh, null);
        g2.setComposite(before);
    }

    private void ensureImages(int w, int h) {
        if (fog == null) {
            fog = makeFog();
            grain = makeGrain();
        }
        if (backdrop != null && backdrop.getWidth() == w && backdrop.getHeight() == h) {
            return;
        }
        w = Math.max(1, w);
        h = Math.max(1, h);
        float radius = Math.max(w, h) * 0.8f;
        Point center = new Point(w / 2, (int) (h * 0.42));

        backdrop = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D b = backdrop.createGraphics();
        b.setPaint(new RadialGradientPaint(center, radius, new float[]{0f, 1f},
                new Color[]{Theme.BG_LIFT, Theme.BG}));
        b.fillRect(0, 0, w, h);
        b.dispose();

        vignette = radialEdge(w, h, center, radius, Color.BLACK, 250);
        bloodVignette = radialEdge(w, h, center, radius, new Color(0x5A0505), 235);
    }

    private static BufferedImage radialEdge(int w, int h, Point center, float radius, Color color, int edgeAlpha) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new RadialGradientPaint(center, radius, new float[]{0f, 0.45f, 1f},
                new Color[]{Theme.alpha(color, 0), Theme.alpha(color, edgeAlpha / 3), Theme.alpha(color, edgeAlpha)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    /* Soft grey blobs that tile seamlessly left to right. */
    private static BufferedImage makeFog() {
        BufferedImage img = new BufferedImage(FOG_W, FOG_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random r = new Random(13);
        for (int i = 0; i < 70; i++) {
            float cx = r.nextFloat() * FOG_W;
            float cy = FOG_H * (0.3f + r.nextFloat() * 0.6f);
            // Keep each blob fully inside the image so the fog has no hard top or bottom edge.
            float rad = Math.min(60 + r.nextFloat() * 150, Math.min(cy, FOG_H - cy) - 2);
            Color c = new Color(150, 152, 165, 22 + r.nextInt(18));
            for (int dx = -FOG_W; dx <= FOG_W; dx += FOG_W) {
                g.setPaint(new RadialGradientPaint(cx + dx, cy, rad, new float[]{0f, 1f},
                        new Color[]{c, Theme.alpha(c, 0)}));
                g.fillOval((int) (cx + dx - rad), (int) (cy - rad), (int) (rad * 2), (int) (rad * 2));
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage makeGrain() {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(5);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                int v = r.nextInt(256);
                int a = r.nextInt(16);
                img.setRGB(x, y, (a << 24) | (v << 16) | (v << 8) | v);
            }
        }
        return img;
    }
}
