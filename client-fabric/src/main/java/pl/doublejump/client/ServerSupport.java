package pl.doublejump.client;

import pl.doublejump.client.net.HelloPayload;

/**
 * Stan "czy bieżący serwer zadeklarował obsługę podwójnego skoku".
 * Na serwerze bez pluginu DoubleJump (np. vanilla) wartość pozostaje fałszywa,
 * a mod w multiplayerze nie wykonuje żadnego dodatkowego ruchu.
 */
public final class ServerSupport {

    private static volatile boolean active;
    private static volatile float maxStrength = (float) DoubleJumpConfig.DEFAULT_STRENGTH;
    private static volatile int minAirTicks = JumpStateMachine.MIN_AIR_TICKS;

    private ServerSupport() {
    }

    public static void reset() {
        active = false;
        maxStrength = (float) DoubleJumpConfig.DEFAULT_STRENGTH;
        minAirTicks = JumpStateMachine.MIN_AIR_TICKS;
    }

    public static void onHello(HelloPayload payload) {
        if (payload.version() != HelloPayload.PROTOCOL_VERSION) {
            DoubleJumpClient.LOGGER.warn("Serwer używa innej wersji protokołu DoubleJump ({}), ignoruję.", payload.version());
            reset();
            return;
        }
        float max = payload.maxStrength();
        if (Float.isNaN(max) || max < (float) DoubleJumpConfig.MIN_STRENGTH) {
            max = (float) DoubleJumpConfig.MIN_STRENGTH;
        }
        maxStrength = Math.min(max, (float) DoubleJumpConfig.MAX_STRENGTH);
        minAirTicks = Math.max(0, Math.min(40, payload.minAirTicks()));
        active = payload.enabled();
        DoubleJumpClient.LOGGER.info("Serwer zgłosił obsługę DoubleJump (włączony: {}, max siła: {}).", active, maxStrength);
    }

    public static boolean isActive() {
        return active;
    }

    public static float maxStrength() {
        return maxStrength;
    }

    public static int minAirTicks() {
        return minAirTicks;
    }

}
