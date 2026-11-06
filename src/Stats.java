/* The player's win/loss record and streaks. Remembered between launches. */
final class Stats {

    int wins;
    int losses;
    int streak;
    int bestStreak;

    static Stats load() {
        Stats s = new Stats();
        s.wins = Prefs.getInt("wins", 0);
        s.losses = Prefs.getInt("losses", 0);
        s.streak = Prefs.getInt("streak", 0);
        s.bestStreak = Prefs.getInt("bestStreak", 0);
        return s;
    }

    void record(boolean won) {
        if (won) {
            wins++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
        } else {
            losses++;
            streak = 0;
        }
        Prefs.putInt("wins", wins);
        Prefs.putInt("losses", losses);
        Prefs.putInt("streak", streak);
        Prefs.putInt("bestStreak", bestStreak);
    }

    String summary() {
        if (wins + losses == 0) {
            return "No one has been saved yet";
        }
        return "Saved " + wins + "   ·   Hanged " + losses
                + "   ·   Streak " + streak + "   ·   Best " + bestStreak;
    }
}
