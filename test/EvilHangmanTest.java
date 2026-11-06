import java.util.*;

/*
 * Tests for the game rules and the word list. No libraries needed. From the project folder:
 *   javac -d out src/*.java test/*.java
 *   java -cp out EvilHangmanTest
 */
public class EvilHangmanTest {

    private static int passed;

    public static void main(String[] args) throws Exception {
        invalidAndRepeatedGuesses();
        dodgesWheneverAnyWordAvoidsTheLetter();
        forcedHitKeepsLargestFamily();
        forcedHitPrefersRevealingFewerLetters();
        winsWhenWordIsFilledIn();
        losesAfterMaxLivesAndRevealsConsistentWord();
        noGuessesAfterGameOver();
        neverContradictsEarlierGuessesOnRealWordList();
        wordBankFiltersAndRotatesUsedWords();
        hangmanPartsScaleWithLives();
        System.out.println("All " + passed + " tests passed.");
    }

    private static EvilHangman game(int lives, String... words) {
        return new EvilHangman(Arrays.asList(words), lives, new Random(42));
    }

    static void invalidAndRepeatedGuesses() {
        EvilHangman g = game(5, "cat", "dog");
        check(g.guess('1') == EvilHangman.Result.INVALID, "digits are invalid");
        check(g.guess('#') == EvilHangman.Result.INVALID, "symbols are invalid");
        g.guess('z');
        check(g.guess('Z') == EvilHangman.Result.ALREADY_GUESSED, "repeat (any case) is detected");
        check(g.wrongGuesses() == 1, "repeat does not cost a life");
    }

    static void dodgesWheneverAnyWordAvoidsTheLetter() {
        EvilHangman g = game(5, "cat", "car", "dog");
        check(g.guess('a') == EvilHangman.Result.MISS, "'a' dodged because 'dog' avoids it");
        check(g.candidateCount() == 1 && g.candidates().contains("dog"), "only dog remains");
        check(g.livesLeft() == 4, "a miss costs one life");
    }

    static void forcedHitKeepsLargestFamily() {
        // Every word has 'e'. Family "e__" has 1 word, "_e_" has 3.
        EvilHangman g = game(5, "egg", "bed", "red", "led");
        check(g.guess('e') == EvilHangman.Result.HIT, "forced hit when all words contain the letter");
        check(g.pattern().equals("_e_"), "keeps the biggest family, got " + g.pattern());
        check(g.candidateCount() == 3, "three words still possible");
        check(g.lastOccurrences() == 1, "one occurrence revealed");
    }

    static void forcedHitPrefersRevealingFewerLetters() {
        // Two families of equal size: "a_a_" (2 letters) and "_a__" (1 letter).
        EvilHangman g = game(5, "abac", "adad", "bacd", "cabd");
        g.guess('a');
        check(g.pattern().equals("_a__"), "tie goes to fewer revealed letters, got " + g.pattern());
    }

    static void winsWhenWordIsFilledIn() {
        EvilHangman g = game(5, "cab");
        g.guess('c');
        g.guess('a');
        check(!g.isOver(), "not over with a blank left");
        g.guess('b');
        check(g.isWon() && !g.isLost(), "won when every letter is revealed");
        check("cab".equals(g.answer()), "answer is the word");
    }

    static void losesAfterMaxLivesAndRevealsConsistentWord() {
        EvilHangman g = game(2, "cat", "dog", "pig");
        g.guess('c');
        check(!g.isOver(), "one miss of two lives is not over");
        g.guess('d');
        check(g.isLost(), "lost after two misses");
        String answer = g.answer();
        check(answer != null && answer.indexOf('c') < 0 && answer.indexOf('d') < 0,
                "revealed word avoids every missed letter, got " + answer);
    }

    static void noGuessesAfterGameOver() {
        EvilHangman g = game(1, "cat", "dog");
        g.guess('c');
        check(g.guess('o') == EvilHangman.Result.GAME_OVER, "no guesses once the round is over");
    }

    /* The core rule, checked over many random games on the real word list. */
    static void neverContradictsEarlierGuessesOnRealWordList() throws Exception {
        WordBank bank = WordBank.load();
        Random r = new Random(7);
        int games = 0;
        for (int length : bank.playableLengths()) {
            for (int i = 0; i < 150; i++) {
                EvilHangman g = new EvilHangman(bank.wordsFor(length), 1 + r.nextInt(12), r);
                List<Character> letters = new ArrayList<>();
                for (char c = 'a'; c <= 'z'; c++) {
                    letters.add(c);
                }
                Collections.shuffle(letters, r);
                for (char c : letters) {
                    if (g.isOver()) {
                        break;
                    }
                    g.guess(c);
                    for (String word : g.candidates()) {
                        for (char guessed : g.guessed()) {
                            for (int p = 0; p < length; p++) {
                                boolean inWord = word.charAt(p) == guessed;
                                boolean shown = g.pattern().charAt(p) == guessed;
                                if (inWord != shown) {
                                    throw new AssertionError("'" + word + "' contradicts guess '"
                                            + guessed + "' (pattern " + g.pattern() + ")");
                                }
                            }
                        }
                    }
                }
                check(g.isOver(), "every game ends");
                String answer = g.answer();
                for (char m : g.misses()) {
                    check(answer.indexOf(m) < 0, "final word avoids missed letter " + m);
                }
                games++;
            }
        }
        System.out.println("  simulated " + games + " games across lengths " + bank.playableLengths());
    }

    static void wordBankFiltersAndRotatesUsedWords() {
        WordBank bank = new WordBank(Arrays.asList("Apple", "apple", "msgstr", "abc", "ok-go", "mango", "\tlemon\r"));
        List<String> five = bank.wordsFor(5);
        check(five.size() == 3, "keeps apple, mango, lemon once each, got " + five);
        check(!five.contains("msgstr") && bank.count(3) == 0, "drops vowel-less and too-short words");
        bank.markUsed("mango");
        check(!bank.wordsFor(5).contains("mango"), "used word is skipped next round");
        bank.markUsed("apple");
        bank.markUsed("lemon");
        check(bank.wordsFor(5).size() == 3, "starts over once every word was used");
    }

    static void hangmanPartsScaleWithLives() {
        for (int lives : Settings.LIVES_OPTIONS) {
            int previous = 0;
            for (int wrong = 1; wrong <= lives; wrong++) {
                int parts = HangmanView.partsFor(wrong, lives);
                // With more lives than body parts, some misses only show in the hearts.
                boolean grows = lives <= HangmanView.PARTS ? parts > previous : parts >= previous;
                check(grows, "parts never shrink, and grow every miss when lives <= parts (lives " + lives + ")");
                check(wrong == lives ? parts == HangmanView.PARTS : parts < HangmanView.PARTS,
                        "full figure only on the last life (lives " + lives + ")");
                previous = parts;
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        passed++;
    }
}
