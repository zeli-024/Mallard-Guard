package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Acknowledge a config format update before existing values are migrated. */
public final class GuardConfigUpdateScreen extends Screen {
    private final Screen previous;

    public GuardConfigUpdateScreen(Screen previous) {
        super(Component.literal("Mallard Guard config update"));
        this.previous = previous;
    }

    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Update configs"), button -> {
            GuardConfig.respondToUpdate();
            Minecraft.getInstance().setScreen(previous);
        }).bounds(width / 2 - 70, height / 2 + 30, 140, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 34, 0xFFFFFF);
        graphics.drawCenteredString(font, "This update affects Mallard Guard configs.", width / 2, height / 2 - 8, 0xDDDDDD);
        graphics.drawCenteredString(font, "Your existing settings will be kept.", width / 2, height / 2 + 5, 0xDDDDDD);
    }

    @Override public void onClose() {
        // Config migration must be acknowledged before continuing.
    }
}
