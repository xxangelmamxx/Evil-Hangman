/* The options chosen on the main menu. Remembered between launches. */
final class Settings {

    static final int[] LIVES_OPTIONS = {12, 10, 8, 6};
    static final String[] LIVES_NAMES = {"Merciful", "Grim", "Cruel", "Merciless"};

    int wordLength;
    int lives;
    boolean sound;

    static Settings load() {
        Settings s = new Settings();
        s.wordLength = Prefs.getInt("wordLength", 6);
        s.lives = Prefs.getInt("lives", 10);
        s.sound = Prefs.getBoolean("sound", true);
        return s;
    }

    void save() {
        Prefs.putInt("wordLength", wordLength);
        Prefs.putInt("lives", lives);
        Prefs.putBoolean("sound", sound);
    }
}
