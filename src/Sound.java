import javax.sound.sampled.*;
import java.util.Random;

/*
 * All sound is generated in code - no audio files. A low drone with wind, a thud for a miss,
 * a bell for a hit, a heartbeat on the last life, and endings for winning and losing.
 * If the computer has no audio device, the game simply stays silent.
 */
final class Sound {

    private static final float RATE = 22050f;
    private static final double TAU = Math.PI * 2;

    private static Clip drone;
    private static Clip thud;
    private static Clip bell;
    private static Clip heartbeat;
    private static Clip doom;
    private static Clip relief;
    private static boolean enabled;
    private static boolean heartbeatOn;
    private static volatile boolean ready;

    private Sound() {
    }

    /* Builds the sounds in the background so the window opens instantly. */
    static void init(boolean startEnabled) {
        enabled = startEnabled;
        if (Boolean.getBoolean("evilhangman.silent")) {
            enabled = false; // For automated runs: -Devilhangman.silent=true
            return;
        }
        Thread loader = new Thread(() -> {
            try {
                drone = clip(drone());
                thud = clip(thud());
                bell = clip(bell());
                heartbeat = clip(heartbeat());
                doom = clip(doom());
                relief = clip(relief());
                ready = true;
                if (enabled) {
                    loop(drone);
                }
            } catch (Throwable t) {
                ready = false; // No audio available; play on in silence.
            }
        }, "sound-loader");
        loader.setDaemon(true);
        loader.start();
    }

    static boolean isEnabled() {
        return enabled;
    }

    static void setEnabled(boolean on) {
        enabled = on;
        if (!ready) {
            return;
        }
        if (on) {
            loop(drone);
            if (heartbeatOn) {
                loop(heartbeat);
            }
        } else {
            for (Clip c : new Clip[]{drone, thud, bell, heartbeat, doom, relief}) {
                stop(c);
            }
        }
    }

    static void miss() {
        play(thud);
    }

    static void hit() {
        play(bell);
    }

    static void lose() {
        heartbeat(false);
        play(doom);
    }

    static void win() {
        heartbeat(false);
        play(relief);
    }

    static void heartbeat(boolean on) {
        if (on == heartbeatOn) {
            return;
        }
        heartbeatOn = on;
        if (!ready || !enabled) {
            return;
        }
        if (on) {
            loop(heartbeat);
        } else {
            stop(heartbeat);
        }
    }

    private static void play(Clip c) {
        if (!ready || !enabled || c == null) {
            return;
        }
        try {
            c.stop();
            c.setFramePosition(0);
            c.start();
        } catch (RuntimeException ignored) {
            // Skip this sound.
        }
    }

