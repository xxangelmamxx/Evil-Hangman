import javax.swing.*;
import java.awt.*;

/* The rules, and nothing more. */
class HowToScreen extends ScenePanel {

    private static final String[] LINES = {
            "Guess the word, one letter at a time.",
            "Each wrong letter puts out a candle",
            "and brings him closer to the rope.",
            "When the last candle dies, so does he.",
    };

    HowToScreen(Runnable onBack) {
        super(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("THE RULES");
        heading.setFont(Theme.engraved(Font.PLAIN, 34, 0.3f));
        heading.setForeground(Theme.BLOOD_HI);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        column.add(heading);
        column.add(Box.createVerticalStrut(36));

        for (int i = 0; i < LINES.length; i++) {
            JLabel line = new JLabel(LINES[i]);
            line.setFont(Theme.serif(Font.ITALIC, 25));
            line.setForeground(Theme.BONE);
            line.setAlignmentX(CENTER_ALIGNMENT);
            column.add(line);
            column.add(Box.createVerticalStrut(i == 0 || i == 2 ? 24 : 6));
        }

        JLabel keys = new JLabel("TYPE OR CLICK A LETTER   ·   ENTER TO TRY AGAIN   ·   ESC TO FLEE");
        keys.setFont(Theme.engraved(Font.PLAIN, 12, 0.16f));
        keys.setForeground(Theme.mix(Theme.DUSK, Theme.ASH, 0.5f));
        keys.setAlignmentX(CENTER_ALIGNMENT);
        column.add(Box.createVerticalStrut(30));
        column.add(keys);

        FlatButton back = FlatButton.primary("I understand");
        back.addActionListener(e -> onBack.run());
        back.setAlignmentX(CENTER_ALIGNMENT);
        column.add(Box.createVerticalStrut(40));
        column.add(back);
        add(column);
    }
}
