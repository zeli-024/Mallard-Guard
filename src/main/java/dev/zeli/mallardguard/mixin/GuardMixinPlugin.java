package dev.zeli.mallardguard.mixin;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.neoforged.fml.loading.LoadingModList;
public final class GuardMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String pkg) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("EmfGuardPoseMixin")) return LoadingModList.get().getModFileById("entity_model_features") != null;
        if (mixin.endsWith("BetterCombatFeintMixin")) return LoadingModList.get().getModFileById("bettercombat") != null;
        if (mixin.contains("Punchy")) {
            var file = LoadingModList.get().getModFileById("punchy");
            return file != null && file.getMods().stream().anyMatch(mod ->
                mod.getModId().equals("punchy") && (mod.getVersion().toString().equals("2.8b") || mod.getVersion().toString().equals("2.8d")));
        }
        return true;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
