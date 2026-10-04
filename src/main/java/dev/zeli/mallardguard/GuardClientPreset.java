package dev.zeli.mallardguard;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import java.util.stream.Collectors;

/** Values used when a server supplies initial or enforced client settings. */
public final class GuardClientPreset {
    public static final int PARTICLES = 1, HUD = 2, AUDIO = 4, SCREEN=8, ANIMATIONS=16, PUNCHY = 32;
    public static final int[] CATEGORIES = {PARTICLES, HUD, AUDIO, SCREEN, ANIMATIONS, PUNCHY};
    public static final int ALL_CATEGORIES = 63;
    public static final int[] DEFAULTS = GuardPoseSettings.appendMotion(GuardShieldReactions.append(GuardPoseSettings.appendDefaults(GuardClientSettings.defaults()),false),false);
    private static final int[] MIN = GuardClientSettings.minimums();
    private static final int[] MAX = GuardClientSettings.maximums();

    // The copied client config is an optional, editable template on the server.
    private static final String[] TEMPLATE_KEYS = GuardClientSettings.keys();

    public static Path enforceTemplatePath() {
        return FMLPaths.CONFIGDIR.get().resolve("mallard_guard/enforce/client.toml");
    }

    private static boolean booleanField(int index) { return GuardClientSettings.flag(index); }

