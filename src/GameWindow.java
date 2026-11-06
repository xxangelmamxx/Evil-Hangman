import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Random;

/* The application window. Switches between the menu, the rules and the game. */
class GameWindow extends JFrame implements MenuScreen.Host, GameScreen.Host {

    private static final String MENU = "menu";
    private static final String HOW_TO = "howto";
    private static final String GAME = "game";

    private final WordBank bank;
    private final Settings settings = Settings.load();
    private final Stats stats = Stats.load();
    private final Random random = new Random();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final MenuScreen menu;
    private final GameScreen game;

    GameWindow(WordBank bank) {
        super("Evil Hangman");
        this.bank = bank;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Image icon = HangmanView.icon(256);
        setIconImages(Arrays.asList(icon, HangmanView.icon(64), HangmanView.icon(32)));
        try {
            Taskbar.getTaskbar().setIconImage(icon); // Dock icon on macOS.
        } catch (RuntimeException ignored) {
            // Not supported on this platform.
        }

        Sound.init(settings.sound);
        menu = new MenuScreen(this, settings, stats, bank.playableLengths());
        game = new GameScreen(this);
        root.setBackground(Theme.BG);
        root.add(menu, MENU);
        root.add(new HowToScreen(this::showMenu), HOW_TO);
        root.add(game, GAME);
        setContentPane(root);

        setMinimumSize(new Dimension(900, 680));
        setSize(1120, 780);
        setLocationRelativeTo(null);
    }

    @Override
    public void play() {
        settings.save();
        EvilHangman round = new EvilHangman(bank.wordsFor(settings.wordLength), settings.lives, random);
        game.start(round);
        cards.show(root, GAME);
    }

    @Override
    public void showHowTo() {
        cards.show(root, HOW_TO);
    }

    @Override
    public void quit() {
        dispose();
        System.exit(0);
    }

    @Override
    public void soundChanged() {
        settings.save();
    }

    @Override
    public void newRound() {
        play();
    }

    @Override
    public void showMenu() {
        game.stop();
        settings.sound = Sound.isEnabled();
        settings.save();
        menu.refresh();
        cards.show(root, MENU);
    }

    @Override
    public void roundFinished(EvilHangman round) {
        stats.record(round.isWon());
        bank.markUsed(round.answer());
    }

    @Override
    public Stats stats() {
        return stats;
    }

    GameScreen gameScreen() {
        return game;
    }
}
