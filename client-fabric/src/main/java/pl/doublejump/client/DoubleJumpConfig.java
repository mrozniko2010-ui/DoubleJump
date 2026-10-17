package pl.doublejump.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Ustawienia moda zapisywane w {@code config/doublejump.json}.
 */
public final class DoubleJumpConfig {

    public static final double MIN_STRENGTH = 0.20D;
    public static final double MAX_STRENGTH = 0.80D;
    public static final double DEFAULT_STRENGTH = 0.42D; // tyle co zwykły skok w Minecrafcie

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DoubleJumpConfig instance = new DoubleJumpConfig();

    public boolean enabled = true;
    public double strength = DEFAULT_STRENGTH;
    public boolean particles = true;
    public boolean sound = true;

    public static synchronized DoubleJumpConfig get() {
        return instance;
    }

    public void setStrength(double value) {
        this.strength = clampStrength(value);
    }

    private void sanitize() {
        this.strength = clampStrength(this.strength);
    }

    private static double clampStrength(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_STRENGTH;
        }
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, value));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("doublejump.json");
    }

    /** Wczytuje ustawienia; przy błędzie używa wartości domyślnych i nigdy nie rzuca wyjątku. */
    public static synchronized void load() {
        Path file = path();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                DoubleJumpConfig loaded = GSON.fromJson(reader, DoubleJumpConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    instance = loaded;
                }
            } catch (IOException | JsonParseException | IllegalStateException e) {
                DoubleJumpClient.LOGGER.warn("Nie udało się wczytać {} - używam ustawień domyślnych.", file, e);
                instance = new DoubleJumpConfig();
            }
        }
        save();
    }

    /** Zapisuje ustawienia; błąd zapisu jest tylko logowany. */
    public static synchronized void save() {
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(instance), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DoubleJumpClient.LOGGER.warn("Nie udało się zapisać {}.", file, e);
        }
    }
}
