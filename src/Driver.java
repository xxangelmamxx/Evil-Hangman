import javax.swing.*;
import java.io.IOException;

/*
 * Evil Hangman - originally developed by Eddie Ubri Jr on an HP Stream 14 :)
 *
 * Starts the game. Run from the project folder:
 *   javac -d out src/*.java
 *   java -cp out Driver
 */
public class Driver {

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Evil Hangman");
        SwingUtilities.invokeLater(() -> {
            WordBank bank;
            try {
                bank = WordBank.load();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Evil Hangman",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            new GameWindow(bank).setVisible(true);
        });
    }
}
