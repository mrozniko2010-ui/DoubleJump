package pl.doublejump.paper;

/**
 * Czysta logika walidacji zgłoszeń podwójnego skoku (bez zależności od Bukkita).
 * Wszystko, co dotyczy stanu gracza, przychodzi jako {@link Facts}.
 */
public final class JumpValidator {

    /** Margines (w tickach) na różnice w czasie między klientem a serwerem. */
    public static final int TICK_TOLERANCE = 2;

    public static final float MIN_STRENGTH = 0.20F;
    public static final float MAX_STRENGTH = 0.80F;

    private JumpValidator() {
    }

    public enum Verdict {
        OK,
        DISABLED,
        NO_PERMISSION,
        WORLD_DISABLED,
        FORBIDDEN_STATE,
        SUPPORTED,
        ALREADY_USED,
        COOLDOWN,
        TOO_EARLY,
        BAD_STRENGTH
    }

    /** Ustawienia serwera. */
    public record Settings(boolean enabled, float maxStrength, int minAirTicks, long cooldownMs) {
    }

    /** Aktualne fakty o graczu zebrane przez plugin. */
    public record Facts(boolean permitted, boolean worldDisabled, boolean forbiddenState, boolean supported) {
    }

    /** Stan jednego gracza śledzony po stronie serwera. */
    public static final class PlayerState {
        private boolean usedInAir;
        private int airTicks;
        private long lastRequestMs = Long.MIN_VALUE / 2;

        public boolean isUsedInAir() {
            return usedInAir;
        }

        public int getAirTicks() {
            return airTicks;
        }

        public void reset() {
            usedInAir = false;
            airTicks = 0;
        }

        /** Wywoływane co tick serwera dla każdego gracza. */
        public void advance(Facts facts) {
            if (facts.supported()) {
                usedInAir = false;
                airTicks = 0;
            } else if (facts.forbiddenState()) {
                airTicks = 0;
            } else if (airTicks < Integer.MAX_VALUE) {
                airTicks++;
            }
        }

        void markJump(long nowMs) {
            usedInAir = true;
            lastRequestMs = nowMs;
        }
    }

    /**
     * Sprawdza zgłoszenie. Przy {@link Verdict#OK} zapisuje wykorzystanie skoku w stanie gracza.
     */
    public static Verdict validate(Settings settings, PlayerState state, Facts facts, long nowMs, float requested) {
        if (!settings.enabled()) {
            return Verdict.DISABLED;
        }
        if (!facts.permitted()) {
            return Verdict.NO_PERMISSION;
        }
        if (facts.worldDisabled()) {
            return Verdict.WORLD_DISABLED;
        }
        if (facts.forbiddenState()) {
            return Verdict.FORBIDDEN_STATE;
        }
        if (facts.supported()) {
            return Verdict.SUPPORTED;
        }
        if (Float.isNaN(requested) || Float.isInfinite(requested) || requested <= 0.0F) {
            return Verdict.BAD_STRENGTH;
        }
        if (state.usedInAir) {
            return Verdict.ALREADY_USED;
        }
        if (nowMs - state.lastRequestMs < settings.cooldownMs()) {
            return Verdict.COOLDOWN;
        }
        if (state.airTicks < Math.max(0, settings.minAirTicks() - TICK_TOLERANCE)) {
            return Verdict.TOO_EARLY;
        }
        state.markJump(nowMs);
        return Verdict.OK;
    }

    /** Siła faktycznie zaakceptowana przez serwer (przycięta do limitów). */
    public static float clampStrength(float requested, float serverMax) {
        float max = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, serverMax));
        return Math.max(MIN_STRENGTH, Math.min(max, requested));
    }
}
