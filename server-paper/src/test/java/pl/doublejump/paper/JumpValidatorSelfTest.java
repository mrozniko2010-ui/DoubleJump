package pl.doublejump.paper;

import pl.doublejump.paper.JumpValidator.Facts;
import pl.doublejump.paper.JumpValidator.PlayerState;
import pl.doublejump.paper.JumpValidator.Settings;
import pl.doublejump.paper.JumpValidator.Verdict;

/** Samodzielny test walidatora serwera (bez Bukkita i bez JUnit). */
public final class JumpValidatorSelfTest {

    private static int failures;
    private static int checks;

    private static final Settings S = new Settings(true, 0.6F, 4, 250L);
    private static final Facts AIR = new Facts(true, false, false, false);
    private static final Facts GROUND = new Facts(true, false, false, true);

    private static void check(boolean ok, String msg) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("BŁĄD: " + msg);
        }
    }

    private static PlayerState airborne(int ticks) {
        PlayerState st = new PlayerState();
        st.advance(GROUND);
        for (int i = 0; i < ticks; i++) {
            st.advance(AIR);
        }
        return st;
    }

    public static void main(String[] args) {
        PlayerState st = airborne(8);
        check(JumpValidator.validate(S, st, AIR, 10_000, 0.42F) == Verdict.OK, "poprawne zgłoszenie");
        check(JumpValidator.validate(S, st, AIR, 20_000, 0.42F) == Verdict.ALREADY_USED, "drugie zgłoszenie w tym samym locie");

        st.advance(GROUND);
        for (int i = 0; i < 8; i++) {
            st.advance(AIR);
        }
        check(JumpValidator.validate(S, st, AIR, 30_000, 0.42F) == Verdict.OK, "po lądowaniu ponownie OK");

        check(JumpValidator.validate(S, airborne(8), GROUND, 1_000, 0.4F) == Verdict.SUPPORTED, "na ziemi odrzucone");
        check(JumpValidator.validate(S, airborne(1), AIR, 1_000, 0.4F) == Verdict.TOO_EARLY, "zbyt wcześnie po opuszczeniu ziemi");
        check(JumpValidator.validate(S, airborne(2), AIR, 1_000, 0.4F) == Verdict.OK, "margines tolerancji 2 ticków");
        check(JumpValidator.validate(S, airborne(8), new Facts(false, false, false, false), 1_000, 0.4F) == Verdict.NO_PERMISSION, "brak uprawnienia");
        check(JumpValidator.validate(S, airborne(8), new Facts(true, true, false, false), 1_000, 0.4F) == Verdict.WORLD_DISABLED, "świat wyłączony");
        check(JumpValidator.validate(S, airborne(8), new Facts(true, false, true, false), 1_000, 0.4F) == Verdict.FORBIDDEN_STATE, "zabroniony stan (widz/elytry/latanie)");
        check(JumpValidator.validate(new Settings(false, 0.6F, 4, 250L), airborne(8), AIR, 1_000, 0.4F) == Verdict.DISABLED, "plugin wyłączony");
        check(JumpValidator.validate(S, airborne(8), AIR, 1_000, Float.NaN) == Verdict.BAD_STRENGTH, "NaN");
        check(JumpValidator.validate(S, airborne(8), AIR, 1_000, -1F) == Verdict.BAD_STRENGTH, "siła ujemna");

        PlayerState spam = airborne(8);
        int ok = 0;
        for (int i = 0; i < 500; i++) {
            if (JumpValidator.validate(S, spam, AIR, 1_000_000L + i * 10L, 0.4F) == Verdict.OK) {
                ok++;
            }
        }
        check(ok == 1, "spam zgłoszeń daje dokładnie jeden skok, było " + ok);

        PlayerState cd = airborne(8);
        JumpValidator.validate(S, cd, AIR, 5_000, 0.4F);
        cd.advance(GROUND);
        for (int i = 0; i < 8; i++) {
            cd.advance(AIR);
        }
        check(JumpValidator.validate(S, cd, AIR, 5_100, 0.4F) == Verdict.COOLDOWN, "cooldown między zgłoszeniami");
        check(JumpValidator.validate(S, cd, AIR, 5_300, 0.4F) == Verdict.OK, "po cooldownie OK");

        check(JumpValidator.clampStrength(5F, 0.6F) == 0.6F, "przycięcie do max serwera");
        check(JumpValidator.clampStrength(0.01F, 0.6F) == JumpValidator.MIN_STRENGTH, "przycięcie do min");

        System.out.println(checks + " sprawdzeń, błędów: " + failures);
        if (failures > 0) {
            System.exit(1);
        }
    }
}
