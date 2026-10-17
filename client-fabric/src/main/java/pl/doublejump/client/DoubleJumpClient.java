package pl.doublejump.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.doublejump.client.net.HelloPayload;
import pl.doublejump.client.net.JumpPayload;

/**
 * Punkt wejścia modyfikacji DoubleJump (strona KLIENTA).
 */
public final class DoubleJumpClient implements ClientModInitializer {

    public static final String MOD_ID = "doublejump";
    public static final Logger LOGGER = LoggerFactory.getLogger("DoubleJump");

    private static final DoubleJumpHandler HANDLER = new DoubleJumpHandler();

    private static KeyMapping triggerKey;
    private static KeyMapping settingsKey;
    private static boolean errorLogged;

    @Override
    public void onInitializeClient() {
        DoubleJumpConfig.load();

        // Kanały sieciowe (używane tylko na serwerach z pluginem DoubleJump).
        PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(JumpPayload.TYPE, JumpPayload.CODEC);

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        // Domyślnie nieprzypisany: wtedy aktywuje klawisz skoku z ustawień gry (domyślnie SPACJA).
        // Nie wiążemy własnego klawisza ze SPACJĄ, bo kolidowałby ze zwykłym skokiem.
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.doublejump.trigger", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.doublejump.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_J, category));

        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE,
                (payload, context) -> ServerSupport.onHello(payload));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerSupport.reset();
            HANDLER.reset();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerSupport.reset();
            HANDLER.reset();
        });

        ClientTickEvents.END_CLIENT_TICK.register(DoubleJumpClient::onTick);
    }

    /** Klawisz aktywujący: własny, jeśli przypisany, w przeciwnym razie zwykły klawisz skoku. */
    public static KeyMapping triggerKey(Minecraft mc) {
        if (triggerKey != null && !triggerKey.isUnbound()) {
            return triggerKey;
        }
        return mc.options.keyJump;
    }

    private static void onTick(Minecraft mc) {
        try {
            // Opróżniamy kolejki kliknięć, żeby się nie kumulowały.
            while (triggerKey.consumeClick()) {
                // celowo pusta pętla: samo zużycie zdarzeń
            }
            while (settingsKey.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new DoubleJumpConfigScreen(null));
                }
            }
            HANDLER.tick(mc, triggerKey(mc));
        } catch (RuntimeException e) {
            // Mod nie może wywołać crasha gry - w razie błędu tylko logujemy (raz) i resetujemy stan.
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.error("Błąd w DoubleJump (kolejne błędy nie będą logowane):", e);
            }
            HANDLER.reset();
        }
    }
}
