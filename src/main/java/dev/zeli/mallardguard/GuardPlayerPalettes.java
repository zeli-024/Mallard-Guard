package dev.zeli.mallardguard;

import dev.zeli.mallardguard.GuardPackets.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server palette ownership, enforcement and join/leave publication. */
public final class GuardPlayerPalettes {
    private GuardPlayerPalettes() {}
    private record CachedPalette(int id, Palette personal, Palette effective) {}
    // Accessed only on the server thread; never store players, entities or worlds in these entries.
    private static final java.util.Map<java.util.UUID, CachedPalette> PLAYER_PALETTES = new java.util.HashMap<>();
    private static int nextPaletteId = 1;
    private static volatile net.minecraft.server.MinecraftServer paletteServer;
    private static Palette enforcedPalette;

    public static void playerPalettesJoined(ServerPlayer player) {
        paletteServer = player.serverLevel().getServer();
        int id = playerPaletteId(player);
        for (CachedPalette entry : PLAYER_PALETTES.values())
            if (entry.id() != id) PacketDistributor.sendToPlayer(player, new PlayerPalette(entry.id(), entry.effective()));
    }
    public static int playerPaletteId(ServerPlayer player) {
        CachedPalette entry = PLAYER_PALETTES.get(player.getUUID());
        if (entry == null) {
            paletteServer = player.serverLevel().getServer();
            Palette personal = Palette.fromPreset(GuardClientPreset.DEFAULTS);
            entry = new CachedPalette(nextPaletteId++, personal, enforcedPalette == null ? personal : enforcedPalette);
            PLAYER_PALETTES.put(player.getUUID(), entry);
            broadcastPalette(new PlayerPalette(entry.id(), entry.effective()));
        }
        return entry.id();
    }
    public static void receivePalette(SetPalette data, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !data.colors().valid()) return;
        int id = playerPaletteId(player);
        CachedPalette previous = PLAYER_PALETTES.get(player.getUUID());
        Palette effective = enforcedPalette == null ? data.colors() : enforcedPalette;
        if (data.colors().equals(previous.personal()) && effective.equals(previous.effective())) return;
        PLAYER_PALETTES.put(player.getUUID(), new CachedPalette(id, data.colors(), effective));
        if (!effective.equals(previous.effective())) broadcastPalette(new PlayerPalette(id, effective));
    }
    private static void broadcastPalette(PlayerPalette data) {
        if (paletteServer != null) for (ServerPlayer viewer : paletteServer.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(viewer, data);
    }
    public static void enforcePalettes(boolean locked, int[] values) {
        enforcedPalette = locked ? Palette.fromPreset(values) : null;
        PLAYER_PALETTES.replaceAll((uuid, previous) -> {
            Palette effective = enforcedPalette == null ? previous.personal() : enforcedPalette;
            if (effective.equals(previous.effective())) return previous;
            broadcastPalette(new PlayerPalette(previous.id(), effective));
            return new CachedPalette(previous.id(), previous.personal(), effective);
        });
    }
    public static void forgetPlayerPalette(java.util.UUID uuid) {
        CachedPalette removed = PLAYER_PALETTES.remove(uuid);
        if (removed != null) broadcastPalette(new PlayerPalette(removed.id(), null));
    }
    public static void clearPlayerPalettes() {
        PLAYER_PALETTES.clear(); paletteServer = null; enforcedPalette = null; nextPaletteId = 1;
    }
    /** Config reloads may arrive off-thread; distribute policy and palettes on the server thread. */
    public static void refreshPalettePolicy() {
        var server = paletteServer;
        if (server != null) server.execute(() -> {
            if (paletteServer != server) return;
            ClientPolicy policy = GuardPackets.clientPolicy();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) GuardConfigTransfer.sendConfigData(player, policy);
        });
    }

}
