package dev.brezze.voxymobile.fabric;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Touch-friendly settings screen; buttons are deliberately wide for Android. */
public final class VoxyMobileConfigScreen extends Screen {
    private final Screen parent;
    private ButtonWidget masterButton;
    private ButtonWidget lodButton;
    private ButtonWidget radiusButton;

    public VoxyMobileConfigScreen(Screen parent) {
        super(Text.literal("Voxy Mobile Settings"));
        this.parent = parent;
    }

    @Override protected void init() {
        int width = Math.min(320, this.width - 24);
        int x = (this.width - width) / 2;
        int y = Math.max(36, this.height / 2 - 78);
        masterButton = addDrawableChild(ButtonWidget.builder(masterText(), b -> {
            VoxyMobileConfig.enabled = !VoxyMobileConfig.enabled;
            apply(); refreshLabels();
        }).dimensions(x, y, width, 24).build());
        lodButton = addDrawableChild(ButtonWidget.builder(lodText(), b -> {
            VoxyMobileConfig.lodEnabled = !VoxyMobileConfig.lodEnabled;
            apply(); refreshLabels();
        }).dimensions(x, y + 32, width, 24).build());
        radiusButton = addDrawableChild(ButtonWidget.builder(radiusText(), b -> {
            VoxyMobileConfig.radiusChunks = VoxyMobileConfig.radiusChunks == 8 ? 16
                    : VoxyMobileConfig.radiusChunks == 16 ? 24 : 8;
            VoxyMobileConfig.save(); refreshLabels();
        }).dimensions(x, y + 64, width, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset default"), b -> {
            VoxyMobileConfig.enabled = true;
            VoxyMobileConfig.lodEnabled = true;
            VoxyMobileConfig.radiusChunks = 16;
            apply(); refreshLabels();
        }).dimensions(x, y + 100, width, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(x, y + 132, width, 24).build());
    }

    private Text masterText() { return Text.literal("Voxy Mobile: " + (VoxyMobileConfig.enabled ? "ON" : "OFF")); }
    private Text lodText() { return Text.literal("Distant LOD: " + (VoxyMobileConfig.lodEnabled ? "ON" : "OFF")); }
    private Text radiusText() { return Text.literal("LOD radius: " + VoxyMobileConfig.radiusChunks + " chunks"); }
    private void refreshLabels() {
        if (masterButton != null) masterButton.setMessage(masterText());
        if (lodButton != null) lodButton.setMessage(lodText());
        if (radiusButton != null) radiusButton.setMessage(radiusText());
    }
    private void apply() {
        VoxyMobileConfig.save();
        VoxyRenderBridge.setEnabled(VoxyMobileConfig.enabled);
    }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, Math.max(12, height / 2 - 112), 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Open from Pause Menu > Voxy Mobile Settings (or press O)"), width / 2, height - 28, 0xAAAAAA);
    }
}
