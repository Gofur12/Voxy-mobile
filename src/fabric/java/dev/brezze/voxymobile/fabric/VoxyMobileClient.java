package dev.brezze.voxymobile.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Fabric client entry point. Minecraft objects are only read on the client thread. */
public final class VoxyMobileClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientChunkEvents.CHUNK_LOAD.register(VoxyChunkBridge::onChunkLoad);
        ClientChunkEvents.CHUNK_UNLOAD.register(VoxyChunkBridge::onChunkUnload);
        ClientTickEvents.END_CLIENT_TICK.register(client -> VoxyChunkBridge.flushPendingChanges());
        WorldRenderEvents.LAST.register(VoxyRenderBridge::render);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // A previous disconnect disables rendering; explicitly restore it for the next session.
            VoxyRenderBridge.enable();
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
