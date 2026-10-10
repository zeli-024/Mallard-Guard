package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardControlSettings.Control;
import dev.zeli.mallardguard.GuardParticleColors;
import dev.zeli.mallardguard.GuardSparkTracerConfig;

import dev.zeli.mallardguard.GuardPoseSettings;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Page content uses the screen's shared controls and editable draft. */
final class GuardConfigPages {
    private GuardConfigPages() {}
    static void build(GuardConfigScreen screen) {
        if (!screen.settingsSearch.isBlank()) screen.buildSearchResults();
        else if (screen.enforceSection) {
            screen.toggle("Enforce client settings", () -> screen.configDraft.enforceEnabled, v -> {
                screen.configDraft.enforceEnabled = v;
                if (!v) screen.enforceParticlesOpen = screen.enforceHudOpen = screen.enforceAudioOpen = screen.enforcePunchyOpen = false;
                else screen.selectRememberedGroup();
                screen.refreshControls();
            }, true, "Apply these starting values to connected clients and lock categories without a Don't Enforce exception.");
            screen.buildEnforcedDefaults();
        } else if (!screen.clientSection) {
            if(screen.debugSection && screen.page==18) screen.buildDiagnostics();
            if (screen.page == 0) {
            screen.featureDivider("Timing", () -> screen.timingOpen, ()->screen.configDraft.parry, value->screen.configDraft.parry=value, true, "Enable timed parries and configure their windows.");
            if (screen.timingOpen && screen.configDraft.parry) {
            screen.sliderBound(Control.PERFECT_TICKS, "Perfect window", () -> screen.configDraft.perfect, " ticks", 1, true, v -> screen.configDraft.perfect = Math.min(v,screen.configDraft.window), "Ticks at the start of a parry that count as perfect. 0 disables perfect parries.");
            screen.sliderBound(Control.PARRY_TICKS, "Parry window", () -> screen.configDraft.window, " ticks", 1, true, v -> {screen.configDraft.window=v;screen.configDraft.perfect=Math.min(screen.configDraft.perfect,v);}, "How long a parry can catch a hit, including the perfect window.");
            screen.sliderBound(Control.RECHARGE_TICKS, "Recharge", () -> screen.configDraft.recharge, " ticks", 1, true, v -> screen.configDraft.recharge = v, "Time before another parry can begin after this attempt.");
            screen.sliderBound(Control.FACING_ANGLE, "Facing angle", () -> screen.configDraft.angle, "°", 1, true, v -> screen.configDraft.angle = v, "Which directions attacks can come from. 180° covers your front half; 360° covers all directions.");
            screen.sliderBound(Control.FOLLOW_UP_TICKS, "Follow-up parries", () -> screen.configDraft.followUp, " ticks", 1, true, v -> screen.configDraft.followUp = v, "Extra time to catch melee hits after a successful parry. 0 disables follow-ups; follow-up hits do not extend the timer.");
                        }
            }
            if (screen.page == 0) {
            screen.dropdown("Healing", () -> screen.healingOpen, "Players and guarding mobs heal on successful parries. Off by default.");
            if (screen.healingOpen) {
                screen.toggle(Control.PARRY_HEALING, "Parry Healing", () -> screen.configDraft.parryHealing, v -> screen.configDraft.parryHealing = v, true, "Players and guarding mobs heal once per successful parry attempt.\nBlocking and follow-up safety hits do not heal. Default: Off.");
                screen.sliderBound(Control.PARRY_HEALING_HEARTS, "Parry healing amount", () -> screen.configDraft.parryHealingHearts, " hearts", 1, true, v -> screen.configDraft.parryHealingHearts = v, "Perfect parries heal this many hearts; regular parries heal half.\n0 disables healing. Default: 1 heart.");
            }
            screen.dropdown("Damage Returned", () -> screen.retaliationOpen, "Show or hide retaliation damage settings.");
            if (screen.retaliationOpen) {
            screen.sliderBound(Control.PARRY_RETALIATION, "Parry retaliation", () -> screen.configDraft.parryReturnPercent, "%", 1, true, v -> screen.configDraft.parryReturnPercent = v, "Return this percentage of the parried damage. 0% disables return; 200% doubles it. Pushback has its own setting.");
            screen.sliderBound(Control.PERFECT_RETALIATION, "Perfect retaliation", () -> screen.configDraft.perfectReturnPercent, "%", 1, true, v -> screen.configDraft.perfectReturnPercent = v, "Return this percentage of perfect-parried damage. 0% disables return; 200% doubles it. Pushback has its own setting.");
            screen.sliderBound(Control.RETALIATION_CAP, "Retaliation damage cap", () -> screen.configDraft.retaliationCap, " damage", 1, true, v -> { screen.configDraft.retaliationCap = v; screen.dirtyShield = true; }, "Maximum damage returned to each target after the multiplier. 0 removes the cap. Default: 25 damage.");
                        }
            }
            if (screen.page == 0) {
                screen.dropdown("Parry Pushback", () -> screen.pushbackOpen, "Set how parries push you and the attacker independently. Returned damage adds no extra knockback.");
                if (screen.pushbackOpen) {
                    screen.sliderBound(Control.KNOCKBACK_STRENGTH, "Defender pushback", () -> screen.configDraft.knockbackStrength, "%", 1, true, v -> screen.configDraft.knockbackStrength = v, "How strongly parrying pushes you away from the hit. Shield parries push you at half this strength. 0% disables both.");
                    screen.sliderBound(Control.SHIELD_PARRY_PUSHBACK_PERCENT, "Shield attacker pushback", () -> screen.configDraft.shieldRegularPush, "%", 1, true, v -> { screen.configDraft.shieldRegularPush = v; screen.dirtyShield = true; }, "Pushes the attacker away on a regular shield parry. Perfect shield parries use cone pushback instead. 0% disables this pushback.");
                    screen.sliderBound(Control.TOOL_PUSHBACK_PERCENT, "Weapon attacker pushback", () -> screen.configDraft.toolPush, "%", 1, true, v -> { screen.configDraft.toolPush = v; screen.dirtyShield = true; }, "Pushes the attacker away on regular and perfect weapon parries. 0% disables this pushback.");
                }
            }
            if (screen.page == 0) {
            screen.dropdown("Projectiles", () -> screen.parryProjectileOpen, "Show or hide projectiles settings.");
            if (screen.parryProjectileOpen) {
            screen.toggle(Control.PARRY_PROJECTILES, "Projectile parry", () -> screen.configDraft.parryProjectiles, v -> screen.configDraft.parryProjectiles = v, true,
                "Let parries deflect projectiles. Regular parries send them in a random direction; perfect parries aim them at the attacker.");
                        }
            }
            if (screen.page == 0) {
            screen.dropdown("Explosions", () -> screen.parryExplosionOpen, "Show or hide explosions settings.");
            if (screen.parryExplosionOpen) {
            screen.toggle(Control.PARRY_EXPLOSIONS, "Explosion parry", () -> screen.configDraft.parryExplosions, v -> { screen.configDraft.parryExplosions = v; screen.refreshControls(); }, true,
                "Let a timed parry prevent explosion damage.");
            if (screen.configDraft.parryExplosions) screen.toggle(Control.PERFECT_EXPLOSIONS_ONLY, "Explosions: perfect only", () -> screen.configDraft.perfectExplosionsOnly, v -> screen.configDraft.perfectExplosionsOnly = v, true,
                "Require perfect timing to parry explosions. Only applies when explosion parrying is enabled.");
                        }
            }
            if (screen.page == 6) screen.buildDamageTypes();

            if (screen.page == 12) {
                screen.toggle(Control.MOB_GUARD, "Mob guarding",()->screen.configDraft.mobGuard,v->{screen.configDraft.mobGuard=v;screen.dirtyMobs=true;},true,
                    "Let whitelisted mobs guard with eligible weapons or shields.\nEmpty hands and ranged weapons cannot guard. Default: Off.");
                screen.sliderBound(Control.MOB_DIFFICULTY, "Mob guard difficulty",()->screen.configDraft.mobDifficulty,"%",1,true,v->{screen.configDraft.mobDifficulty=v;screen.dirtyMobs=true;},
                    "Scale chance-based guard, rush and repeat-guard decisions.\n100% uses normal chances; 50% halves them; 0% stops new decisions.");
                screen.dropdown("Parry Colors",()->screen.mobColorsOpen,"Mob tracers and their impact share these three colors.");
                if(screen.mobColorsOpen){
                    screen.colorField(Control.MOB_TRACER_START, "Mob Tracer Start Color",()->screen.configDraft.mobTracerStartColor,v->screen.configDraft.mobTracerStartColor=v,true,"Color at spawn. Also colors the start of the mob impact.\nDefault: #FFE7E3.");
                    screen.colorField(Control.MOB_TRACER_MIDDLE, "Mob Tracer Middle Color",()->screen.configDraft.mobTracerMiddleColor,v->screen.configDraft.mobTracerMiddleColor=v,true,"Color halfway through fading. Also colors the middle of the impact.\nDefault: #F20900.");
                    screen.colorField(Control.MOB_TRACER_END, "Mob Tracer End Color",()->screen.configDraft.mobTracerEndColor,v->screen.configDraft.mobTracerEndColor=v,true,"Color near disappearance. Also colors the end of the impact.\nDefault: #B9B9B9.");
                }
                screen.dropdown("Spawn Equipment",()->screen.mobEquipmentOpen,"Assign equipment for fresh whitelisted vanilla mobs.\nDifficulty does not affect gear chance.");
                if(screen.mobEquipmentOpen){
                    screen.sliderBound(Control.MOB_GEAR_CHANCE, "Mob gear chance",()->screen.configDraft.mobGearChance,"%",1,true,v->{screen.configDraft.mobGearChance=v;screen.dirtyMobs=true;},"Chance to generate assigned gear on a fresh mob.\nExisting melee gear and occupied armor slots are preserved. Default: 50%.");
                    screen.mobGearAssignments();
                }
                screen.dropdown("Mob Eligibility",()->screen.mobEligibilityOpen,"Choose which mobs may guard or receive generated gear. Bosses remain excluded.");
                if(screen.mobEligibilityOpen)screen.editBox(Control.MOB_WHITELIST, "Mob Guard Whitelist",screen.configDraft.mobWhitelist,v->{screen.configDraft.mobWhitelist=v;screen.dirtyMobs=true;},"Exact entity IDs, separated by commas; empty allows none.\nOnly listed mobs can guard or receive gear. Bosses remain excluded.");
            }
            if (screen.page == 11) {
                screen.dropdown("Shield Timing", () -> screen.shieldTimingOpen, "Show shield parry timing and guard break settings.");
                if (screen.shieldTimingOpen) {
                screen.sliderBound(Control.SHIELD_PERFECT_TICKS, "Shield perfect window", () -> screen.configDraft.shieldPerfect, " ticks", 1, true, v -> {
                    screen.configDraft.shieldPerfect = Math.min(v, screen.configDraft.shieldWindow); screen.dirtyShield = true;
                }, "Perfect parry timing for shields. It must fit within the total shield parry window. 0 disables shield perfect parries.");
                screen.sliderBound(Control.SHIELD_PARRY_TICKS, "Shield parry window", () -> screen.configDraft.shieldWindow, " ticks", 1, true, v -> {
                    screen.configDraft.shieldWindow = v; screen.configDraft.shieldPerfect = Math.min(screen.configDraft.shieldPerfect, v); screen.dirtyShield = true;
                }, "Total shield parry time, including its perfect window. Lowering this also limits perfect parry timing. Shield use continues into blocking until you release guard or guard breaks.");
                screen.sliderBound(Control.SHIELD_RECHARGE_TICKS, "Shield parry recharge", () -> screen.configDraft.shieldRechargeTicks, " ticks", 1, true, v -> { screen.configDraft.shieldRechargeTicks = v; screen.dirtyShield = true; }, "Time before another shield parry after releasing guard. Regular shield parries halve it; perfect shield parries reset it. Separate from the guard break penalty.");
                screen.blockCountAssignments();
                screen.sliderBound(Control.SHIELD_MAX_BLOCKS, "Shield blocks before break", () -> screen.configDraft.shieldMaxBlocks, "", 1, true, v -> { screen.configDraft.shieldMaxBlocks = v; screen.dirtyShield = true; }, "Successful shield blocks allowed before the guard ends. 0 allows unlimited blocks. A disabled shield still follows vanilla's cooldown.");
                }
                screen.dropdown("Shield Perfect Parry", () -> screen.shieldPerfectOpen, "Show shield perfect parry cone effects.");
                if (screen.shieldPerfectOpen) {
                screen.sliderBound(Control.SHIELD_CONE_DEGREES, "Cone half-angle", () -> screen.configDraft.shieldCone, "°", 1, true, v -> { screen.configDraft.shieldCone = v; screen.dirtyShield = true; }, "Angle to each side of where you look. 90° covers the whole front half (180° total); 0° disables cone retaliation, stun and pushback.");
                screen.sliderBound(Control.SHIELD_CONE_REACH, "Cone reach", () -> screen.configDraft.shieldReach, " blocks", 1, true, v -> { screen.configDraft.shieldReach = v; screen.dirtyShield = true; }, "Maximum distance between your eyes and a target's eyes for perfect shield parry cone effects.");
                screen.sliderBound(Control.SHIELD_RETALIATION_PERCENT, "Shield retaliation", () -> screen.configDraft.shieldReturn, "%", 1, true, v -> { screen.configDraft.shieldReturn = v; screen.dirtyShield = true; }, "Percentage of the parried hit dealt to each target in the cone. 0% disables damage but allows configured stun and pushback.");
                screen.sliderBound(Control.SHIELD_STUN_TICKS, "Stun duration", () -> screen.configDraft.shieldStun, " ticks", 1, true, v -> { screen.configDraft.shieldStun = v; screen.dirtyShield = true; }, "Stop voluntary movement and attacks after cone pushback. 20 ticks = 1 second; 0 disables stun. Unlisted bosses are immune.");
                screen.sliderBound(Control.SHIELD_PUSHBACK_PERCENT, "Cone pushback", () -> screen.configDraft.shieldPerfectPush, "%", 1, true, v -> { screen.configDraft.shieldPerfectPush = v; screen.dirtyShield = true; }, "Pushes targets in the perfect parry cone away. 0% disables pushback.");
                }
                screen.dropdown("Additional Shields and Bosses", () -> screen.shieldIdsOpen, "Show extra shield item IDs and bosses eligible for stun.");
                if (screen.shieldIdsOpen) {
                screen.itemRuleAssignment("Shield Item Whitelist", screen.configDraft.shieldItems, v -> { screen.configDraft.shieldItems = v; screen.configDraft.invalidItemRules = false; screen.dirtyShield = true; },
                    "Assign item IDs, #tags, @mod namespaces, or ID/tag keywords to recognize extra shields. Default: Empty.");
                screen.itemRuleAssignment("Shield Item Blacklist", screen.configDraft.shieldBlacklist, v -> {screen.configDraft.shieldBlacklist=v;screen.configDraft.invalidItemRules=false;screen.dirtyShield=true;}, "Assigned items and matching rules excluded from shield recognition. Overrides tags and whitelist. Default: Empty.");
                screen.editBox(Control.SHIELD_STUN_BOSSES, "Stunnable bosses", screen.configDraft.stunnableBosses, v -> { screen.configDraft.stunnableBosses = v; screen.configDraft.invalidItemRules = false; screen.dirtyShield = true; },
                    "Comma-separated entity IDs or #entity tags. Listed bosses may be stunned by a shield perfect parry. Other bosses stay immune. Default: Empty.");
                }
            }
            if (screen.page == 0) {
            screen.dropdown("Fall Damage", () -> screen.fallOpen, "Configure fall parries and launch behavior.");
            if(screen.fallOpen) {
            screen.toggle(Control.FALL_PERFECT_PARRY, "Perfect fall parry", () -> screen.configDraft.fallPerfectParry, v -> screen.configDraft.fallPerfectParry = v, true,
                "On: falls and wall collisions caught during the perfect window count as perfect parries. Off: they count as regular parries even in that window.");
            screen.sliderBound(Control.FALL_BLAST_STRENGTH, "Fall blast", () -> screen.configDraft.fallBlastStrength, "%", 1, true, v -> screen.configDraft.fallBlastStrength = v, "Size of the blast after parrying a fall of more than ten blocks. 0% disables the blast.");
            screen.sliderBound(Control.FALL_LAUNCH_POWER, "Fall launch", () -> screen.configDraft.fallLaunchPower, "%", 1, true, v -> screen.configDraft.fallLaunchPower = v, "Fall parry launch strength. With Look down enabled, carries current movement; otherwise launches where you look. Perfect parries launch farther. 0% prevents damage without launching.");
            screen.toggle(Control.FALL_BREAK_BLOCKS, "Fall blast breaks blocks", () -> screen.configDraft.fallBreakBlocks, v -> screen.configDraft.fallBreakBlocks = v, true,
                "Allow the blast after a long fall parry to break terrain.");
            screen.toggle(Control.FALL_LOOK_DOWN, "Look down to parry", () -> screen.configDraft.fallLookDown, v -> screen.configDraft.fallLookDown = v, true,
                "On requires looking at least 40° down and carries movement momentum. Off allows any aim and launches toward your view.");
            }
            }
            if (screen.page == 1) {
            screen.dropdown("Guard Stance", () -> screen.guardOpen, "Show or hide blocking stance and movement settings.");
            if (screen.guardOpen) {
            screen.toggle(Control.BLOCK, "Blocking", () -> screen.configDraft.block, v -> { screen.configDraft.block = v; screen.refreshControls(); }, true,
                "Keep weapons and shields guarding past the parry window. Off also lowers shields after successful parries. Guard defaults to Right Click and can be rebound in Controls.");
            if (screen.configDraft.block) screen.sliderBound(Control.BLOCK_REDUCTION, "Damage reduction", () -> screen.configDraft.reductionPercent, "%", 1, true, v -> screen.configDraft.reductionPercent = v, "Damage prevented by held block. 50% halves the damage; 100% prevents it.");
            if(screen.configDraft.block)screen.blockCountAssignments();
            if (screen.configDraft.block) screen.sliderBound(Control.TOOL_MAX_BLOCKS, "Weapon blocks before break", () -> screen.configDraft.toolMaxBlocks, "", 1, true, v -> { screen.configDraft.toolMaxBlocks = v; screen.dirtyShield = true; }, "Successful weapon or empty-hand blocks before guard ends. 0 allows unlimited blocks. Attacking with your weapon remains available after guard breaks.");
            if (screen.configDraft.block) screen.sliderBound(Control.SHIELD_BREAK_TICKS, "Guard break recharge", () -> screen.configDraft.shieldBreakTicks, " ticks", 1, true, v -> { screen.configDraft.shieldBreakTicks = v; screen.dirtyShield = true; }, "Wait after the last allowed shield or weapon block before guarding again. A broken shield also receives an item cooldown. Minimum matches normal recharge; max 10 seconds.");
            screen.sliderBound(Control.GUARD_MOVEMENT_PERCENT, "Guard movement", () -> screen.configDraft.guardMovementPercent, "%", 1, true, v -> screen.configDraft.guardMovementPercent = v, "Movement speed while guarding, including parry and block. 100% means normal movement speed.");
                        }
            }
            if (screen.page == 1) {
            if (screen.configDraft.block) {
            screen.dropdown("Projectiles", () -> screen.blockProjectileOpen, "Show or hide projectiles settings.");
            if (screen.blockProjectileOpen) {
            if (screen.configDraft.block) screen.sliderBound(Control.BLOCK_DEFLECT_CHANCE, "Block deflect chance", () -> screen.configDraft.blockDeflectChance, "%", 1, true, v -> screen.configDraft.blockDeflectChance = v, "Chance that held block sends any incoming projectile in a random direction. 0% disables deflection.");
            if (screen.configDraft.block) screen.toggle(Control.BLOCK_PROJECTILES, "Block projectiles", () -> screen.configDraft.blockProjectiles, v -> screen.configDraft.blockProjectiles = v, true,
                "Let held block reduce damage from projectiles that are not deflected.");
                        }
            }
            }
            if (screen.page == 1) {
            if (screen.configDraft.block) {
            screen.dropdown("Explosions", () -> screen.blockExplosionOpen, "Show or hide explosions settings.");
            if (screen.blockExplosionOpen) {
            if (screen.configDraft.block) screen.toggle(Control.BLOCK_EXPLOSIONS, "Block explosions", () -> screen.configDraft.blockExplosions, v -> screen.configDraft.blockExplosions = v, true,
                "Let held block reduce explosion damage using your damage reduction setting.");
                        }
            }
            }
            if (screen.page == 2) {
            screen.toggle(Control.HIT_PARTICLES, "Sparks and flashes", () -> screen.configDraft.hitParticles, v -> { screen.configDraft.hitParticles = v; screen.refreshControls(); }, true,
                "Allow nearby players to see parry sparks and flashes. Blocks do not create particles.");
            screen.toggle(Control.HIT_SOUNDS, "Hit sounds", () -> screen.configDraft.serverHitSounds, v -> screen.configDraft.serverHitSounds = v, true,
                "Allow parry and block sounds to play for nearby players. Everyone controls their own playback volume.");
            }
            if (screen.page == 4) {
            screen.dropdown("Valid Parry Conditions", () -> screen.eligibilityOpen, "Choose eligible items and guard restrictions.");
            if (screen.eligibilityOpen) {
                screen.toggle(Control.ALLOW_EMPTY_HAND, "Empty Hand", () -> screen.configDraft.allowEmptyHand, v -> {screen.configDraft.allowEmptyHand=v;screen.dirtyShield=true;}, true,
                    "Allow empty-hand guarding only when BOTH hands are completely empty. Held items must qualify separately. Default: Off.");
                screen.choice("Tool Requirement", new String[]{"Default", "Any", "Off"}, () -> screen.configDraft.allowAnyItem?2:screen.configDraft.allowUsableItems?1:0,
                    v -> {screen.configDraft.allowAnyItem=v==2;screen.configDraft.allowUsableItems=v==1;}, "Default uses tools without a use action; Any includes usable tools. Off allows any held item. Shields/whitelist also qualify; empty hands use their own toggle.");
                screen.itemRuleAssignment("Item Whitelist", screen.configDraft.includedItems, v -> {screen.configDraft.includedItems=v;screen.configDraft.invalidItemRules=false;}, "Assign individual items, #tags, @mod namespaces, or ID/tag keywords. Default: Empty.");
                screen.itemRuleAssignment("Item Blacklist", screen.configDraft.excludedItems, v -> {screen.configDraft.excludedItems=v;screen.configDraft.invalidItemRules=false;}, "Exclude assigned items and matching rules in every mode. Overrides the whitelist. Default: Empty.");
                screen.toggle(Control.COOLDOWN_PREVENTS_GUARD, "Respect Item Cooldown", () -> screen.configDraft.cooldownPreventsGuard, v -> {screen.configDraft.cooldownPreventsGuard=v;screen.dirtyShield=true;}, true,
                    "Items on cooldown cannot guard. Disabled shields always obey cooldown. Default: On.");
                screen.toggle(Control.CONSUMABLE_PRIORITY, "Consumables Take Priority", () -> screen.configDraft.consumablePriority, v -> {screen.configDraft.consumablePriority=v;screen.dirtyShield=true;}, true,
                    "Eat or drink before guarding when either hand holds a consumable. Default: On.");
            }
            screen.dropdown("Priority", () -> screen.priorityOpen, "Choose which eligible hand guards.");
            if (screen.priorityOpen) {
                screen.choice("Parry Hand Priority", new String[]{"Random", "Off-Hand", "Main-Hand"}, () -> screen.configDraft.parryHandPriority,
                    v -> {screen.configDraft.parryHandPriority=v;screen.dirtyShield=true;}, "Choose the guarding item when both hands qualify. Random never repeats a hand more than twice. Default: Random.");
                screen.choice("Shield Parry Priority", new String[]{"Random", "Shield", "Default"}, () -> screen.configDraft.shieldParryPriority,
                    v -> {screen.configDraft.shieldParryPriority=v;screen.dirtyShield=true;}, "Random chooses either eligible hand; Shield prefers shields. Default prefers the main hand. Crouch override takes priority.");
                screen.toggle(Control.FORCE_CROUCH_OFFHAND, "Force Crouch Off-Hand", () -> screen.configDraft.forceCrouchOffhand, v -> {screen.configDraft.forceCrouchOffhand=v;screen.dirtyShield=true;}, true,
                    "Crouching selects an eligible offhand, overriding both priorities. Otherwise normal selection applies. Default: On.");
            }
            screen.dropdown("Item Durability Wear", () -> screen.durabilityOpen, "Durability consumed by guarded hits.");
            if (screen.durabilityOpen) {
                screen.sliderBound(Control.PARRY_WEAR, "Parry durability", () -> screen.configDraft.parryWear, "%", 10, true, v->screen.configDraft.parryWear=v, "Maximum item durability consumed per regular parry, rounded up. 0% disables wear.");
                screen.sliderBound(Control.PERFECT_WEAR, "Perfect durability", () -> screen.configDraft.perfectWear, "%", 10, true, v->screen.configDraft.perfectWear=v, "Maximum item durability consumed per perfect parry, rounded up. 0% disables wear.");
                screen.sliderBound(Control.BLOCK_WEAR, "Block durability", () -> screen.configDraft.blockWear, "%", 10, true, v->screen.configDraft.blockWear=v, "Maximum item durability consumed per block, rounded up. Shields use vanilla wear; items without durability are unaffected.");
            }
            }
        } else {
            if (screen.page == 7) {
            screen.featureDivider("Flying Sparks", () -> screen.animatedOpen, () -> screen.configDraft.extraClient[GuardParticleColors.FLYING_ENABLED]!=0, value -> screen.configDraft.extraClient[GuardParticleColors.FLYING_ENABLED]=value?1:0, false, "Uneven bursts with animated firework sparks and occasional electric sparks. On/Off preserves your intensity settings.");
            if(screen.animatedOpen) {
                screen.sliderBound(Control.FLYING_BASE_INTENSITY, "Base Intensity", () -> screen.configDraft.extraClient[16], "%", 1, false, v -> screen.configDraft.extraClient[16]=v, "Regular-parry amount. 100% starts with 100 sparks; 0% turns Flying Sparks off completely. Both appearances share the total.");
                screen.sliderBound(Control.FLYING_PERFECT_INTENSITY, "Perfect Intensity", () -> screen.configDraft.extraClient[17], "%", 1, false, v -> screen.configDraft.extraClient[17]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
                screen.sliderBound(Control.FLYING_DAMAGE_SCALING, "Damage Intensity", () -> screen.configDraft.extraClient[18], "%", 1, false, v -> screen.configDraft.extraClient[18]=v, "Increase the amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All particles share a 512-head burst cap.");
                screen.particleAppearance(false,GuardParticleColors.BASE,GuardParticleColors.FLYING_SIZE_VARIANCE+1);
            }

            screen.featureDivider("Spark Debris", () -> screen.dotStreaksOpen, () -> screen.configDraft.extraClient[GuardParticleColors.DEBRIS_ENABLED]!=0, value -> screen.configDraft.extraClient[GuardParticleColors.DEBRIS_ENABLED]=value?1:0, false, "Small debris sparks with immediate gravity, brief dotted tails, and diminishing bounces. On/Off preserves your settings.");
            if(screen.dotStreaksOpen) {
                screen.sliderBound(Control.DEBRIS_BASE_INTENSITY, "Debris Base Intensity", () -> screen.configDraft.extraClient[19], "%", 1, false, v -> screen.configDraft.extraClient[19]=v, "Regular-parry amount. 100% starts with 100 debris sparks; 0% turns Spark Debris off completely.");
                screen.sliderBound(Control.DEBRIS_PERFECT_INTENSITY, "Debris Perfect Intensity", () -> screen.configDraft.extraClient[20], "%", 1, false, v -> screen.configDraft.extraClient[20]=v, "Additional percentage of Base Intensity on perfect parries. 0% adds nothing; 100% doubles the base. Base Intensity 0 turns this effect off completely.");
                screen.sliderBound(Control.DEBRIS_DAMAGE_SCALING, "Debris Damage Intensity", () -> screen.configDraft.extraClient[21], "%", 1, false, v -> screen.configDraft.extraClient[21]=v, "Increase debris amount per damage point: 10% × 5 damage adds 50%. 0% adds nothing. All three effects share the head budget.");
                screen.particleAppearance(false,GuardParticleColors.DEBRIS_COLOR,GuardParticleColors.DEBRIS_SIZE+1);
            }

            int tracerOffset=GuardSparkTracerConfig.OFFSET;
            screen.featureDivider("Spark Tracers", () -> screen.sparkTracersOpen, () -> screen.configDraft.extraClient[GuardParticleColors.TRACER_ENABLED]!=0, value -> screen.configDraft.extraClient[GuardParticleColors.TRACER_ENABLED]=value?1:0, false, "Upright tick-by-tick square trails. On/Off preserves your settings.");
            if(screen.sparkTracersOpen) {
                for(int k:GuardSparkTracerConfig.DISPLAY_ORDER) {
                    final int index=tracerOffset+k;
                    screen.slider(GuardSparkTracerConfig.LABELS[k],()->screen.configDraft.extraClient[index],0,100,"%",1,false,v->screen.configDraft.extraClient[index]=v,GuardSparkTracerConfig.TIPS[k]);
                }
                screen.particleAppearance(false,GuardParticleColors.TRACER_START_COLOR,GuardParticleColors.PREVIOUS_LENGTH);
            }
            screen.featureDivider("Center Impact", () -> screen.centerImpactOpen, () -> screen.configDraft.extraClient[GuardParticleColors.IMPACT_ENABLED]!=0, value -> screen.configDraft.extraClient[GuardParticleColors.IMPACT_ENABLED]=value?1:0, false, "Six sharp frames disperse into fading arcs. Uses tracer colors. On/Off preserves your settings.");
            if(screen.centerImpactOpen)screen.particleAppearance(false,GuardParticleColors.IMPACT_SIZE,GuardParticleColors.IMPACT_DURATION+2);

            }
            if (screen.page == 17) {
            screen.featureDivider("Shield Icon", () -> screen.shieldOpen, () -> screen.configDraft.hud, value -> screen.configDraft.hud=value, false, "Show or hide shield icon settings.");
            if (screen.shieldOpen && screen.configDraft.hud) {
                if (screen.configDraft.hud) screen.toggle(Control.SHIELD_EFFECTS, "Shield reactions", () -> screen.configDraft.shieldEffects, v -> screen.configDraft.shieldEffects = v, false,
                    "Enable the individual shield reactions below. Each reaction has its own controls. Default: On.");
                if(screen.configDraft.hud && screen.configDraft.shieldEffects) screen.buildShieldReactions(false);
                        }
            }
            if (screen.page == 2) {
            screen.dropdown("Screen Flash", () -> screen.screenFlashOpen, "Show or hide screen flash settings.");
            if (screen.screenFlashOpen) {
            screen.sliderBound(Control.FLASH_STRENGTH, "Screen flash", () -> screen.configDraft.flashStrength, "%", 1, false, v -> screen.configDraft.flashStrength = v, "Strength of the ordinary white screen flash. 0% disables it; meme flash still uses a full white flash. Blocks never trigger it. ");
            screen.toggle(Control.PERFECT_ONLY_FLASH, "Perfect only flash", () -> screen.configDraft.perfectOnlyFlash, v -> screen.configDraft.perfectOnlyFlash = v, false,
                "Show the ordinary white screen flash only on perfect parries. Meme flash still flashes on either parry. Default: On.");
            screen.toggle(Control.MEME_FLASH, "Meme flash", () -> screen.configDraft.memeFlash, v -> screen.configDraft.memeFlash = v, false,
                "Show a random image with a full white flash after hitlag. Holds 0.5 seconds, then fades for 0.2 seconds. Default: Off.");
                        }
            }
            if (screen.page == 2) {
            screen.dropdown("Camera Feedback", () -> screen.cameraFeedbackOpen, "Show or hide camera feedback settings.");
            if (screen.cameraFeedbackOpen) {
                screen.sliderBound(Control.SHAKE_STRENGTH, "Camera shake", () -> screen.configDraft.shakeStrength, "%", 1, false, v -> screen.configDraft.shakeStrength = v, "Strength of camera shake after a hit. Blocking shakes the screen the most. 0% disables it.");
                screen.sliderBound(Control.HITLAG_FRAMES, "Perfect parry hitlag", () -> screen.configDraft.hitlagFrames, " frames", 1, false, v -> screen.configDraft.hitlagFrames = v, "Freeze duration: each frame is 1/60 second. Impact holds for the same duration (minimum 1 frame). Feedback and queued damage follow completion.");
                screen.controlBindings.put("Regular parry hitlag", Control.REGULAR_HITLAG_FRAMES.binding(false));
                screen.slider("Regular parry hitlag", () -> screen.configDraft.extraClient[62], 0, 10, " frames", 1, false, v -> screen.configDraft.extraClient[62]=v, "Freeze duration for regular parries; each frame is 1/60 second.\n0 disables regular hitlag. Default: 0.");
                screen.toggle(Control.IMPACT_FRAMES, "Impact frame", () -> screen.configDraft.impactFrames > 0, v -> { screen.configDraft.impactFrames = v ? 1 : 0; screen.refreshControls(); }, false,
                    "Show one freshly captured grayscale image after hitlag. It lasts as long as the configured hitlag, or one frame if hitlag is zero. Default: On.");
                if (screen.configDraft.impactFrames > 0) {
                    screen.toggle(Control.IMPACT_PERFECT_ONLY, "Perfect only impact", () -> screen.configDraft.impactPerfectOnly, v -> screen.configDraft.impactPerfectOnly = v, false,
                        "Show the impact image only on perfect parries. Off lets regular parries show it too. Default: On.");
                    int previewRow = screen.controlIndex++;
                    if (screen.visible(previewRow)) {
                        screen.settingLabel(previewRow, "Live preview", "Tune brightness, contrast, edges, and grain over a captured view. Return to settings and press Apply to save.", true);
                        Button preview = screen.addControl(GuardUi.builder(Component.literal("Preview"), b ->
                            screen.openImpactPreview(false)).bounds(screen.settingX(), screen.row(previewRow), screen.settingWidth(), 20).build());
                        screen.tip(preview, "Open the impact frame preview. Changes return here and save only when you press Apply.");
                    }
                }
                        }
            }
            if (screen.page == 15) {
                screen.dropdown("Hand Animation", () -> screen.animationOpen, "Show or hide first-person and third-person guard pose settings.");
                if (screen.animationOpen) {
                    screen.toggle(Control.THIRD_PERSON_ANIMATION, "Third-person guard pose", () -> screen.configDraft.thirdPersonAnimation, v -> screen.configDraft.thirdPersonAnimation = v, false,
                        "Show the guarding pose on players and supported humanoid mobs. Off hides it only on your client.");
                    screen.toggle(Control.FIRST_PERSON_ANIMATION, "First-person guard pose", () -> screen.simplePoseDraftEnabled(),
                    v -> screen.configDraft.firstPersonAnimation = v, false,
                    "Animate weapon guarding with the simple pose. Used as fallback when Punchy is unavailable; compatible Punchy takes priority. Shields animate separately.");
                }
            }
            if (screen.page == 3) {
            screen.sliderBound(Control.MASTER_VOLUME, "Master volume", () -> screen.configDraft.masterVolume, "%", 1, false, v -> screen.configDraft.masterVolume = v, "Scale all Mallard Guard sounds you hear. 0% mutes them; above 100% uses louder mixes where available.");
            screen.sliderBound(Control.PERFECT_VOLUME, "Perfect volume", () -> screen.configDraft.perfectVolume, "%", 1, false, v -> screen.configDraft.perfectVolume = v, "Perfect-parry and associated meme sound volume, multiplied by Master Volume.");
            screen.sliderBound(Control.PARRY_VOLUME, "Parry volume", () -> screen.configDraft.parryVolume, "%", 1, false, v -> screen.configDraft.parryVolume = v, "Regular-parry and associated meme sound volume, multiplied by Master Volume.");
            screen.sliderBound(Control.BLOCK_VOLUME, "Block volume", () -> screen.configDraft.blockVolume, "%", 1, false, v -> screen.configDraft.blockVolume = v, "Block and shield-parry sound volume, multiplied by Master Volume.");
            }
        }

        if (screen.clientSection && screen.page == 2) {
            screen.dropdown("Chromatic Aberration", () -> screen.chromaticOpen, "Animated color separation after hitlag and impact finish. Both parry types use it by default; the impact preview controls a separate static effect.");
            if (screen.chromaticOpen) {
                screen.sliderBound(Control.CHROMATIC_INTENSITY, "Chromatic intensity", () -> screen.configDraft.extraClient[58], "%", 1, false, v -> screen.configDraft.extraClient[58] = v, "Strength of animated RGB separation. 0 disables it. Default: 200%.");
                screen.sliderBound(Control.CHROMATIC_TICKS, "Chromatic duration", () -> screen.configDraft.extraClient[59], " ticks", 1, false, v -> screen.configDraft.extraClient[59] = v, "How long color separation expands and settles. Duration also controls animation speed. Default: 6 ticks.");
                screen.toggle(Control.CHROMATIC_PERFECT_ONLY, "Perfect only chromatic", () -> screen.configDraft.extraClient[60] != 0, v -> screen.configDraft.extraClient[60] = v ? 1 : 0, false, "On: animate only perfect parries. Off: regular and perfect parries both trigger it. Default: Off.");
                screen.toggle(Control.CHROMATIC_HUD, "Chromatic affects HUD", () -> screen.configDraft.extraClient[61] != 0, v -> screen.configDraft.extraClient[61] = v ? 1 : 0, false, "On processes the HUD too. Off processes the world before HUD drawing. Config screens stay unaffected. Default: On.");
            }

        }

        if (screen.experimentalSection && screen.page == 14) {
            if (PunchyGuardCompat.installed()) {

            screen.toggle("Punchy Compatibility", () -> screen.configDraft.extraClient[65] != 0, v -> {screen.configDraft.extraClient[65]=v?1:0;screen.poseDraft.enabled=v;screen.refreshControls();}, false, "Use Punchy for guard animations when its animation API is compatible, including newer versions. Automatically fall back to the simple pose if integration is unavailable. Default: On.");
            screen.sliderBound(Control.PUNCHY_RELEASE_DELAY, "Release delay", () -> screen.configDraft.extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT], " ms", 1, false, v -> screen.configDraft.extraClient[GuardPoseSettings.RELEASE_DELAY_SLOT]=v, "Extra hold after hitlag finishes. No extra delay when hitlag does not occur. Applies to every Punchy preset. 0 removes the delay. Default: 50 ms.");
            screen.poseEditor(false);
            }
        }

    }
}
