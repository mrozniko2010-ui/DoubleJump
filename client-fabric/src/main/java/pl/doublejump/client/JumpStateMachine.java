package pl.doublejump.client;

/**
 * Czysta logika podwójnego skoku - bez żadnych zależności od Minecrafta,
 * dzięki czemu można ją przetestować samodzielnie.
 *
 * <p>Wywoływana raz na tick klienta.</p>
 */
public final class JumpStateMachine {

    /** Wynik pojedynczego ticku. */
    public enum Result {
        NONE,
        DOUBLE_JUMP
    }

    /** Minimalna liczba ticków w powietrzu przed drugim skokiem (4 ticki = 0,2 s). */
    public static final int MIN_AIR_TICKS = 4;

    private boolean usedInAir;
    private int airTicks;

    /**
     * @param supported    gracz ma "oparcie": ziemia, woda, lawa, drabina/pnącze - odnawia skok
     * @param blocked      stan, w którym skok jest zablokowany (tryb widza, latanie, elytry, pojazd...)
     * @param freshPress   klawisz został NAPRAWDĘ naciśnięty w tym ticku (zbocze, nie przytrzymanie)
     * @param allowed      mod włączony i serwer (jeśli to multiplayer) zezwala na skok
     * @param minAirTicks  ile ticków gracz musi spędzić w powietrzu, zanim drugi skok będzie możliwy
     *                     (ochrona przed przypadkowym skokiem tuż po zejściu z krawędzi)
     */
    public Result tick(boolean supported, boolean blocked, boolean freshPress, boolean allowed, int minAirTicks) {
        if (supported) {
            usedInAir = false;
            airTicks = 0;
            return Result.NONE;
        }
        if (blocked) {
            airTicks = 0;
            return Result.NONE;
        }

        if (airTicks < Integer.MAX_VALUE) {
            airTicks++;
        }

        if (!freshPress || usedInAir || !allowed || airTicks < minAirTicks) {
            return Result.NONE;
        }

        usedInAir = true;
        return Result.DOUBLE_JUMP;
    }

    public void reset() {
        usedInAir = false;
        airTicks = 0;
    }

    public boolean isUsedInAir() {
        return usedInAir;
    }

    public int getAirTicks() {
        return airTicks;
    }
}
