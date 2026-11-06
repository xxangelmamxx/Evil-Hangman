import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/* A keyboard of dark stone keys. Found letters glow like embers; wrong ones are struck out in red. */
class KeyboardPanel extends JPanel {

    interface Listener {
        void onLetter(char letter);
    }

    private static final String[] ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};
    private static final int GAP = 7;

    private final Map<Character, Key> keys = new HashMap<>();

    KeyboardPanel(Listener listener) {
        setOpaque(false);
        setLayout(null);
        for (String row : ROWS) {
            for (char upper : row.toCharArray()) {
                char letter = Character.toLowerCase(upper);
                Key key = new Key(String.valueOf(upper));
                key.addActionListener(e -> listener.onLetter(letter));
                keys.put(letter, key);
                add(key);
            }
        }
    }

    void reset() {
        for (Key key : keys.values()) {
            key.setState(Key.State.READY);
        }
    }

    void mark(char letter, boolean hit) {
        Key key = keys.get(Character.toLowerCase(letter));
        if (key != null) {
            key.setState(hit ? Key.State.HIT : Key.State.MISS);
        }
    }

    /* At the end of a round, lock the letters that were never used. */
    void lockRemaining() {
        for (Key key : keys.values()) {
            if (key.state == Key.State.READY) {
                key.setState(Key.State.LOCKED);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(10 * 52 + 9 * GAP, 3 * 52 + 2 * GAP);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(10 * 32 + 9 * GAP, 3 * 36 + 2 * GAP);
    }

    @Override
    public void doLayout() {
        int keyW = Math.min(58, (getWidth() - 9 * GAP) / 10);
        int keyH = Math.min((int) (keyW * 1.05), (getHeight() - 2 * GAP) / 3);
        Font font = Theme.serif(Font.PLAIN, Math.max(15, keyH * 0.48));
        int top = (getHeight() - (3 * keyH + 2 * GAP)) / 2;
        for (int r = 0; r < ROWS.length; r++) {
            String row = ROWS[r];
            int rowWidth = row.length() * keyW + (row.length() - 1) * GAP;
            int x = (getWidth() - rowWidth) / 2;
            int y = top + r * (keyH + GAP);
            for (char upper : row.toCharArray()) {
                Key key = keys.get(Character.toLowerCase(upper));
                key.setFont(font);
                key.setBounds(x, y, keyW, keyH);
                x += keyW + GAP;
            }
        }
    }

    /* One key. Painted by hand so it looks the same on every platform. */
    static final class Key extends JButton {

        enum State { READY, HIT, MISS, LOCKED }

        private State state = State.READY;
        private double changedAt;

        Key(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setState(State.READY);
        }

        void setState(State state) {
            this.state = state;
            this.changedAt = Theme.now();
            setEnabled(state == State.READY);
            setCursor(Cursor.getPredefinedCursor(state == State.READY ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.smooth(g);
            int w = getWidth();
            int h = getHeight();
            boolean hover = state == State.READY && getModel().isRollover();
            boolean pressed = state == State.READY && getModel().isPressed();
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 5, 5);

            Color top = Theme.STONE_HI;
            Color bottom = Theme.STONE;
            Color edge = Theme.EDGE;
            Color text = Theme.mix(Theme.BONE, Theme.ASH, 0.25f);
            if (hover) {
                top = new Color(0x2A2620);
                edge = Theme.alpha(Theme.EMBER, 150);
                text = Theme.BONE;
            }
            if (pressed) {
                top = Theme.STONE;
            }
            switch (state) {
                case HIT:
                    top = new Color(0x2A2116);
                    bottom = new Color(0x18130D);
                    edge = Theme.alpha(Theme.EMBER, 120);
                    text = Theme.EMBER;
                    break;
                case MISS:
                    top = new Color(0x0E0D10);
                    bottom = new Color(0x0B0A0C);
                    edge = new Color(0x1E1A1D);
                    text = new Color(0x4A3A3A);
                    break;
                case LOCKED:
                    top = new Color(0x0F0E12);
                    bottom = new Color(0x0C0B0E);
                    edge = new Color(0x1C1A1F);
                    text = Theme.DUSK;
                    break;
                default:
                    break;
            }

            if (state == State.HIT) {
                g2.setPaint(new RadialGradientPaint(w / 2f, h / 2f, Math.max(w, h) * 0.9f, new float[]{0f, 1f},
                        new Color[]{Theme.alpha(Theme.EMBER, 50), Theme.alpha(Theme.EMBER, 0)}));
                g2.fillRect(-w / 2, -h / 2, w * 2, h * 2);
            }
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fill(shape);
            g2.setColor(edge);
            g2.draw(shape);
            if (state == State.READY) {
                g2.setColor(Theme.alpha(Color.WHITE, 12));
                g2.drawLine(4, 2, w - 5, 2);
            }

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String label = getText();
            float x = (w - fm.stringWidth(label)) / 2f;
            float y = (h - fm.getAscent() - fm.getDescent()) / 2f + fm.getAscent();
            if (state == State.HIT) {
                Theme.drawGlowing(g2, label, x, y, text, Theme.alpha(Theme.EMBER, 150), 3);
            } else {
                g2.setColor(text);
                g2.drawString(label, x, y);
            }

            if (state == State.MISS) {
                // A red slash drawn across the letter.
                float p = (float) Math.min(1, (Theme.now() - changedAt) / 0.25);
                g2.setColor(Theme.alpha(Theme.BLOOD_HI, 210));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double x1 = w * 0.24;
                double y1 = h * 0.74;
                double x2 = w * 0.76;
                double y2 = h * 0.28;
                g2.draw(new Line2D.Double(x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p));
            }
            g2.dispose();
        }
    }
}