    private static void loop(Clip c) {
        if (c == null) {
            return;
        }
        try {
            c.setFramePosition(0);
            c.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (RuntimeException ignored) {
            // Skip this sound.
        }
    }

    private static void stop(Clip c) {
        if (c != null) {
            try {
                c.stop();
            } catch (RuntimeException ignored) {
                // Already stopped.
            }
        }
    }

    private static Clip clip(double[] samples) throws LineUnavailableException {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, samples[i])) * 32767);
            pcm[i * 2] = (byte) v;
            pcm[i * 2 + 1] = (byte) (v >> 8);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        Clip c = AudioSystem.getClip();
        c.open(format, pcm, 0, pcm.length);
        return c;
    }

    private static double[] buffer(double seconds) {
        return new double[(int) (RATE * seconds)];
    }

    /* 8-second seamless loop: two slightly detuned low tones beating against each other, plus wind. */
    private static double[] drone() {
        double seconds = 8;
        double[] out = buffer(seconds);
        int n = out.length;
        int fade = (int) (RATE * 1.5);
        double[] wind = lowNoise(n + fade, 0.02, 3);
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            // Frequencies chosen to complete whole cycles in 8 seconds, so the loop has no click.
            double tone = Math.sin(TAU * 55 * t) * 0.5 + Math.sin(TAU * 55.5 * t) * 0.5
                    + Math.sin(TAU * 82.5 * t) * 0.25 + Math.sin(TAU * 110.125 * t) * 0.08;
            double w = i < fade ? lerp(wind[n + i], wind[i], i / (double) fade) : wind[i];
            double swell = 0.6 + 0.4 * Math.sin(TAU * t / seconds);
            out[i] = tone * 0.07 + w * 0.35 * swell;
        }
        return out;
    }

    private static double[] thud() {
        double[] out = buffer(0.9);
        double[] noise = lowNoise(out.length, 0.08, 9);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / RATE;
            double freq = 42 + 38 * Math.exp(-t * 9);
            phase += TAU * freq / RATE;
            out[i] = Math.sin(phase) * Math.exp(-t * 5) * 0.9 + noise[i] * Math.exp(-t * 25) * 0.9;
        }
        return out;
    }

    /* An out-of-tune bell: inharmonic partials, like something old and cracked. */
    private static double[] bell() {
        double[] out = buffer(2.2);
        double base = 311.1;
        double[] ratios = {1, 2.41, 2.76, 5.4};
        double[] amps = {0.5, 0.22, 0.18, 0.08};
        for (int i = 0; i < out.length; i++) {
            double t = i / RATE;
            double v = 0;
            for (int p = 0; p < ratios.length; p++) {
                v += Math.sin(TAU * base * ratios[p] * t) * amps[p] * Math.exp(-t * (1.6 + p));
            }
            out[i] = v * 0.32 * Math.min(1, t * 200);
        }
        return out;
    }

    private static double[] heartbeat() {
        double[] out = buffer(1.1);
        addThump(out, 0.0, 0.9);
        addThump(out, 0.27, 0.65);
        return out;
    }

    private static void addThump(double[] out, double start, double gain) {
        int s = (int) (start * RATE);
        for (int i = s; i < out.length; i++) {
            double t = (i - s) / RATE;
            out[i] += Math.sin(TAU * 48 * t) * Math.exp(-t * 16) * gain * Math.min(1, t * 400);
        }
    }

    /* A long, falling, distorted toll for losing. */
    private static double[] doom() {
        double[] out = buffer(4.5);
        double[] noise = lowNoise(out.length, 0.01, 21);
        double phase1 = 0;
        double phase2 = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / RATE;
            double bend = Math.exp(-t * 0.25);
            phase1 += TAU * 65 * bend / RATE;
            phase2 += TAU * 97.7 * bend / RATE;
            double v = Math.sin(phase1) * 0.6 + Math.sin(phase2) * 0.35 + Math.sin(phase1 * 3.01) * 0.12;
            v = Math.tanh(v * 1.8) * 0.55;
            out[i] = (v + noise[i] * 0.6) * Math.exp(-t * 0.8) * Math.min(1, t * 60);
        }
        return out;
    }

    /* A quiet, uneasy three-note phrase for surviving. Not quite happy. */
    private static double[] relief() {
        double[] out = buffer(3.2);
        double[] notes = {293.7, 349.2, 415.3};
        for (int n = 0; n < notes.length; n++) {
            int s = (int) (n * 0.38 * RATE);
            for (int i = s; i < out.length; i++) {
                double t = (i - s) / RATE;
                double v = Math.sin(TAU * notes[n] * t) + Math.sin(TAU * notes[n] * 2.003 * t) * 0.3;
                out[i] += v * 0.16 * Math.exp(-t * 1.4) * Math.min(1, t * 80);
            }
        }
        return out;
    }

    /* Low-passed white noise, normalised to roughly -1..1. */
    private static double[] lowNoise(int length, double smoothing, long seed) {
        Random r = new Random(seed);
        double[] out = new double[length];
        double y = 0;
        double max = 1e-9;
        for (int i = 0; i < length; i++) {
            y += smoothing * ((r.nextDouble() * 2 - 1) - y);
            out[i] = y;
            max = Math.max(max, Math.abs(y));
        }
        for (int i = 0; i < length; i++) {
            out[i] /= max;
        }
        return out;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
