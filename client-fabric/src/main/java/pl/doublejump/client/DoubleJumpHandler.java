package pl.doublejump.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import pl.doublejump.client.net.JumpPayload;

/**
 * Łączy stan gry (gracz, klawisze, serwer) z czystą maszyną stanów {@link JumpStateMachine}.
 */
final class DoubleJumpHandler {

    private final JumpStateMachine machine = new JumpStateMachine();
    private boolean wasDown;

    void reset() {
        machine.reset();
        wasDown = false;
    }

    /**
     * @param trigger klawisz aktywacji (własny, a jeśli nieprzypisany - zwykły klawisz skoku)
     */
    void tick(Minecraft mc, KeyMapping trigger) {
        boolean down = trigger.isDown();
        boolean fresh = down && !wasDown;
        wasDown = down;

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            machine.reset();
            return;
        }

        DoubleJumpConfig config = DoubleJumpConfig.get();
        boolean local = mc.getSingleplayerServer() != null;

        // Oparcie: ziemia, woda, lawa, drabina/winorośl/rusztowanie. Tu skok się odnawia,
        // a w wodzie i na drabinie drugi skok nigdy się nie aktywuje.
        boolean supported = player.onGround()
                || player.isInWater()
                || player.isInLava()
                || player.onClimbable();

        boolean blocked = player.isSpectator()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.isPassenger()
                || player.isSleeping()
                || !player.isAlive();

        boolean allowed = config.enabled
                && mc.screen == null
                && (local || ServerSupport.isActive());

        int minAir = JumpStateMachine.MIN_AIR_TICKS;
        if (!local) {
            minAir = Math.max(minAir, ServerSupport.minAirTicks());
        }

        JumpStateMachine.Result result = machine.tick(supported, blocked, fresh, allowed, minAir);
        if (result != JumpStateMachine.Result.DOUBLE_JUMP) {
            return;
        }

        double strength = config.strength;
        if (!local) {
            strength = Math.min(strength, ServerSupport.maxStrength());
        }

        applyImpulse(player, strength);

        if (!local) {
            try {
                ClientPlayNetworking.send(new JumpPayload((float) strength));
            } catch (RuntimeException e) {
                DoubleJumpClient.LOGGER.debug("Nie udało się wysłać zgłoszenia skoku do serwera.", e);
            }
        }

        if (config.particles) {
            spawnParticles(mc, player);
        }
        if (config.sound) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BAT_TAKEOFF, 1.3F, 0.6F));
        }
    }

    private static void applyImpulse(LocalPlayer player, double strength) {
        Vec3 motion = player.getDeltaMovement();
        // Ustawiamy prędkość pionową (zerujemy dotychczasowe opadanie) - poziomy pęd zostaje bez zmian.
        player.setDeltaMovement(motion.x, strength, motion.z);
    }

    private static void spawnParticles(Minecraft mc, LocalPlayer player) {
        RandomSource random = player.getRandom();
        int count = 14;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * i / count;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            mc.particleEngine.createParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + dx * 0.3D,
                    player.getY() + 0.1D,
                    player.getZ() + dz * 0.3D,
                    dx * 0.1D,
                    -0.02D + random.nextDouble() * 0.02D,
                    dz * 0.1D
            );
        }
    }
}
