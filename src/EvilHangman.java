import java.util.*;

/*
 * The rules of Evil Hangman, with no user interface code.
 *
 * There is no secret word. The game keeps every word that is still consistent with what the
 * player has seen. On each guess it groups those words by where the guessed letter would appear
 * (a "word family") and keeps one family:
 *
 *   1. If any words do NOT contain the letter, it keeps those - the guess is a miss.
 *      This is the core rule: the word keeps changing, but never to a word that
 *      contains a letter that has already been guessed.
 *   2. Only when every remaining word contains the letter is it forced to accept a hit.
 *      It then keeps the largest family (ties: the one revealing the fewest letters),
 *      so it stays as flexible - and as evil - as possible until the very end.
 *
 * Because every remaining word always matches the revealed pattern and avoids every missed
 * letter, the word shown at the end is always consistent with every guess the player made.
 */
public final class EvilHangman {

    public enum Result { HIT, MISS, ALREADY_GUESSED, INVALID, GAME_OVER }

    public static final char BLANK = '_';

    private final int wordLength;
    private final int maxLives;
    private final Random random;
    private final char[] pattern;
    private final Set<Character> guessed = new LinkedHashSet<>();
    private final List<Character> misses = new ArrayList<>();
    private List<String> candidates = new ArrayList<>();
    private int lastOccurrences;
    private String answer;

    public EvilHangman(Collection<String> words, int maxLives, Random random) {
        if (words == null || words.isEmpty()) {
            throw new IllegalArgumentException("Evil Hangman needs at least one word.");
        }
        if (maxLives < 1) {
            throw new IllegalArgumentException("maxLives must be at least 1.");
        }
        int length = -1;
        for (String word : words) {
            String w = word.toLowerCase(Locale.ROOT);
            if (length == -1) {
                length = w.length();
            } else if (w.length() != length) {
                throw new IllegalArgumentException("All words must be the same length.");
            }
            candidates.add(w);
        }
        if (length < 1 || length > 31) {
            throw new IllegalArgumentException("Word length must be between 1 and 31.");
        }
        this.wordLength = length;
        this.maxLives = maxLives;
        this.random = random;
        this.pattern = new char[length];
        Arrays.fill(pattern, BLANK);
    }

    /* Plays one letter and reports what happened. */
    public Result guess(char letter) {
        if (isOver()) {
            return Result.GAME_OVER;
        }
        char c = Character.toLowerCase(letter);
        if (c < 'a' || c > 'z') {
            return Result.INVALID;
        }
        if (!guessed.add(c)) {
            return Result.ALREADY_GUESSED;
        }

        // Group the remaining words by the positions where this letter appears (as a bitmask).
        Map<Integer, List<String>> families = new HashMap<>();
        for (String word : candidates) {
            families.computeIfAbsent(positionsOf(word, c), k -> new ArrayList<>()).add(word);
        }

        // Rule 1: dodge whenever possible. Rule 2: otherwise keep the most flexible family.
        int chosen = families.containsKey(0) ? 0 : pickForcedFamily(families);
        candidates = families.get(chosen);
        lastOccurrences = Integer.bitCount(chosen);

        if (chosen == 0) {
            misses.add(c);
        } else {
            for (int i = 0; i < wordLength; i++) {
                if ((chosen & (1 << i)) != 0) {
                    pattern[i] = c;
                }
            }
        }

        if (isWon()) {
            answer = new String(pattern);
        } else if (isLost()) {
            // Any remaining word is consistent with every guess; pick one to reveal.
            answer = candidates.get(random.nextInt(candidates.size()));
        }
        return chosen == 0 ? Result.MISS : Result.HIT;
    }

    private int pickForcedFamily(Map<Integer, List<String>> families) {
        int best = -1;
        int bestSize = -1;
        int bestRevealed = Integer.MAX_VALUE;
        int ties = 0;
        for (Map.Entry<Integer, List<String>> family : families.entrySet()) {
            int size = family.getValue().size();
            int revealed = Integer.bitCount(family.getKey());
            if (size > bestSize || (size == bestSize && revealed < bestRevealed)) {
                best = family.getKey();
                bestSize = size;
                bestRevealed = revealed;
                ties = 1;
            } else if (size == bestSize && revealed == bestRevealed && random.nextInt(++ties) == 0) {
                best = family.getKey(); // Pick fairly among equally good families.
            }
        }
        return best;
    }

    private static int positionsOf(String word, char c) {
        int mask = 0;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == c) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    public int wordLength() { return wordLength; }

    public int maxLives() { return maxLives; }

    public int wrongGuesses() { return misses.size(); }

    public int livesLeft() { return maxLives - misses.size(); }

    /* The revealed word so far, with BLANK for letters not yet found. */
    public String pattern() { return new String(pattern); }

    public List<Character> misses() { return Collections.unmodifiableList(misses); }

    public Set<Character> guessed() { return Collections.unmodifiableSet(guessed); }

    public boolean hasGuessed(char c) { return guessed.contains(Character.toLowerCase(c)); }

    /* How many words the game is still choosing between. */
    public int candidateCount() { return candidates.size(); }

    List<String> candidates() { return Collections.unmodifiableList(candidates); }

    /* How many times the last guessed letter appeared (0 for a miss). */
    public int lastOccurrences() { return lastOccurrences; }

    public boolean isWon() { return new String(pattern).indexOf(BLANK) < 0; }

    public boolean isLost() { return misses.size() >= maxLives; }

    public boolean isOver() { return isWon() || isLost(); }

    /* The word to show at the end of the round, or null while the round is still going. */
    public String answer() { return answer; }
}
