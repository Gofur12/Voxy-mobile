package dev.brezze.voxymobile.fabric;

import net.minecraft.client.MinecraftClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small persistent client configuration for the in-game Voxy Mobile screen. */
public final class VoxyMobileConfig {
    public static boolean enabled = false;
    public static boolean lodEnabled = true;
    /** Search radius in chunks; selectable values are 8, 16 and 24. */
    public static int radiusChunks = 16;
    private static Path file() { return MinecraftClient.getInstance().runDirectory.toPath().resolve("config/voxymobile.properties"); }
    private VoxyMobileConfig() {}

    public static void load() {
        if (!Files.isRegularFile(file())) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file())) {
            p.load(in);
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "false"));
            lodEnabled = Boolean.parseBoolean(p.getProperty("lodEnabled", "true"));
            int radius = Integer.parseInt(p.getProperty("radiusChunks", "16"));
            radiusChunks = radius == 8 || radius == 24 ? radius : 16;
        } catch (IOException | NumberFormatException e) {
            System.err.println("[VoxyMobile] Could not load config: " + e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(file().getParent());
            Properties p = new Properties();
            p.setProperty("enabled", Boolean.toString(enabled));
            p.setProperty("lodEnabled", Boolean.toString(lodEnabled));
            p.setProperty("radiusChunks", Integer.toString(radiusChunks));
            try (OutputStream out = Files.newOutputStream(file())) { p.store(out, "Voxy Mobile client settings"); }
        } catch (IOException e) {
            System.err.println("[VoxyMobile] Could not save config: " + e);
        }
    }
}
