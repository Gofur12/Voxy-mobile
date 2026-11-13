package dev.brezze.voxymobile.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Fabric client entry point. Minecraft objects are only read on the client thread. */
public final class VoxyMobileClient implements ClientModInitializer {
    private static KeyBinding openSettings;
    @Override
    public void onInitializeClient() {
        VoxyMobileConfig.load();
        openSettings = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.voxymobile.settings",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.voxymobile"));
        // Touch-friendly entry point: expose settings directly on the pause menu.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GameMenuScreen) {
                int buttonWidth = Math.min(200, scaledWidth - 20);
                int x = (scaledWidth - buttonWidth) / 2;
                int y = Math.max(8, scaledHeight - 34);
                screen.addDrawableChild(ButtonWidget.builder(Text.literal("Voxy Mobile Settings"),
                        button -> client.setScreen(new VoxyMobileConfigScreen(screen)))
                        .dimensions(x, y, buttonWidth, 20).build());
            }
        });
        ClientChunkEvents.CHUNK_LOAD.register(VoxyChunkBridge::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(VoxyChunkBridge::onChunkUnload);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            VoxyChunkBridge.flushPendingChanges();
            while (openSettings.wasPressed()) client.setScreen(new VoxyMobileConfigScreen(client.currentScreen));
        });
        VoxyRenderBridge.setEnabled(VoxyMobileConfig.enabled);
        WorldRenderEvents.LAST.register(VoxyRenderBridge::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Respect the saved toggle; never auto-enable the experimental renderer.
            VoxyRenderBridge.setEnabled(VoxyMobileConfig.enabled);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VoxyRenderBridge.disable();
            // GL objects must be destroyed on the client/render thread, not from a worker.
            client.execute(() -> {
                VoxyRenderBridge.detach();
                VoxyChunkBridge.clear();
            });
        });
    }
}
