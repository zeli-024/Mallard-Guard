package dev.zeli.mallardguard;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import java.nio.file.Files;
import net.neoforged.fml.loading.LoadingModList;
import org.slf4j.LoggerFactory;

/** Check required animation hooks without loading or transforming Punchy's classes. */
public final class GuardPunchyApi {
    @FunctionalInterface
    public interface ClassSource {
        ClassNode read(String name) throws Exception;
    }

    private static Boolean available;
    private static String failure;

    private GuardPunchyApi() {}

    public static synchronized boolean compatible() {
        if (available == null) {
            available = supports(GuardPunchyApi::readInstalledClass);
            if (available)
                LoggerFactory.getLogger("MallardGuard").info("Punchy animation hooks verified.");
            else
                LoggerFactory.getLogger("MallardGuard").warn(
                    "Punchy guard integration unavailable: {}. Using the simple guard pose; preset editing is disabled.", failure);
        }
        return available;
    }

    private static ClassNode readInstalledClass(String name) throws Exception {
        var file = LoadingModList.get().getModFileById("punchy");
        if (file == null) throw new ClassNotFoundException("Punchy is not installed");
        var path = file.getFile().findResource(name.replace('.', '/') + ".class");
        try (var input = Files.newInputStream(path)) {
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    public static boolean supports(ClassSource source) {
        failure = null;
        try {
            ClassNode pose = source.read("punchy.client.animation.PoseHandler");
            ClassNode renderer = source.read("punchy.client.render.PunchyArmRenderer");
            ClassNode client = source.read("punchy.client.PunchyClient");
            ClassNode intent = source.read("punchy.client.animation.AnimationIntentCoordinator");
            return method(pose, "applyTransformations", "(Ljava/util/Map;FZ)V")
                && method(pose, "isDualHandedPoseActive", "(F)Z")
                && method(pose, "captureCurrentArmSamplesInto", "(Ljava/util/Map;Lnet/minecraft/world/entity/HumanoidArm;)V")
                && method(pose, "applyBlendInEasing", "(F)F")
                && method(pose, "getBlendOutProgress", "(F)F")
                && method(pose, "tick", "(FF)V")
                && method(renderer, "renderFirstPerson", "(Lnet/minecraft/client/renderer/ItemInHandRenderer;Lnet/minecraft/client/player/LocalPlayer;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", true)
                && method(client, "shouldSuppressOffhandRender", "(Lnet/minecraft/world/entity/player/Player;)Z", true)
                && method(client, "shouldLowerOffhand", "(Lnet/minecraft/client/player/LocalPlayer;)Z", true)
                && method(intent, "flush", "()V", true)
                && method(intent, "discardPendingForEquipTransition", "()V", true);
        } catch (Exception | LinkageError error) {
            failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            return false;
        }
    }

    public static boolean method(ClassNode type, String name, String descriptor) {
        return method(type, name, descriptor, false);
    }

    private static boolean method(ClassNode type, String name, String descriptor, boolean isStatic) {
        boolean found = type != null && type.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)
            && ((method.access & Opcodes.ACC_STATIC) != 0) == isStatic);
        if (!found) failure = "Missing " + (isStatic ? "static " : "") + "hook "
            + (type == null ? "<missing class>" : type.name) + "." + name + descriptor;
        return found;
    }

    public static boolean diagnosticsCompatible() {
        try {
            ClassNode type = readInstalledClass("punchy.client.modelparts.ModelPartsParticleSystem");
            return type.methods.stream().anyMatch(method -> method.name.equals("maybeLogGlowDefinition") && method.desc.endsWith(")V"))
                && type.methods.stream().anyMatch(method -> method.name.equals("maybeLogGlowRender") && method.desc.endsWith(")V"));
        } catch (Exception | LinkageError error) {
            return false;
        }
    }
}
