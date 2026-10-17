package pl.doublejump.paper;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serwerowa część DoubleJump dla Paper 1.21.11.
 *
 * <p>Protokół (plugin messaging, wszystko big-endian jak w Minecrafcie):</p>
 * <ul>
 *   <li>{@code doublejump:hello} (serwer -> klient): VarInt wersja, boolean włączony,
 *       float maksymalna siła, VarInt minimalne ticki w powietrzu.</li>
 *   <li>{@code doublejump:jump} (klient -> serwer): float siła skoku. Serwer waliduje zgłoszenie.</li>
 * </ul>
 *
 * <p>Plugin niczego nie omija: nie modyfikuje antycheata ani cudzych pluginów.</p>
 */
public final class DoubleJumpPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    public static final String HELLO_CHANNEL = "doublejump:hello";
    public static final String JUMP_CHANNEL = "doublejump:jump";
    public static final int PROTOCOL_VERSION = 1;
    public static final String PERMISSION_USE = "doublejump.use";

    private final Map<UUID, JumpValidator.PlayerState> states = new HashMap<>();
    private final Set<String> disabledWorlds = new HashSet<>();

    private JumpValidator.Settings settings = new JumpValidator.Settings(true, 0.6F, 4, 250L);
    private boolean resyncOnReject;
    private boolean effectsForOthers = true;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getServer().getMessenger().registerOutgoingPluginChannel(this, HELLO_CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, JUMP_CHANNEL, this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, this::tickPlayers, 1L, 1L);
        getLogger().info("DoubleJump włączony. Oczekuję na klientów z modem DoubleJump.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        states.clear();
    }

    private void loadSettings() {
        reloadConfig();
        boolean enabled = getConfig().getBoolean("enabled", true);
        float max = (float) getConfig().getDouble("max-strength", 0.60D);
        if (Float.isNaN(max)) {
            max = 0.60F;
        }
        max = Math.max(JumpValidator.MIN_STRENGTH, Math.min(JumpValidator.MAX_STRENGTH, max));
        int minAir = Math.max(0, Math.min(40, getConfig().getInt("min-air-ticks", 4)));
        long cooldown = Math.max(0L, Math.min(5000L, getConfig().getLong("cooldown-ms", 250L)));
        settings = new JumpValidator.Settings(enabled, max, minAir, cooldown);

        disabledWorlds.clear();
        List<String> worlds = getConfig().getStringList("disabled-worlds");
        disabledWorlds.addAll(worlds);

        resyncOnReject = getConfig().getBoolean("resync-on-reject", false);
        effectsForOthers = getConfig().getBoolean("effects-for-others", true);
        debug = getConfig().getBoolean("debug", false);
    }

    // ------------------------------------------------------------------ fakty o graczu

    @SuppressWarnings("deprecation") // isOnGround() zależy od klienta - dlatego walidujemy też czas i stan
    private JumpValidator.Facts facts(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        boolean liquid = player.isInWater() || feet == Material.WATER || feet == Material.LAVA;
        boolean climbing = Tag.CLIMBABLE.isTagged(feet);
        boolean supported = player.isOnGround() || liquid || climbing;

        boolean forbidden = player.getGameMode() == GameMode.SPECTATOR
                || player.isFlying()
                || player.isGliding()
                || player.isInsideVehicle()
                || player.isSleeping()
                || player.isDead();

        boolean permitted = player.hasPermission(PERMISSION_USE);
        boolean worldDisabled = disabledWorlds.contains(player.getWorld().getName());
        return new JumpValidator.Facts(permitted, worldDisabled, forbidden, supported);
    }

    private JumpValidator.PlayerState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> new JumpValidator.PlayerState());
    }

    private void tickPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            state(player).advance(facts(player));
        }
    }

    // ------------------------------------------------------------------ zgłoszenia od klientów

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!JUMP_CHANNEL.equals(channel) || message == null || message.length != Float.BYTES) {
            return;
        }
        final float requested = ByteBuffer.wrap(message).getFloat();
        if (Bukkit.isPrimaryThread()) {
            handleJump(player, requested);
        } else {
            Bukkit.getScheduler().runTask(this, () -> handleJump(player, requested));
        }
    }

    private void handleJump(Player player, float requested) {
        if (!player.isOnline()) {
            return;
        }
        JumpValidator.PlayerState state = state(player);
        JumpValidator.Verdict verdict = JumpValidator.validate(
                settings, state, facts(player), System.currentTimeMillis(), requested);

        if (verdict != JumpValidator.Verdict.OK) {
            if (debug) {
                getLogger().info("Odrzucono skok gracza " + player.getName() + ": " + verdict);
            }
            if (resyncOnReject) {
                // Cofnięcie do ostatniej pozycji znanej serwerowi. Nie omija żadnych zabezpieczeń.
                player.teleport(player.getLocation());
            }
            return;
        }

        if (debug) {
            float accepted = JumpValidator.clampStrength(requested, settings.maxStrength());
            getLogger().info("Zaakceptowano skok gracza " + player.getName() + " (siła " + accepted + ").");
        }
        if (effectsForOthers) {
            showEffectsToOthers(player);
        }
    }

    private void showEffectsToOthers(Player jumper) {
        Location location = jumper.getLocation();
        for (Player other : jumper.getWorld().getNearbyPlayers(location, 32.0D)) {
            if (other.equals(jumper)) {
                continue; // skaczący gracz ma własne efekty z klienta
            }
            other.spawnParticle(Particle.CLOUD, location, 10, 0.25D, 0.05D, 0.25D, 0.02D);
            other.playSound(location, Sound.ENTITY_BAT_TAKEOFF, 0.6F, 1.3F);
        }
    }

    // ------------------------------------------------------------------ handshake

    private byte[] buildHello(boolean allowed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(16);
        writeVarInt(out, PROTOCOL_VERSION);
        out.write(allowed ? 1 : 0);
        int bits = Float.floatToIntBits(settings.maxStrength());
        out.write((bits >>> 24) & 0xFF);
        out.write((bits >>> 16) & 0xFF);
        out.write((bits >>> 8) & 0xFF);
        out.write(bits & 0xFF);
        writeVarInt(out, settings.minAirTicks());
        return out.toByteArray();
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    private void sendHello(Player player) {
        if (!player.getListeningPluginChannels().contains(HELLO_CHANNEL)) {
            return; // klient bez moda DoubleJump - nic nie wysyłamy
        }
        boolean allowed = settings.enabled()
                && player.hasPermission(PERMISSION_USE)
                && !disabledWorlds.contains(player.getWorld().getName());
        player.sendPluginMessage(this, HELLO_CHANNEL, buildHello(allowed));
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (HELLO_CHANNEL.equals(event.getChannel())) {
            sendHello(event.getPlayer());
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        state(event.getPlayer()).reset();
        sendHello(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        state(event.getPlayer()).reset();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------------ komenda

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendHello(player);
            }
            sender.sendMessage("DoubleJump: konfiguracja przeładowana.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("DoubleJump: włączony=" + settings.enabled()
                    + ", max siła=" + settings.maxStrength()
                    + ", min ticki w powietrzu=" + settings.minAirTicks()
                    + ", cooldown=" + settings.cooldownMs() + " ms"
                    + ", wyłączone światy=" + disabledWorlds);
            return true;
        }
        sender.sendMessage("Użycie: /" + label + " <reload|status>");
        return true;
    }
}
