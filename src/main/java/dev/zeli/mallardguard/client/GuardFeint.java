package dev.zeli.mallardguard.client;
import java.lang.reflect.Method;
import dev.zeli.mallardguard.GuardPackets;
import dev.zeli.mallardguard.GuardState;
import dev.zeli.mallardguard.mixin.client.LivingEntityAttackTickerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Optional Better Combat integration. Reflection is resolved once and only used at stance transitions. */
public final class GuardFeint {
    private static KeyMapping feintKey;
    private static Method stopAnimation;
    private static boolean resolved;
    private static long cancelledUntil = Long.MIN_VALUE;
    private GuardFeint() {}
    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!net.neoforged.fml.ModList.get().isLoaded("bettercombat")) return;
        try {
            feintKey = (KeyMapping) Class.forName("net.bettercombat.client.Keybindings").getField("feintKeyBinding").get(null);
            stopAnimation = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable").getMethod("stopAttackAnimation", float.class);
        } catch (ReflectiveOperationException failure) {
            System.err.println("Mallard Guard: Better Combat feint integration unavailable: " + failure.getClass().getSimpleName());
        }
    }
    public static void cancelled() {
        resolve();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || feintKey == null || !feintKey.isDown()) return;
        cancelledUntil = mc.player.level().getGameTime() + 10;
        GuardState.clearClientAttackLock();
        if (mc.getConnection() != null) PacketDistributor.sendToServer(new GuardPackets.FeintEnded());
    }
    public static void beforeGuard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.level().getGameTime() > cancelledUntil) return;
        ((LivingEntityAttackTickerAccessor) mc.player).mallardguard$attackTicker((int) Math.ceil(mc.player.getCurrentItemAttackStrengthDelay()));
        cancelledUntil = Long.MIN_VALUE;
        stopAttackAnimation();
    }
    public static void stopAttackAnimation() {
        resolve();
        if (stopAnimation == null || Minecraft.getInstance().player == null) return;
        try { stopAnimation.invoke(Minecraft.getInstance().player, 0.0F); }
        catch (ReflectiveOperationException failure) { stopAnimation = null; }
    }
    public static void clear() { cancelledUntil = Long.MIN_VALUE; }
}
