import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/*
 * Custom-painted buttons that look the same on every platform.
 *   PRIMARY - a dark blood-red slab that glows when hovered.
 *   GHOST   - an outlined button for secondary actions.
 *   TEXT    - bare engraved text; when selected it brightens and gets a red underline.
 */
class FlatButton extends JButton {

    enum Kind { PRIMARY, GHOST, TEXT }

    private final Kind kind;
    private boolean chosen;

    FlatButton(String text, Kind kind, double fontSize) {
        super(text);
        this.kind = kind;
        setFont(Theme.engraved(Font.PLAIN, fontSize, kind == Kind.TEXT ? 0.08f : 0.18f));
        setForeground(Theme.BONE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        // Buttons never take keyboard focus, so typed letters always reach the game.
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (kind == Kind.TEXT) {
            setBorder(BorderFactory.createEmptyBorder(6, 2, 10, 2));
        } else if (kind == Kind.PRIMARY) {
            setBorder(BorderFactory.createEmptyBorder(15, 46, 15, 46));
        } else {
            setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        }
    }

    static FlatButton primary(String text) {
        return new FlatButton(text.toUpperCase(), Kind.PRIMARY, 17);
    }

    static FlatButton ghost(String text) {
        return new FlatButton(text.toUpperCase(), Kind.GHOST, 13);
    }

    static FlatButton text(String text) {
        return new FlatButton(text, Kind.TEXT, 17);
    }

    /* For TEXT buttons used as options: marks this one as the current choice. */
    void setChosen(boolean chosen) {
        this.chosen = chosen;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        return new Dimension(fm.stringWidth(getText()) + in.left + in.right,
                fm.getAscent() + fm.getDescent() + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.smooth(g);
        ButtonModel model = getModel();
        boolean hover = isEnabled() && model.isRollover();
        boolean pressed = isEnabled() && model.isPressed();
        int w = getWidth();
        int h = getHeight();

        Color text = Theme.BONE;
        if (kind == Kind.PRIMARY) {
            if (hover) {
                for (int r = 10; r >= 2; r -= 2) {
                    g2.setColor(Theme.alpha(Theme.BLOOD_HI, 10));
                    g2.fill(new RoundRectangle2D.Float(-r / 2f + 5, -r / 2f + 5, w + r - 10, h + r - 10, 6, 6));
                }
            }
            Color top = pressed ? Theme.BLOOD : hover ? new Color(0xA82424) : new Color(0x8A1C1C);
            Color bottom = pressed ? new Color(0x4A0C0C) : new Color(0x5C1010);
            g2.setPaint(new GradientPaint(0, 4, top, 0, h - 4, bottom));
            g2.fill(new RoundRectangle2D.Float(5, 5, w - 10, h - 10, 4, 4));
            g2.setColor(Theme.alpha(Theme.BLOOD_HI, hover ? 200 : 110));
            g2.draw(new RoundRectangle2D.Float(5.5f, 5.5f, w - 11, h - 11, 4, 4));
            g2.setColor(Theme.alpha(Color.WHITE, 18));
            g2.drawLine(8, 7, w - 9, 7);
        } else if (kind == Kind.GHOST) {
            g2.setColor(Theme.alpha(Theme.STONE, hover ? 230 : 150));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 4, 4));
            g2.setColor(hover ? Theme.ASH : Theme.EDGE);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 4, 4));
            text = hover ? Theme.BONE : Theme.mix(Theme.BONE, Theme.ASH, 0.4f);
        } else {
            text = chosen ? Theme.BONE : hover ? Theme.mix(Theme.ASH, Theme.BONE, 0.6f) : Theme.ASH;
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String label = getText();
        int tw = fm.stringWidth(label);
        Insets in = getInsets();
        int x = (w - tw) / 2;
        int y = in.top + (h - in.top - in.bottom - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
        if (kind == Kind.PRIMARY) {
            Theme.drawGlowing(g2, label, x, y, text, Theme.alpha(Theme.BLOOD_HI, hover ? 160 : 80), 3);
        } else {
            g2.setColor(text);
            g2.drawString(label, x, y);
        }
        if (kind == Kind.TEXT && chosen) {
            g2.setColor(Theme.BLOOD_HI);
            g2.fillRect(x, y + fm.getDescent() + 2, tw, 2);
        }
        g2.dispose();
    }
}
