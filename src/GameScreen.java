import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Random;

/* The main game: the gallows, the candles, the word and the keyboard, with the verdict on top. */
class GameScreen extends ScenePanel {

    interface Host {
        void newRound();

        void showMenu();

        void roundFinished(EvilHangman game);

        Stats stats();
    }

    private static final String[] MISS_LINES = {
            "No.",
            "Not %s.",
            "Wrong.",
            "The rope tightens.",
            "He felt that.",
            "Another candle dies.",
            "%s? No.",
            "Try again.",
    };
    private static final String[] HIT_LINES = {
            "Yes... for now.",
            "%s. Fine.",
            "Lucky.",
            "Hm. %s.",
            "You know something.",
    };

    private final Host host;
    private final HangmanView hangman = new HangmanView();
    private final WordView word = new WordView();
    private final InfoPanel info = new InfoPanel();
    private final KeyboardPanel keyboard;
    private final ResultOverlay overlay;
    private final FlatButton soundButton = FlatButton.text("");
    private final Random random = new Random();

    private EvilHangman game;
    private Timer overlayDelay;

    GameScreen(Host host) {
        super(null);
        this.host = host;
        setLayout(new OverlayLayout(this));

        keyboard = new KeyboardPanel(this::guess);
        overlay = new ResultOverlay(host::newRound, host::showMenu);
        overlay.setVisible(false);

        JPanel middle = new JPanel(new BorderLayout(20, 10));
        middle.setOpaque(false);
        middle.add(hangman, BorderLayout.CENTER);
        middle.add(info, BorderLayout.EAST);
        middle.add(word, BorderLayout.SOUTH);

        JPanel board = new JPanel(new BorderLayout(0, 16));
        board.setOpaque(false);
        board.setBorder(BorderFactory.createEmptyBorder(14, 30, 24, 30));
        board.add(buildTopBar(), BorderLayout.NORTH);
        board.add(middle, BorderLayout.CENTER);
        board.add(keyboard, BorderLayout.SOUTH);

        // With OverlayLayout, the first component added is drawn on top.
        add(overlay);
        add(board);
        installKeys();
    }

    private JComponent buildTopBar() {
        FlatButton flee = FlatButton.text("←  Flee");
        flee.setFont(Theme.engraved(Font.PLAIN, 15, 0.12f));
        flee.addActionListener(e -> host.showMenu());
        FlatButton another = FlatButton.text("Another word");
        another.setFont(Theme.engraved(Font.PLAIN, 15, 0.12f));
        another.addActionListener(e -> host.newRound());
        soundButton.setFont(Theme.engraved(Font.PLAIN, 15, 0.12f));
        soundButton.addActionListener(e -> {
            Sound.setEnabled(!Sound.isEnabled());
            updateSoundLabel();
        });
        updateSoundLabel();

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 26, 0));
        right.setOpaque(false);
        right.add(soundButton);
        right.add(another);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(flee, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    void updateSoundLabel() {
        soundButton.setText(Sound.isEnabled() ? "Sound on" : "Sound off");
        soundButton.revalidate();
    }

    /* A-Z guess, Enter starts the next round once this one is over, Esc goes back to the menu. */
    private void installKeys() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        for (int i = 0; i < 26; i++) {
            char letter = (char) ('a' + i);
            String name = "guess-" + letter;
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A + i, 0), name);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A + i, InputEvent.SHIFT_DOWN_MASK), name);
            am.put(name, action(() -> guess(letter)));
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
        am.put("next", action(() -> {
            if (game != null && game.isOver()) {
                host.newRound();
            }
        }));
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "menu");
        am.put("menu", action(host::showMenu));
    }

    private AbstractAction action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) {
                    r.run();
                }
            }
        };
    }

    void start(EvilHangman newGame) {
        if (overlayDelay != null) {
            overlayDelay.stop();
        }
        this.game = newGame;
        overlay.setVisible(false);
        hangman.reset();
        word.reset(newGame.wordLength());
        keyboard.reset();
        info.start(newGame, "Choose a letter.");
        setDanger(0, false);
        Sound.heartbeat(false);
        updateSoundLabel();
        revalidate();
        repaint();
    }

    /* Called when leaving the game screen. */
    void stop() {
        Sound.heartbeat(false);
    }

    void guess(char letter) {
        if (game == null || game.isOver()) {
            return;
        }
        String shown = String.valueOf(Character.toUpperCase(letter));
        EvilHangman.Result result = game.guess(letter);
        switch (result) {
            case ALREADY_GUESSED:
                info.update(game, "You already chose " + shown + ".", Theme.ASH);
                return;
            case HIT:
                keyboard.mark(letter, true);
                info.update(game, String.format(pick(HIT_LINES), shown), Theme.EMBER);
                Sound.hit();
                break;
            case MISS:
                keyboard.mark(letter, false);
                String line = game.livesLeft() == 1 ? "One candle left." : String.format(pick(MISS_LINES), shown);
                info.update(game, line, Theme.BLOOD_HI);
                flash();
                Sound.miss();
                break;
            default:
                return;
        }
        hangman.setParts(HangmanView.partsFor(game.wrongGuesses(), game.maxLives()));
        word.setPattern(game.pattern());
        boolean lastLife = game.livesLeft() == 1 && !game.isOver();
        setDanger(game.wrongGuesses() / (float) game.maxLives(), lastLife);
        Sound.heartbeat(lastLife);
        if (game.isOver()) {
            finishRound();
        }
    }

    private void finishRound() {
        EvilHangman finished = game;
        keyboard.lockRemaining();
        host.roundFinished(finished);
        if (finished.isWon()) {
            hangman.setWon(true);
            word.setWon(true);
            info.update(finished, "...He breathes.", Theme.EMBER);
            setDanger(0, false);
            Sound.win();
        } else {
            word.showAnswer(finished.answer());
            info.update(finished, "It's over.", Theme.BLOOD_HI);
            setDanger(1, false);
            Sound.lose();
        }
        // Let the moment land before the verdict appears.
        overlayDelay = new Timer(1600, e -> overlay.present(finished.isWon(), finished.answer(), host.stats()));
        overlayDelay.setRepeats(false);
        overlayDelay.start();
    }

    private String pick(String[] lines) {
        return lines[random.nextInt(lines.length)];
    }

    // The board and the verdict overlap, so Swing must not assume children never overlap.
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}
