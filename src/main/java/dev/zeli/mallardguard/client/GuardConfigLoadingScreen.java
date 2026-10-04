package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Wait for permission and the current world's settings before opening the editor. */
public final class GuardConfigLoadingScreen extends Screen {
    private final Screen parent;
    private boolean requested;

    public GuardConfigLoadingScreen(Screen parent) {
        super(Component.literal("Mallard Guard"));
        this.parent = parent;
    }

    public Screen parent() { return parent; }

    @Override protected void init() {
        addRenderableWidget(GuardUi.builder(Component.literal("Back"), b -> onClose())
            .bounds(width / 2 - 70, height / 2 + 24, 140, 20).build());
        if (!requested && Minecraft.getInstance().getConnection() != null) {
            requested = true;
            PacketDistributor.sendToServer(new GuardPackets.RequestSettings());
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.flush();renderBackground(graphics,mouseX,mouseY,delta);graphics.flush();for(var widget:renderables)widget.render(graphics,mouseX,mouseY,delta);

        graphics.drawCenteredString(font, "Loading Mallard Guard settings...", width / 2, height / 2, 0xFFFFFF);
    }

    @Override public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
