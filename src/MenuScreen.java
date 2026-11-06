import javax.swing.*;
import java.awt.*;
import java.util.List;

/* The main menu: an empty noose swaying under the moon, the title, the options and the record. */
class MenuScreen extends ScenePanel {

    interface Host {
        void play();

        void showHowTo();

        void quit();

        void soundChanged();
    }

    private final Stats stats;
    private final JLabel record = new JLabel(" ");
    private final Segmented soundPicker;

    MenuScreen(Host host, Settings settings, Stats stats, List<Integer> lengths) {
        super(new GridBagLayout());
        this.stats = stats;

        HangmanView noose = new HangmanView();
        noose.setParts(1);
        noose.setSway(4.5);
        noose.setPreferredSize(new Dimension(380, 440));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        TitleArt title = new TitleArt(60);
        JLabel tagline = new JLabel("Save him, if you can.");
        tagline.setFont(Theme.serif(Font.ITALIC, 22));
        tagline.setForeground(Theme.ASH);
        tagline.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // Word length.
        String[] lengthLabels = new String[lengths.size()];
        int lengthIndex = Math.max(0, lengths.indexOf(settings.wordLength));
        for (int i = 0; i < lengths.size(); i++) {
            lengthLabels[i] = String.valueOf(lengths.get(i));
        }
        settings.wordLength = lengths.get(lengthIndex);
        Segmented lengthPicker = new Segmented(lengthLabels, lengthIndex,
                i -> settings.wordLength = lengths.get(i));

        // Candles (lives).
        String[] livesLabels = new String[Settings.LIVES_OPTIONS.length];
        int livesIndex = 1;
        for (int i = 0; i < livesLabels.length; i++) {
            livesLabels[i] = Settings.LIVES_NAMES[i];
            if (Settings.LIVES_OPTIONS[i] == settings.lives) {
                livesIndex = i;
            }
        }
        settings.lives = Settings.LIVES_OPTIONS[livesIndex];
        Segmented livesPicker = new Segmented(livesLabels, livesIndex,
                i -> settings.lives = Settings.LIVES_OPTIONS[i]);

        soundPicker = new Segmented(new String[]{"On", "Off"}, settings.sound ? 0 : 1, i -> {
            settings.sound = i == 0;
            Sound.setEnabled(settings.sound);
            host.soundChanged();
        });

        JPanel options = new JPanel(new GridBagLayout());
        options.setOpaque(false);
        addOption(options, 0, "Letters", lengthPicker);
        addOption(options, 1, "Mercy", livesPicker);
        addOption(options, 2, "Sound", soundPicker);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 2;
        filler.weightx = 1;
        options.add(Box.createHorizontalGlue(), filler);
        options.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        FlatButton play = FlatButton.primary("Begin");
        play.addActionListener(e -> host.play());
        FlatButton rules = FlatButton.ghost("Rules");
        rules.addActionListener(e -> host.showHowTo());
        FlatButton leave = FlatButton.ghost("Leave");
        leave.addActionListener(e -> host.quit());

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.add(play);
        actions.add(Box.createHorizontalStrut(12));
        actions.add(rules);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(leave);

        record.setFont(Theme.engraved(Font.PLAIN, 13, 0.14f));
        record.setForeground(Theme.mix(Theme.DUSK, Theme.ASH, 0.45f));
        record.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        for (JComponent c : new JComponent[]{title, tagline, options, actions, record}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }
        column.add(title);
        column.add(tagline);
        column.add(Box.createVerticalStrut(44));
        column.add(options);
        column.add(Box.createVerticalStrut(40));
        column.add(actions);
        column.add(Box.createVerticalStrut(30));
        column.add(record);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.insets = new Insets(0, 0, 0, 40);
        add(noose, gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        add(column, gc);
        refresh();
    }

    private static void addOption(JPanel panel, int row, String name, JComponent picker) {
        JLabel label = new JLabel(name.toUpperCase());
        label.setFont(Theme.engraved(Font.PLAIN, 12, 0.22f));
        label.setForeground(Theme.mix(Theme.DUSK, Theme.ASH, 0.35f));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 28);
        panel.add(label, gc);
        gc.gridx = 1;
        gc.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 0);
        panel.add(picker, gc);
    }

    /* Updates the record and the sound choice (which can also change from the game screen). */
    void refresh() {
        record.setText(stats.summary().toUpperCase());
        soundPicker.select(Sound.isEnabled() ? 0 : 1);
    }
}
