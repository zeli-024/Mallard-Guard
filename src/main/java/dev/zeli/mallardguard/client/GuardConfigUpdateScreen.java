package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.zeli.mallardguard.GuardPackets;

/** Explain config migrations when the player opens Mallard Guard settings. */
public final class GuardConfigUpdateScreen extends Screen {
    private final Screen parent;
    private final List<String> changes;
    private final List<Button> themedButtons = new ArrayList<>();
    private final boolean remoteServer;
    private int scroll;
    private boolean failed;

    public GuardConfigUpdateScreen(Screen parent) {
        super(Component.literal("Mallard Guard config update"));
        this.parent = parent;
        this.changes = GuardConfig.pendingChanges();
        this.remoteServer = false;
    }

    public GuardConfigUpdateScreen(Screen parent, String serverChanges) {
        super(Component.literal("Mallard Guard config update"));
        this.parent = parent;
        this.remoteServer = true;
        this.changes = new ArrayList<>();
        this.changes.add("World/server settings");
        if (serverChanges.isBlank()) this.changes.add("Config format changed; settings remain available.");
        else this.changes.addAll(List.of(serverChanges.split("\\n")));
    }

    @Override protected void init() {
        themedButtons.clear();
        int center = width / 2;
        int available = Math.max(100, width - 32);
        int buttonWidth = Math.min(156, (available - 8) / 2);
        int bottom = height - 52;
        themedButtons.add(addRenderableWidget(Button.builder(Component.literal(buttonWidth < 116 ? "Keep values" : "Keep my values"), button -> finish(true))
            .bounds(center - buttonWidth - 4, bottom, buttonWidth, 20).build()));
        themedButtons.add(addRenderableWidget(Button.builder(Component.literal(buttonWidth < 116 ? "Reset" : "Use new defaults"), button -> finish(false))
            .bounds(center + 4, bottom, buttonWidth, 20).build()));
    }

    private void finish(boolean keepOldValues) {
        if (remoteServer) {
            PacketDistributor.sendToServer(new GuardPackets.ResolveConfigUpdate(keepOldValues));
        } else {
            if (!GuardConfig.respondToUpdate(keepOldValues)) {
                failed = true;
                return;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(mc.getConnection() == null
            ? new GuardConfigScreen(GuardConfig.defaultSnapshot(false), parent)
            : new GuardConfigLoadingScreen(parent));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int center = width / 2;
        int listWidth = Math.max(80, Math.min(430, width - 36));
        int left = center - listWidth / 2;
        int listTop = height < 230 ? 44 : 62;
        int listBottom = Math.max(listTop + 12, height - (height < 230 ? 70 : 101));
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0x6219141E);
        graphics.fill(left - 9, 10, left + listWidth + 9, height - 7, 0xD1211B2A);
        graphics.fill(left - 9, 10, left + listWidth + 9, 12, 0xFFD8BEAA);
        graphics.drawCenteredString(font, title, center, 17, 0xFFD8BEAA);
        if (height >= 230) graphics.drawCenteredString(font, "Saved settings need an update before editing.", center, 36, 0xFFF5F0F6);
        graphics.fill(left, listTop - 3, left + listWidth, listBottom, 0xFF302737);
        record Row(FormattedCharSequence text, int color) {}
        List<Row> rows = new ArrayList<>();
        for (String change : changes) {
            int color = change.startsWith("Added:") ? 0xFFAEE9A3 : change.startsWith("Removed:") ? 0xFFE6A8A8
                : change.startsWith("Default changed:") ? 0xFFD8BEAA : 0xFFF5F0F6;
            for (FormattedCharSequence line : font.split(Component.literal(change), listWidth - 12))
                rows.add(new Row(line, color));
        }
        int visible = Math.max(1, (listBottom - listTop) / 12);
        scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visible));
        graphics.enableScissor(left, listTop, left + listWidth, listBottom);
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++)
            graphics.drawString(font, rows.get(i).text(), left + 6, listTop + (i - scroll) * 12, rows.get(i).color());
        graphics.disableScissor();
        if (rows.size() > visible) graphics.drawCenteredString(font, "Scroll for more changes", center, listBottom + 2, 0xFFAAAAAA);
        int noteY = height - 87;
        if (height >= 230) {
            graphics.drawCenteredString(font, "Keep retains your values and removes obsolete entries.", center, noteY, 0xFFC3B4CA);
            graphics.drawCenteredString(font, "Defaults resets the affected config files.", center, noteY + 12, 0xFFC3B4CA);
        }
        if (failed) graphics.drawCenteredString(font, "Could not save settings. Check the log and try again.", center,
            Math.min(height - 82, noteY + 28), 0xFFFF7777);
        for (Button button : themedButtons) {
            int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
            boolean hover = button.isMouseOver(mouseX, mouseY);
            graphics.fill(x, y, x + w, y + h, hover ? 0xFFD8BEAA : 0xFF947D9A);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xD45C4D68 : 0xCC372D42);
            graphics.drawCenteredString(font, button.getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFF5F0F6);
        }
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
        return true;
    }

    @Override public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