    public static int[] readEnforceTemplate(int[] fallback) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return fallback;
        int[] values = fallback.clone();
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] == null) continue;
                Object raw = config.get(TEMPLATE_KEYS[i]);
                if (raw instanceof Boolean booleanValue && booleanField(i)) values[i] = booleanValue ? 1 : 0;
                else if (raw instanceof Number number && !booleanField(i)) {
                    int value = number.intValue();
                    if (value >= MIN[i] && value <= MAX[i]) values[i] = value;
                }
            }
            GuardShieldReactions.readTemplate(config,values);
            Object punchy = config.get("punchy.enabled");
            if (punchy instanceof Boolean enabled) values[65] = enabled ? 1 : 0;
            for (int i = 0; i < GuardPoseSettings.COUNT; i++) {
                Object raw = config.get("punchy.pose" + (i + 1));
                int[] pose = raw instanceof String text ? GuardPoseSettings.parsePose(text) : null;
                if (pose != null) System.arraycopy(pose, 0, values, GuardPoseSettings.OFFSET + i * GuardPoseSettings.STRIDE, GuardPoseSettings.STRIDE);
            }
            Path punchyPath = path.getParent().resolve("mg_punchy/config.toml");
            if(Files.isRegularFile(punchyPath)) try(CommentedFileConfig profile=CommentedFileConfig.builder(punchyPath).sync().build()) {
                profile.load();Object delay=profile.get("releaseDelayMillis");if(delay instanceof Number n&&n.intValue()>=0&&n.intValue()<=1000)values[GuardPoseSettings.RELEASE_DELAY_SLOT]=n.intValue(); Object enabled=profile.get("enabled");if(enabled instanceof Boolean b)values[65]=b?1:0;
                for(int i=0;i < GuardPoseSettings.COUNT;i++){Object raw=profile.get("pose"+(i+1));int[] pose=raw instanceof String text?GuardPoseSettings.parsePose(text):null;if(pose!=null)System.arraycopy(pose,0,values,GuardPoseSettings.OFFSET+i*23,23);}
                // Legacy shared motion is only a fallback for presets without their own saved values.
                String[] fields={"raiseMillis","lowerMillis","curve","strength"};
                for(int i=0;i < GuardPoseSettings.COUNT;i++) {
                    Object pose=profile.get("pose"+(i+1));
                    if(pose instanceof String text&&GuardPoseSettings.parsePose(text)!=null)continue;
                    for(int f=0;f<fields.length;f++) {
                        Object raw=profile.get(fields[f]);
                        if(raw instanceof Number n&&n.intValue()>=GuardPoseSettings.min(19+f)&&n.intValue()<=GuardPoseSettings.max(19+f))
                            values[GuardPoseSettings.OFFSET+i*GuardPoseSettings.STRIDE+19+f]=n.intValue();
                    }
                }
            }
            Number revision = config.get("configRevision");
            if (revision == null || revision.intValue() < 12) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = normalizeOldValue(i, values[i]);
                }
            }
            if (revision == null || revision.intValue() < 15) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = sharedScale(i, values[i]);
                }
            }
            if (Boolean.FALSE.equals(config.get("display.screenShake"))) values[5] = 0;
            return values;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not load enforce template: " + error.getMessage());
            return fallback;
        }
    }

    public static boolean writeEnforceTemplate(int[] values) {
        Path path = enforceTemplatePath();
        if (!Files.isRegularFile(path)) return true;
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] != null) {
                    Object value = booleanField(i) ? Boolean.valueOf(values[i] != 0) : Integer.valueOf(values[i]);
                    config.set(TEMPLATE_KEYS[i], value);
                }
            }
            GuardShieldReactions.writeTemplate(config,values);
            config.remove("display.offhandGuardPriority");
            config.remove("punchy");
            Path directory=path.getParent().resolve("mg_punchy");
            try{Files.createDirectories(directory);}catch(java.io.IOException error){throw new IllegalStateException(error);}
            try(CommentedFileConfig profile=CommentedFileConfig.builder(directory.resolve("config.toml")).sync().build()) {
                profile.load();profile.remove("reaction");profile.set("releaseDelayMillis",values[GuardPoseSettings.RELEASE_DELAY_SLOT]);profile.set("configRevision",GuardConfig.CONFIG_REVISION);profile.set("enabled",values[65]!=0);for(int i=0;i < GuardPoseSettings.COUNT;i++){profile.set("pose"+(i+1),GuardPoseSettings.encodePose(GuardPoseSettings.pose(values,i)));}
                for(String legacy:new String[]{"raiseMillis","lowerMillis","curve","strength"})profile.remove(legacy);
                profile.save();
            }
            config.set("display.screenShake", values[5] > 0);
            config.set("configRevision", GuardConfig.CONFIG_REVISION);
            config.save();
            GuardConfig.clearBackups(path.getParent());
            return true;
        } catch (RuntimeException error) {
            System.err.println("Mallard Guard: could not save enforce template: " + error.getMessage());
            return false;
        }
    }

    private static int normalizeOldValue(int index, int value) {
        float factor = switch (index) {
            case 7, 44 -> 0.1F;
            case 8 -> 0.3F;
            case 17 -> 1.3F;
            case 20 -> 0.5F;
            case 21 -> 0.2F;
            case 30 -> 1.4F;
            case 37, 46 -> 0.6F;
            case 45 -> 0.4F;
            default -> 1.0F;
        };
        return Math.clamp(Math.round(value / factor), MIN[index], MAX[index]);
    }

    private static int sharedScale(int index, int value) {
        float factor = switch (index) {
            case 7, 44 -> 0.1F;
            case 8 -> 0.3F;
            case 17 -> 1.69F;
            case 20 -> 0.5F;
            case 21 -> 0.8F;
            case 29 -> 1.1F;
            case 30 -> 1.4F;
            case 37, 46 -> 0.6F;
            case 45 -> 0.4F;
            default -> 1.0F;
        };
        return Math.clamp(Math.round(value * factor), MIN[index], MAX[index]);
    }

    public static String settingName(int index) {
        if(index==GuardPoseSettings.RELEASE_DELAY_SLOT)return "Punchy release delay";
        if(index>=GuardShieldReactions.OFFSET)return GuardShieldReactions.key(index-GuardShieldReactions.OFFSET);
        if(index>=GuardPoseSettings.OFFSET){int local=index-GuardPoseSettings.OFFSET;return "Punchy preset "+(local/23+1)+" / value "+(local%23);}
        return index<TEMPLATE_KEYS.length&&TEMPLATE_KEYS[index]!=null?TEMPLATE_KEYS[index]:"Client setting "+index;
    }
    private GuardClientPreset() {}

    public static String encode(int[] values) {
        return Arrays.stream(values).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }

    public static int[] parse(String text) {
        if (text == null || text.length() > 2048) return null;
        String[] parts = text.split(",", -1);
        // Accept saved server presets from earlier config revisions, filling new slots with defaults.
        if (parts.length != DEFAULTS.length && parts.length != 217 && parts.length != GuardPoseSettings.RELEASE_DELAY_SLOT && parts.length != GuardShieldReactions.OFFSET && parts.length != 158 && parts.length != 186 && parts.length != 65 && parts.length != 50 && parts.length != 49 && parts.length != 48 && parts.length != 47 && parts.length != 46 && parts.length != 45 && parts.length != 44 && parts.length != 40 && parts.length != 39 && parts.length != 35 && parts.length != 29 && parts.length != 26 && parts.length != 25) return null;
        int[] values = DEFAULTS.clone();
        try {
            boolean oldScale = parts.length < 48 || Integer.parseInt(parts[47]) < 15;
            for (int i = 0; i < Math.min(parts.length==186?GuardShieldReactions.OFFSET:parts.length,values.length); i++) {
                values[i] = Integer.parseInt(parts[i]);
                if (parts.length < 40) {
                    if (i == 16 || i == 17 || i == 19) values[i] = Math.clamp(Math.round(values[i] / 0.7F), MIN[i], MAX[i]);
                    if (i == 20 || i == 21) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 29) values[i] = Math.clamp(Math.round(values[i] / 0.75F), MIN[i], MAX[i]);
                    if (i == 30) values[i] = Math.clamp(Math.round(values[i] / 2.5F), MIN[i], MAX[i]);
                    if (i == 37) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 38) values[i] = Math.clamp(Math.round(values[i] / 4.0F), MIN[i], MAX[i]);
                }
                if (parts.length < 45 && parts.length >= 40) {
                    if (i == 21) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 17) values[i] = Math.clamp(Math.round(values[i] / 1.3F), MIN[i], MAX[i]);
                    if (i == 29) values[i] = Math.clamp(Math.round(values[i] / 1.1F), MIN[i], MAX[i]);
                    if (i == 30) values[i] = Math.clamp(Math.round(values[i] / 2.0F), MIN[i], MAX[i]);
                    if (i == 37) values[i] = Math.clamp(Math.round(values[i] / 0.9F), MIN[i], MAX[i]);
                    if (i == 38) values[i] = Math.clamp(Math.round(values[i] / 1.8F), MIN[i], MAX[i]);
                }
                if (i == 3) values[i] = Math.min(10, values[i]);
                if (i == 12) values[i] = Math.min(200, values[i]);
                if (i == 25 || i == 34) values[i] = Math.min(1, values[i]);
                if (parts.length < 48) values[i] = normalizeOldValue(i, values[i]);
                if (oldScale && i < 65) values[i] = i == 47 ? 15 : sharedScale(i, values[i]);
                if(i==GuardPoseSettings.RELEASE_DELAY_SLOT){if(values[i]<0||values[i]>1000)return null;continue;}
                if(i>=GuardShieldReactions.OFFSET){int r=i-GuardShieldReactions.OFFSET;if(values[i]<(r%6==5?1:0)||values[i]>GuardShieldReactions.max(r))return null;continue;}
                if (i >= 65 ? values[i] < (i == 65 ? 0 : GuardPoseSettings.min((i - 66) % GuardPoseSettings.STRIDE)) || values[i] > (i == 65 ? 1 : GuardPoseSettings.max((i - 66) % GuardPoseSettings.STRIDE)) : values[i] < MIN[i] || values[i] > MAX[i]) return null;
            }
            if (parts.length == 35) values[36] = values[33];
            if (parts.length > 29 && parts.length < 40) values[39] = values[29];
        } catch (NumberFormatException error) { return null; }
        return values;
    }

    public static boolean valid(String text) { return parse(text) != null; }

    public static int categoryOf(int index) {
        if(index>=GuardPoseSettings.RELEASE_DELAY_SLOT)return PUNCHY;
        if (index >= GuardShieldReactions.OFFSET) return HUD;
        if (index >= 65) return PUNCHY;
        return GuardClientSettings.category(index);
    }
    public static int[] readLocal() {
        return GuardPoseSettings.appendMotion(GuardShieldReactions.append(GuardPoseSettings.appendLocal(GuardClientSettings.readLocal()), true),true);
    }
    public static void apply(int[] values, int category) {
        if (category == PUNCHY) { GuardPoseSettings.apply(values); }
        else {
            GuardClientSettings.apply(values, category);
            if (category == HUD) GuardShieldReactions.apply(values);
        }
    }
}
