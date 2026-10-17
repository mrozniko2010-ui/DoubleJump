package pl.doublejump.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Menu ustawień moda (otwierane klawiszem "Otwórz ustawienia DoubleJump", domyślnie J).
 */
public final class DoubleJumpConfigScreen extends Screen {

    private final Screen parent;
    private int hintY;

    public DoubleJumpConfigScreen(Screen parent) {
        super(Component.translatable("doublejump.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        DoubleJumpConfig config = DoubleJumpConfig.get();
        int x = this.width / 2 - 100;
        int y = this.height / 4;

        this.addRenderableWidget(Button.builder(label("doublejump.option.enabled", config.enabled), button -> {
            config.enabled = !config.enabled;
            button.setMessage(label("doublejump.option.enabled", config.enabled));
        }).bounds(x, y, 200, 20).build());

        this.addRenderableWidget(new StrengthSlider(x, y + 24, 200, 20, config));

        this.addRenderableWidget(Button.builder(label("doublejump.option.particles", config.particles), button -> {
            config.particles = !config.particles;
            button.setMessage(label("doublejump.option.particles", config.particles));
        }).bounds(x, y + 48, 200, 20).build());

        this.addRenderableWidget(Button.builder(label("doublejump.option.sound", config.sound), button -> {
            config.sound = !config.sound;
            button.setMessage(label("doublejump.option.sound", config.sound));
        }).bounds(x, y + 72, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(x, y + 108, 200, 20).build());

        this.hintY = y + 136;
    }

    private static Component label(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        Component hint = Component.translatable("doublejump.screen.hint",
                DoubleJumpClient.triggerKey(Minecraft.getInstance()).getTranslatedKeyMessage());
        graphics.drawCenteredString(this.font, hint, this.width / 2, this.hintY, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        DoubleJumpConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void removed() {
        DoubleJumpConfig.save();
    }

    /** Suwak siły skoku w zakresie MIN_STRENGTH..MAX_STRENGTH. */
    private static final class StrengthSlider extends AbstractSliderButton {

        private final DoubleJumpConfig config;

        StrengthSlider(int x, int y, int width, int height, DoubleJumpConfig config) {
            super(x, y, width, height, Component.empty(), toSlider(config.strength));
            this.config = config;
            this.updateMessage();
        }

        private static double toSlider(double strength) {
            return (strength - DoubleJumpConfig.MIN_STRENGTH)
                    / (DoubleJumpConfig.MAX_STRENGTH - DoubleJumpConfig.MIN_STRENGTH);
        }

        private static double fromSlider(double slider) {
            return DoubleJumpConfig.MIN_STRENGTH
                    + slider * (DoubleJumpConfig.MAX_STRENGTH - DoubleJumpConfig.MIN_STRENGTH);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.translatable("doublejump.option.strength",
                    String.format(Locale.ROOT, "%.2f", fromSlider(this.value))));
        }

        @Override
        protected void applyValue() {
            this.config.setStrength(fromSlider(this.value));
        }
    }
}
