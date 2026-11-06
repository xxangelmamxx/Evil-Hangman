import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/*
 * Loads wordlist.txt and serves the words of a given length.
 * Also remembers which words were the answer in earlier rounds, so they don't come straight back.
 */
final class WordBank {

    static final int MIN_LENGTH = 4;
    static final int MAX_LENGTH = 10;
    static final int MIN_WORDS = 40; // A length needs at least this many words to be playable.
    static final String FILE_NAME = "wordlist.txt";

    private final Map<Integer, List<String>> byLength = new TreeMap<>();
    private final Map<Integer, Set<String>> used = new HashMap<>();

    WordBank(Collection<String> rawWords) {
        Set<String> seen = new HashSet<>();
        for (String raw : rawWords) {
            String word = raw.trim().toLowerCase(Locale.ROOT);
            if (isPlayable(word) && seen.add(word)) {
                byLength.computeIfAbsent(word.length(), k -> new ArrayList<>()).add(word);
            }
        }
    }

    /* Plain letters, a sensible length, and at least one vowel (filters out junk like "msgstr"). */
    static boolean isPlayable(String word) {
        return word.length() >= MIN_LENGTH && word.length() <= MAX_LENGTH
                && word.matches("[a-z]+") && word.matches(".*[aeiou].*");
    }

    /*
     * Finds wordlist.txt in the current folder, next to the compiled classes (or a few folders up),
     * or inside the classpath, so the game works no matter where it is launched from.
     */
    static WordBank load() throws IOException {
        for (Path path : candidatePaths()) {
            if (Files.isRegularFile(path)) {
                return new WordBank(Files.readAllLines(path, StandardCharsets.UTF_8));
            }
        }
        try (InputStream in = WordBank.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                List<String> lines = new ArrayList<>();
                for (String line; (line = reader.readLine()) != null; ) {
                    lines.add(line);
                }
                return new WordBank(lines);
            }
        }
        throw new FileNotFoundException("Could not find " + FILE_NAME
                + ". Keep it in the project folder next to src/.");
    }

    private static List<Path> candidatePaths() {
        List<Path> paths = new ArrayList<>();
        paths.add(Paths.get(FILE_NAME));
        try {
            Path dir = Paths.get(WordBank.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (int i = 0; i < 4 && dir != null; i++) {
                paths.add(dir.resolve(FILE_NAME));
                dir = dir.getParent();
            }
        } catch (URISyntaxException | SecurityException | NullPointerException ignored) {
            // Fall back to the current folder and the classpath.
        }
        return paths;
    }

    /* Word lengths that have enough words to make a good game. */
    List<Integer> playableLengths() {
        List<Integer> lengths = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : byLength.entrySet()) {
            if (entry.getValue().size() >= MIN_WORDS) {
                lengths.add(entry.getKey());
            }
        }
        return lengths;
    }

    int count(int length) {
        return byLength.getOrDefault(length, Collections.emptyList()).size();
    }

    /* Every word of this length that hasn't been a recent answer. Starts over once all have been used. */
    List<String> wordsFor(int length) {
        List<String> all = byLength.getOrDefault(length, Collections.emptyList());
        Set<String> usedHere = used.computeIfAbsent(length, k -> new HashSet<>());
        List<String> fresh = new ArrayList<>();
        for (String word : all) {
            if (!usedHere.contains(word)) {
                fresh.add(word);
            }
        }
        if (fresh.isEmpty()) {
            usedHere.clear();
            fresh.addAll(all);
        }
        return fresh;
    }

    void markUsed(String word) {
        if (word != null) {
            used.computeIfAbsent(word.length(), k -> new HashSet<>()).add(word);
        }
    }
}
