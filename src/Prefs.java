import java.util.prefs.Preferences;

/*
 * Saved settings and win/loss record, stored with Java's built-in Preferences.
 * If preferences can't be read or written, the game still works - it just won't remember.
 */
final class Prefs {

    private static Preferences node;
    private static boolean unavailable;

    private Prefs() {
    }

    private static Preferences node() {
        if (node == null && !unavailable) {
            try {
                node = Preferences.userRoot().node("evil-hangman");
            } catch (RuntimeException | LinkageError e) {
                unavailable = true; // Don't keep retrying; play on without saving.
            }
        }
        return node;
    }

    static int getInt(String key, int fallback) {
        try {
            Preferences p = node();
            return p == null ? fallback : p.getInt(key, fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static boolean getBoolean(String key, boolean fallback) {
        try {
            Preferences p = node();
            return p == null ? fallback : p.getBoolean(key, fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static void putInt(String key, int value) {
        try {
            Preferences p = node();
            if (p != null) {
                p.putInt(key, value);
            }
        } catch (RuntimeException ignored) {
            // Not saved this time; nothing else to do.
        }
    }

    static void putBoolean(String key, boolean value) {
        try {
            Preferences p = node();
            if (p != null) {
                p.putBoolean(key, value);
            }
        } catch (RuntimeException ignored) {
            // Not saved this time; nothing else to do.
        }
    }
}
