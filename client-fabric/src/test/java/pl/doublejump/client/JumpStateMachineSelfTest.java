package pl.doublejump.client;

import pl.doublejump.client.JumpStateMachine.Result;

/**
 * Samodzielny test logiki podwójnego skoku (bez JUnit i bez Minecrafta).
 * Uruchomienie: patrz README ("Testy logiki").
 */
public final class JumpStateMachineSelfTest {

    private static int failures;
    private static int checks;

    private static final int MIN = JumpStateMachine.MIN_AIR_TICKS;

    public static void main(String[] args) {
        normalDoubleJump();
        onlyOneAirJump();
        landingRenews();
        pressTooEarlyIsIgnoredAndNotBuffered();
        walkingOffLedgeDoesNotTrigger();
        heldKeyDoesNotTrigger();
        waterAndLadderNeverTrigger();
        blockedStatesNeverTrigger();
        disabledOrNotAllowedNeverTriggers();
        spammingNeverGivesInfiniteJumps();
        serverMinAirTicksRespected();

        System.out.println(checks + " sprawdzeń, błędów: " + failures);
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("BŁĄD: " + message);
        }
    }

    /** Symuluje 'n' ticków w powietrzu bez naciskania klawisza. */
    private static void air(JumpStateMachine m, int n) {
        for (int i = 0; i < n; i++) {
            m.tick(false, false, false, true, MIN);
        }
    }

    private static Result press(JumpStateMachine m) {
        return m.tick(false, false, true, true, MIN);
    }

    private static void ground(JumpStateMachine m) {
        m.tick(true, false, false, true, MIN);
    }

    static void normalDoubleJump() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        air(m, 8);
        check(press(m) == Result.DOUBLE_JUMP, "drugi skok w powietrzu powinien się aktywować");
    }

    static void onlyOneAirJump() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        air(m, 8);
        press(m);
        air(m, 5);
        check(press(m) == Result.NONE, "trzeci skok (kolejny w powietrzu) nie może się aktywować");
    }

    static void landingRenews() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        air(m, 8);
        press(m);
        ground(m);
        air(m, 8);
        check(press(m) == Result.DOUBLE_JUMP, "po dotknięciu ziemi skok powinien się odnowić");
    }

    static void pressTooEarlyIsIgnoredAndNotBuffered() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        air(m, 1);
        check(press(m) == Result.NONE, "naciśnięcie zaraz po oderwaniu od ziemi musi być zignorowane");
        check(m.tick(false, false, false, true, MIN) == Result.NONE, "zignorowane naciśnięcie nie może być buforowane");
        air(m, 6);
        check(press(m) == Result.DOUBLE_JUMP, "po upływie czasu nowe naciśnięcie działa");
    }

    static void walkingOffLedgeDoesNotTrigger() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        // gracz schodzi z krawędzi i od razu (w ciągu MIN-1 ticków) naciska skok
        boolean triggered = false;
        for (int i = 0; i < MIN - 1; i++) {
            if (press(m) == Result.DOUBLE_JUMP) {
                triggered = true;
            }
        }
        check(!triggered, "naciśnięcie tuż po zejściu z krawędzi nie może uruchomić skoku");
    }

    static void heldKeyDoesNotTrigger() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        boolean triggered = false;
        for (int i = 0; i < 40; i++) {
            if (m.tick(false, false, false, true, MIN) == Result.DOUBLE_JUMP) {
                triggered = true;
            }
        }
        check(!triggered, "samo przytrzymanie (brak nowego naciśnięcia) nie aktywuje skoku");
    }

    static void waterAndLadderNeverTrigger() {
        JumpStateMachine m = new JumpStateMachine();
        air(m, 10);
        // 'supported' = woda / lawa / drabina - nigdy nie aktywuje i odnawia stan
        check(m.tick(true, false, true, true, MIN) == Result.NONE, "w wodzie / na drabinie skok nie działa");
        check(m.getAirTicks() == 0 && !m.isUsedInAir(), "woda / drabina odnawiają stan");
    }

    static void blockedStatesNeverTrigger() {
        JumpStateMachine m = new JumpStateMachine();
        air(m, 10);
        check(m.tick(false, true, true, true, MIN) == Result.NONE, "tryb widza / latanie / elytry blokują skok");
        // po wyjściu ze stanu blokady licznik czasu w powietrzu startuje od zera
        check(press(m) == Result.NONE, "tuż po stanie blokady obowiązuje opóźnienie");
    }

    static void disabledOrNotAllowedNeverTriggers() {
        JumpStateMachine m = new JumpStateMachine();
        air(m, 10);
        check(m.tick(false, false, true, false, MIN) == Result.NONE, "wyłączony mod / brak zgody serwera");
        check(!m.isUsedInAir(), "odrzucone naciśnięcie nie zużywa skoku");
        check(press(m) == Result.DOUBLE_JUMP, "po włączeniu skok działa");
    }

    static void spammingNeverGivesInfiniteJumps() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        int jumps = 0;
        for (int i = 0; i < 1000; i++) {
            if (press(m) == Result.DOUBLE_JUMP) {
                jumps++;
            }
        }
        check(jumps == 1, "w jednym locie dozwolony jest dokładnie jeden skok w powietrzu, było: " + jumps);
    }

    static void serverMinAirTicksRespected() {
        JumpStateMachine m = new JumpStateMachine();
        ground(m);
        for (int i = 0; i < 7; i++) {
            m.tick(false, false, false, true, 10);
        }
        check(m.tick(false, false, true, true, 10) == Result.NONE, "wymóg serwera (10 ticków) musi być respektowany");
        for (int i = 0; i < 3; i++) {
            m.tick(false, false, false, true, 10);
        }
        check(m.tick(false, false, true, true, 10) == Result.DOUBLE_JUMP, "po 10 tickach skok działa");
    }
}
