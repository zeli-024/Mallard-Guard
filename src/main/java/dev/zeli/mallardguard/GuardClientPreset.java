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
        GuardConfig.preserveServerFile(path);
        GuardConfig.preserveServerFile(path.getParent().resolve("mg_punchy/config.toml"));
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] == null) continue;
                Object raw = config.get(TEMPLATE_KEYS[i]);
                if (raw instanceof Boolean booleanValue && booleanField(i)) values[i] = booleanValue ? 1 : 0;
                else if (raw instanceof Number number && !booleanField(i) && Double.isFinite(number.doubleValue())) {
                    int value = (int)Math.round(number.doubleValue()*GuardClientSettings.scale(i));
                    if (value >= MIN[i] && value <= MAX[i]) values[i] = value;
                }
            }
            GuardShieldReactions.readTemplate(config,values);
            GuardSparkTracerConfig.read(config,values);
            GuardParticleColors.read(config,values);
            if(config.get("display.regularHitlagFrames")==null)values[62]=GuardClientSettings.legacyRegularHitlag(config);
            Number particleRevision=config.get("configRevision");if(particleRevision==null||particleRevision.intValue()<70){
                GuardParticleConfig.migratePerfect(values);
                if(config.get("particles.flyingSparks.perfectIntensity")==null)values[17]=DEFAULTS[17];
                if(config.get("particles.dotStreaks.perfectIntensity")==null)values[20]=DEFAULTS[20];
                if(config.get("particles.shootingSparks.perfectIntensity")==null && config.get("particles.shootingSparks.intensity")==null)values[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY]=DEFAULTS[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY];
            }
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
                    if (!GuardParticleConfig.slot(i) && TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = normalizeOldValue(i, values[i]);
                }
            }
            if (revision == null || revision.intValue() < 15) {
                for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                    if (!GuardParticleConfig.slot(i) && TEMPLATE_KEYS[i] != null && config.contains(TEMPLATE_KEYS[i])) values[i] = sharedScale(i, values[i]);
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
        GuardConfig.preserveServerFile(path);
        GuardConfig.preserveServerFile(path.getParent().resolve("mg_punchy/config.toml"));
        try (CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build()) {
            config.load();
            for (int i = 0; i < TEMPLATE_KEYS.length; i++) {
                if (TEMPLATE_KEYS[i] != null) {
                    Object value;
                    if(booleanField(i))value=Boolean.valueOf(values[i]!=0);
                    else if(GuardClientSettings.scale(i)!=1)value=Double.valueOf(values[i]/(double)GuardClientSettings.scale(i));
                    else value=Integer.valueOf(values[i]);
                    config.set(TEMPLATE_KEYS[i], value);
                }
            }
            GuardShieldReactions.writeTemplate(config,values);
            GuardParticleConfig.writeTemplate(config,values);
            config.remove("display.offhandGuardPriority");
            config.remove("display.perfectOnlyHitlag");
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

    private static int normalizeOldValue(int index, int value) { return Math.clamp(value, MIN[index], MAX[index]); }

    private static int sharedScale(int index, int value) { return Math.clamp(value, MIN[index], MAX[index]); }

    public static String settingName(int index) {
        if(GuardParticleColors.retired(index))return "Retired particle setting";
        if(GuardParticleColors.slot(index))return GuardParticleColors.key(index);
        if(GuardSparkTracerConfig.retired(index))return "Retired particle setting";
        if(GuardSparkTracerConfig.slot(index))return GuardSparkTracerConfig.key(index);
        if(GuardParticleConfig.legacySlot(index))return "Retired particle setting";
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
        if (parts.length != GuardParticleColors.CONTROLS_PREVIOUS_LENGTH && parts.length != GuardParticleColors.CONTROLS_PREVIOUS_LENGTH+2 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_51 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_51+2 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_49 && parts.length != GuardParticleColors.SERIALIZED_LENGTH_49+2 && parts.length != GuardParticleColors.LEGACY_LENGTH && parts.length != GuardParticleColors.LEGACY_LENGTH+2 && parts.length != GuardParticleColors.LIFETIME_LENGTH && parts.length != GuardParticleColors.LIFETIME_LENGTH+2 && parts.length != GuardParticleColors.APPEARANCE_LENGTH && parts.length != GuardParticleColors.APPEARANCE_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_COMPONENT_LENGTH && parts.length != GuardParticleColors.IMPACT_COMPONENT_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_SETTINGS_LENGTH && parts.length != GuardParticleColors.IMPACT_SETTINGS_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_PREVIOUS_LENGTH && parts.length != GuardParticleColors.IMPACT_PREVIOUS_LENGTH+2 && parts.length != GuardParticleColors.IMPACT_BASE_LENGTH && parts.length != GuardParticleColors.IMPACT_BASE_LENGTH+2 && parts.length != GuardParticleColors.PREVIOUS_LENGTH && parts.length != GuardParticleColors.OFFSET+2 && parts.length != GuardParticleColors.OFFSET+4 && parts.length != GuardParticleColors.OFFSET && parts.length != GuardSparkTracerConfig.TUNING_LENGTH && parts.length != GuardSparkTracerConfig.TUNING_LENGTH+2 && parts.length != GuardSparkTracerConfig.VARIANCE_LENGTH && parts.length != GuardSparkTracerConfig.VARIANCE_LENGTH+2 && parts.length != GuardSparkTracerConfig.SHRINK_LENGTH && parts.length != GuardSparkTracerConfig.SHRINK_LENGTH+2 && parts.length != GuardSparkTracerConfig.PREVIOUS_LENGTH && parts.length != GuardSparkTracerConfig.LEGACY_LENGTH && parts.length != GuardSparkTracerConfig.LEGACY_LENGTH+2 && parts.length != GuardSparkTracerConfig.OFFSET && parts.length != GuardSparkTracerConfig.OFFSET+2 && parts.length != DEFAULTS.length && parts.length != DEFAULTS.length+2 && parts.length != 216 && parts.length != 214 && parts.length != 217 && parts.length != GuardPoseSettings.RELEASE_DELAY_SLOT+1 && parts.length != GuardPoseSettings.RELEASE_DELAY_SLOT && parts.length != GuardShieldReactions.OFFSET && parts.length != 158 && parts.length != 186 && parts.length != 65 && parts.length != 50 && parts.length != 49 && parts.length != 48 && parts.length != 47 && parts.length != 46 && parts.length != 45 && parts.length != 44 && parts.length != 40 && parts.length != 39 && parts.length != 35 && parts.length != 29 && parts.length != 26 && parts.length != 25) return null;
        int[] values = DEFAULTS.clone();
        try {
            boolean oldScale = parts.length < 48 || Integer.parseInt(parts[47]) < 15;
            int suppliedLength=parts.length;
            if(parts.length==GuardParticleColors.CONTROLS_PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<88)suppliedLength=GuardParticleColors.CONTROLS_PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.SERIALIZED_LENGTH_51+2 && Integer.parseInt(parts[47])<84)suppliedLength=GuardParticleColors.SERIALIZED_LENGTH_51;
            else if(parts.length==GuardParticleColors.SERIALIZED_LENGTH_49+2 && Integer.parseInt(parts[47])<83)suppliedLength=GuardParticleColors.SERIALIZED_LENGTH_49;
            else if(parts.length==GuardParticleColors.LEGACY_LENGTH+2 && Integer.parseInt(parts[47])<82)suppliedLength=GuardParticleColors.LEGACY_LENGTH;
            else if(parts.length==GuardParticleColors.LIFETIME_LENGTH+2 && Integer.parseInt(parts[47])<81)suppliedLength=GuardParticleColors.LIFETIME_LENGTH;
            else if(parts.length==GuardParticleColors.APPEARANCE_LENGTH+2 && Integer.parseInt(parts[47])<80)suppliedLength=GuardParticleColors.APPEARANCE_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_COMPONENT_LENGTH+2 && Integer.parseInt(parts[47])<79)suppliedLength=GuardParticleColors.IMPACT_COMPONENT_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_SETTINGS_LENGTH+2 && Integer.parseInt(parts[47])<78)suppliedLength=GuardParticleColors.IMPACT_SETTINGS_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<77)suppliedLength=GuardParticleColors.IMPACT_PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.IMPACT_BASE_LENGTH+2)suppliedLength=GuardParticleColors.IMPACT_BASE_LENGTH;
            else if(parts.length==GuardParticleColors.PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<75)suppliedLength=GuardParticleColors.PREVIOUS_LENGTH;
            else if(parts.length==GuardParticleColors.OFFSET+4 && Integer.parseInt(parts[47])<71)suppliedLength=GuardParticleColors.OFFSET+2;
            else if(parts.length==GuardParticleColors.OFFSET+2 && Integer.parseInt(parts[47])<70)suppliedLength=GuardParticleColors.OFFSET;
            else if(parts.length==GuardSparkTracerConfig.TUNING_LENGTH+2 && Integer.parseInt(parts[47])<69)suppliedLength=GuardSparkTracerConfig.TUNING_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.VARIANCE_LENGTH+2)suppliedLength=GuardSparkTracerConfig.VARIANCE_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.SHRINK_LENGTH+2)suppliedLength=GuardSparkTracerConfig.SHRINK_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.PREVIOUS_LENGTH+2 && Integer.parseInt(parts[47])<66)suppliedLength=GuardSparkTracerConfig.PREVIOUS_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.LEGACY_LENGTH+2)suppliedLength=GuardSparkTracerConfig.LEGACY_LENGTH;
            else if(parts.length==GuardSparkTracerConfig.OFFSET+2)suppliedLength=GuardSparkTracerConfig.OFFSET;
            else if(parts.length==186)suppliedLength=GuardShieldReactions.OFFSET;
            for (int i = 0; i < Math.min(suppliedLength,values.length); i++) {
                if(GuardParticleColors.retired(i) || GuardSparkTracerConfig.retired(i) || GuardClientSettings.reserved(i)) { values[i]=DEFAULTS[i];continue; }
                if(i==47 && Integer.parseInt(parts[i])<GuardParticleConfig.PRESET_REVISION){values[i]=DEFAULTS[i];continue;}
                if (i>=16 && i<=21 && (parts.length<48 || Integer.parseInt(parts[47])<53) && (i>=19 || parts.length<48 || Integer.parseInt(parts[47])<52)) { values[i] = DEFAULTS[i]; continue; }
                values[i] = Integer.parseInt(parts[i]);
                if(i>=GuardPoseSettings.OFFSET && i<GuardPoseSettings.OFFSET+GuardPoseSettings.SIZE && (i-GuardPoseSettings.OFFSET)%GuardPoseSettings.STRIDE>=19 && (i-GuardPoseSettings.OFFSET)%GuardPoseSettings.STRIDE<=20 && parts.length>=48 && Integer.parseInt(parts[47])<73){
                    if(values[i]<0 || values[i]>2000)return null;values[i]=Math.min(500,values[i]);
                }
                if (i == 3) values[i] = Math.min(10, values[i]);
                if (i == 12) values[i] = Math.min(200, values[i]);
                if (i == 34) values[i] = Math.min(1, values[i]);
                if (parts.length < 48) values[i] = normalizeOldValue(i, values[i]);
                if (oldScale && i < 65) values[i] = sharedScale(i, values[i]);
                if(i==GuardParticleColors.IMPACT_DURATION && Integer.parseInt(parts[47])<77){
                    if(values[i]<25||values[i]>400)return null;
                    values[i]=GuardParticleColors.durationFromSpeed(values[i]);
                }
                if(i==GuardParticleColors.TRACER_TRAIL_TICKS && Integer.parseInt(parts[47])<80){if(values[i]<1||values[i]>20)return null;values[i]=Math.min(4,values[i]);}
                if(i==GuardParticleColors.IMPACT_SPIN && Integer.parseInt(parts[47])<82){if(values[i]<-720||values[i]>720)return null;values[i]=Math.abs(values[i]);}
                if(GuardParticleColors.slot(i)){if(!GuardParticleColors.valid(i,values[i]))return null;continue;}
                if(GuardSparkTracerConfig.slot(i)){if(!GuardSparkTracerConfig.valid(i,values[i]))return null;continue;}
                if(i==GuardPoseSettings.RELEASE_DELAY_SLOT){if(values[i]<0||values[i]>1000)return null;continue;}
                if(i>=GuardShieldReactions.OFFSET){int r=i-GuardShieldReactions.OFFSET;if(values[i]<(r%6==5?1:0)||values[i]>GuardShieldReactions.max(r))return null;continue;}
                if (i >= 65 ? values[i] < (i == 65 ? 0 : GuardPoseSettings.min((i - 66) % GuardPoseSettings.STRIDE)) || values[i] > (i == 65 ? 1 : GuardPoseSettings.max((i - 66) % GuardPoseSettings.STRIDE)) : values[i] < MIN[i] || values[i] > MAX[i]) return null;
            }
            if(suppliedLength>GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.INTENSITY && Integer.parseInt(parts[47])<69) {
                int intensity=Integer.parseInt(parts[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.INTENSITY]);
                int enabled=Integer.parseInt(parts[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.ENABLED]);
                if(intensity<0||intensity>100||enabled<0||enabled>1)return null;
                GuardSparkTracerConfig.migrateAmounts(values,intensity,enabled!=0);
            }
            if(parts.length>=48&&Integer.parseInt(parts[47])<70){
                GuardParticleConfig.migratePerfect(values);
                int revision=Integer.parseInt(parts[47]);
                if(suppliedLength<=17 || revision<52)values[17]=DEFAULTS[17];
                if(suppliedLength<=20 || revision<53)values[20]=DEFAULTS[20];
                if(suppliedLength<=GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.BASEINTENSITY)values[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY]=DEFAULTS[GuardSparkTracerConfig.OFFSET+GuardSparkTracerConfig.PERFECTINTENSITY];
            }
        } catch (NumberFormatException | ArithmeticException error) { return null; }
        if(parts.length>28 && (parts.length<48 || Integer.parseInt(parts[47])<88))values[62]="0".equals(parts[28])?values[3]:0;
        return values;
    }

    public static boolean valid(String text) { return parse(text) != null; }

    public static int categoryOf(int index) {
        if(GuardParticleColors.slot(index) || GuardSparkTracerConfig.slot(index) || GuardParticleConfig.legacySlot(index))return PARTICLES;
        if(index==GuardPoseSettings.RELEASE_DELAY_SLOT)return PUNCHY;
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
            if(category==PARTICLES){GuardSparkTracerConfig.apply(values);GuardParticleColors.apply(values);}
            if (category == HUD) GuardShieldReactions.apply(values);
        }
    }
}
