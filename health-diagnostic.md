# Mallard Guard health diagnostic — 1.53d-beta-10

This is a source-level review of the mod's gameplay flow, hooks, UI, rendering, configuration, compatibility, and retained state. It includes targeted fixes and regression checks. It is not an in-game heap dump, frame profile, full NeoForge build, or certification that every mod combination works.

**Assessment:** the main systems have sensible separation, bounded particle storage, and substantial cleanup. I did find real correctness/UI problems, fixed below. The largest remaining low-end performance risks are dense tracer rendering, moving-head collision queries, and optional full-screen effects. I found no demonstrated unbounded world/entity leak in the reviewed cleanup paths. A numerical “chance of leaking” would be invented without runtime evidence. Punchy compatibility should remain beta until rapid animation use is verified in Minecraft.

## What the systems do

| Source family | Responsibility |
| --- | --- |
| MallardGuard | Initializes the mod and event listeners; resolves player incoming damage, shield blocks, retaliation and knockback suppression. |
| GuardState | Server player guard phases, timing, eligibility, hand choice, recharge, durability, movement and changed-status synchronization. |
| GuardItemRules | Exact item IDs, live tags, exact @namespace rules, keyword matching, shield recognition and attack-damage/consumable classification. Its parsed-rule cache holds at most 64 entries. |
| GuardMobState | Supported humanoid mob guarding, approach/tactical/pressure decisions, counter attacks, gear generation and temporary AI priorities. Uses weak mob keys and UUID targets. |
| GuardCombatEffects | Projectile defense/redirection, fall blast/launch, cone retaliation, pushback and stun. Gameplay remains server-owned. |
| GuardRetaliation | Defers returned damage until local feedback completes or a two-second server timeout. At most 16 pending feedback entries per player; targets/dimensions are identifiers. |
| GuardDamageRules | Damage-type eligibility, ten recent source IDs per player, runtime catalog and learned projectile/source relationships. Learned pairs are capped at 4,096 and serialized text is bounded. |
| GuardEffects, GuardSounds, GuardParticles | Select sounds, distribute nearby effect packets and register sound/particle types. Clients create and simulate the visual particles. |
| GuardPackets, GuardPoses | Bounded payloads, permissions/range validation, settings saving/sync, feedback completion and tracking-scoped third-person pose updates. Config saves acknowledge accepted sections independently. |
| GuardConfig, GuardSettingRanges, GuardControlSettings | Define config schemas/ranges, bind controls, preserve/migrate settings, create update prompts and clean obsolete entries. |
| GuardClientSettings, GuardClientPreset | Local appearance settings, stored presets, old-format migration and server enforcement. Reserved old slots preserve existing preset layouts. |
| GuardPoseSettings, GuardPoseLibrary, GuardPoseNames, GuardPunchyApi | Pose values, named preset persistence, unique names and optional Punchy API detection. Pose writes use temporary files; local library count is capped at 128. |
| GuardShieldReactions | Shared shield feedback values and preset storage. |
| GuardClient | Client input, synchronized guard status, local feedback/HUD, effect dispatch, enforcement and disconnect cleanup. |
| GuardAnimationHandler, GuardThirdPerson, GuardFeint | Simple first-person fallback, remote humanoid poses and optional Better Combat feint handoff. |
| PunchyGuardCompat | Optional animation ownership, entry/reaction/return sequencing, offhand visibility, previews and interruption. Reflection is resolved/cached; native clips are protected from old guard cleanup. |
| GuardParryParticleSpawner, GuardParryParticleMath | Shared counts, proportional head allocation, spherical launch sampling, random heavier side and pure motion/fade calculations. |
| GuardParryParticle | Flying Sparks and Spark Debris share storage/rendering mechanics but keep distinct motion, lifetimes, appearances and tails. Flying Sparks animate and linger variably; Debris falls more heavily and bounces with a short tail. |
| GuardSparkTracerParticle, GuardSparkPath, GuardSparkTrail | One simulated head, up to two contact-aware path sections for the current tick, and bounded primitive arrays of stationary upright tail squares. Whole square groups appear per tick; tail squares have no independent collision physics. |
| GuardParryParticleRenderer, GuardParticleCollisionProfiler | One shared material batch, distance/behind-camera culling and opt-in aggregate query timings. Hidden heads continue their normal physics. |
| GuardParticleConfig, GuardSparkTracerConfig, GuardParticleColors | Selected motion baselines, three intensity controls, editable sizes/colors/tail duration and color interpolation. Retired tuning slots remain migration storage. |
| GuardHitlag, GuardImpactFrame, GuardChromatic, GuardRenderTargets | Frame capture/freeze, impact/color-separation shaders and framebuffer copies with GL state restoration. |
| GuardShieldArtwork | Reloads shield texture coverage into small cached spans; image resources close after reading. |
| GuardConfigScreen, GuardConfigLoadingScreen, GuardConfigUpdateScreen, GuardDamageSourcesScreen | Main editor, server permission/settings loading, schema-update choices and damage-rule browsing. |
| GuardPoseManagerScreen, GuardPoseEditorScreen, GuardPoseMotionScreen, GuardPoseNameScreen, GuardPoseCopyScreen | Named pose management, preview/editing, motion controls, naming and copying. |
| GuardPoseItemScreen, GuardItemSearch | Shared assignment browser, search/filter categories, exact group rules and the cached Assigned list. Browsing names does not make gameplay keyword rules match names. |
| GuardColorPickerScreen, GuardColorPickerMath | Cached hue wheel/right-facing triangle, HSV math, exact hex input, acceptance/cancel and texture release. |
| GuardPreviewScreen, GuardImpactPreviewScreen, GuardUi, GuardUiLayout | Live previews, shared panel/button/dialog design, UI compositing and layout values. |
| Common mixins and GuardTridentReturn | Retaliation damage-state access, shield readiness override scoped to MG blocking, preserved trident return ownership and optional mixin gating. |
| Client mixins | Hand-render/attack-ticker access, shield release, humanoid/EMF poses, Better Combat feints, and Punchy pose/visibility/render/intent/reload/diagnostic hooks. |

## Verified findings fixed

| Finding | Result in beta-10 |
| --- | --- |
| The favored spherical launch axis could point downward, with no global upward weighting. | Launch sampling now favors upward movement, and the random heavier side is gently tilted upward. Full spherical coverage and downward launches remain. |
| Guard-break rejection followed isShieldBlocking, which already returns false while a break is pending. | Pending breaks are rejected first, preventing an extra vanilla shield block in the same tick after the limit. Ordinary unrelated shield blocks keep their existing behavior. |
| In single-player, client checks could access the server's non-thread-safe WeakHashMap of stunned entities. Even size checks can expunge weak entries. | Client paths return before accessing that map. Server stun behavior is unchanged. |
| Idle guard status sync constructed a record every tick even when nothing changed. | Compare primitive status fields first; create/send only when one changes. |
| Size 0 rendered nothing but still spawned and simulated heads. | Zero-size families are excluded before shared head allocation. |
| The grayscale shader evaluated the same ink treatment three times when chromatic separation was zero. | Evaluate it once and reuse the grayscale value. The chromatic path remains unchanged. |
| Mob color controls had no renderer consuming their values. | Removed those inactive menu controls/search entries. Legacy fields stay in config/payload storage for compatibility and are marked inactive. |
| Default-mode explanations could be swallowed by tooltip default-value stripping. Some intensity, durability and profiling descriptions were inaccurate or outdated. | Reworded modes, displayed-percent durability, shared budgets, tracer colors/lifetime and all-three-particle profiling. |
| Long tooltips were continuous paragraphs. | Shortened the longer explanations; sentence/default information gets deliberate line breaks when controls are built. Formatting is not repeated every frame. |
| Gameplay pose entry bypassed the configured blend duration. | Corrected in beta 11: entry starts immediately and blends over the Enter duration, matching motion previews. Placement previews remain instant. |
| Dialog text was not rendered; Help instructions only appeared through button tooltips. | Dialogs display wrapped instructions, with buttons below the text. |
| Ordinary submenu/dialog overrides returned false for pause. | Assignment-related menus, pose management/motion/naming/copying, damage browsing and shared dialogs inherit parent pause behavior. Live animation previews remain live. |
| Pending config updates were deduplicated only by object identity. | Deduplicate by normalized config path. |
| The beta-10 cleanup incorrectly removed damageStatus, which is used by damage-type toggles. | Restored the helper after the Chromebook compiler caught it; compiled the actual toggle with the helper and checked both call sites. |

The particle change affects launch distribution only. Existing speed sampling, gravity, drag, bounce, lifetime, tick-group stamping and color behavior remain in place. In 280,000 simulated launches, Flying Sparks were 71.89% upward, Debris 72.41%, and Tracers 74.47%. Downward launches were 28.12%, 27.59% and 25.53% respectively. These are statistical launch results, not an in-game visual approval.

## Remaining performance costs

**1. Tracer square count is the most important visual scaling concern.** Batching reduces draw submissions; it does not remove the work to generate/upload vertices or shade overlapping squares. There are still 24 squares per emitting head per tick. Increasing tail duration retains additional groups.

At the selected two-tick base tail and maximum lifetime variance, a trail can have four ticks of squares. At the 20-tick slider maximum, it can last 40 ticks; the physical head emits for at most 32 ticks. The largest per-head trail arrays therefore reserve 24 × 32 = 768 squares, using 24 KiB of primitive element storage. At 1,024 active tracer particles, that is approximately 24 MiB, excluding particle/array headers and render buffers. A sufficiently aligned worst-case population can draw up to 786,432 squares (3,145,728 vertices). Those are limits, not normal default usage. The default regular burst asks for 16 tracer heads, not 1,024.

**2. Head collisions still cost CPU.** Faster original sparks and tracers use up to two world path queries per moving tick; slower original sparks use a small collision volume. Tail squares perform no queries. Sleeping/expired particles stop movement work. Terrain complexity and repeated nearby bursts matter. Small Vec3/AABB/query objects are transient allocation and possible GC work, not evidence of a leak.

**3. Screen effects cost GPU bandwidth/fill rate.** Hitlag copies frames; chromatic separation and impact filtering run over the screen. Editor previews can keep full-screen targets and run blur/composition continuously. Assuming four-byte color pixels, one 1080p color copy uses approximately 8 MiB; multiple cached/preview targets can coexist. Their observed lifecycle is bounded and buffers are destroyed on resize/close/disconnect as appropriate. Reducing window resolution or optional screen effects is more relevant here than changing particle class count.

**4. Mob guarding scales with active supported mobs.** Entity tick listeners run for mobs, eligibility is checked while deciding/maintaining guard, and active guard goals update every tick. More involved decisions are staggered; guard AI does not scan the whole world every frame. Large combat crowds still warrant server profiling. Rule caches remain live-tag aware; blindly caching eligibility by Item alone would break stack-component/tag behavior.

**5. Occasional setup/I/O can spike.** A newly learned projectile/source mapping saves server config synchronously. UI assignment search scans the registry on filtering, not every render. Hue dragging regenerates the supersampled picker texture; idle picker frames reuse it. Those costs are situational, but can matter in large packs or on weak CPUs.

No FPS estimate is justified by source alone. Collision profiling measures queries, not complete rendering, mob AI, networking or full-frame cost.

## Memory retention and leak assessment

| Retained state | Bound/cleanup | Assessment |
| --- | --- | --- |
| Particle lists/history | Combined 1,024 active slots, finite lifetimes, stale/world pruning and disconnect clear | Bounded; high tail settings can still use significant memory. |
| Player guard/input/recent damage state | Per-player entries, logout/death/server-stop cleanup; recent sources capped at ten | No orphan-growth path demonstrated in normal reviewed lifecycle. |
| Deferred damage and feedback | Sixteen pending entries per player, two-second expiry; local callback queue capped at 256; logout/disconnect cleanup | Bounded queues. Cone target lists scale with entities affected within each pending event. |
| Mob state, projectile defenses, stun and sound history | Weak keys; target identifiers; explicit leave/server cleanup | Weak values do not ordinarily point back to their entity keys. Server stun map is now kept off client paths. |
| Third-person rendering references | Entity-leave/world-change cleanup; render begin/end clear temporary model/entity references | No ongoing old-world retention demonstrated. |
| Punchy | Weak player owners, explicit owned-state/transition/preview/session cleanup; cached methods/clips | Ownership cleanup is present. External API/clip internals still require integration testing. |
| GPU targets/picker/native images | Target replacement destroys old buffers; screen removal releases images/targets; disconnect releases feedback targets | No accumulating allocation found in the reviewed normal paths. One shader/VAO is cached for the application lifetime; that is bounded retained state. |
| Item rules/pose library | Sixty-four parsed rule strings; at most 128 local poses and bounded pose files; no stored target worlds | Intentional bounded caches/config data. |
| Old config snapshots | Old files up to 1 MiB each are retained before framework validation; acknowledged files are removed | Not globally capped: startup may retain snapshots for many unvisited outdated worlds. This is a real retention caveat, although it does not grow every tick. Clearing them blindly would lose migration/update information. |
| ThreadLocals | Returned-damage target and nesting flags restored/removed in finally | Exception checks confirm no target is left behind by the tested retaliation path. |

I would describe the reviewed leak exposure as **controlled in the usual gameplay paths, with unverified integration risks and a known old-config retention caveat**. I would not claim “zero leaks” or assign a percentage.

A useful runtime check is to repeat world join/leave, dimension changes, sustained parries, picker/preview opening/closing, window resizing and resource reloads. Compare retained heap after GC at the same idle state; track ClientLevel/entity/particle counts and native/GPU allocations separately. A higher temporary peak is not by itself a leak. Heap data cannot establish VRAM cleanup.

## Cheaper next steps, in order

1. Profile default bursts and dense multiplayer fights first. Compare collision summaries with profiling enabled, then frame/tick behavior with profiling disabled.
2. If tracer drawing dominates, constrain retained square/vertex work with a separate visual budget or measured distance-based detail. This is a visual tradeoff and has not been silently added.
3. If terrain queries dominate, use measurements to choose a cheaper head query strategy. Keep collision-aware bounce segments; removing them produces the wall-bridge artifact you rejected.
4. Debounce newly learned projectile-mapping disk writes if profiles show save spikes, with reliable shutdown flushing. This needs a persistence-focused change.
5. If picker dragging itself is slow, precompute invariant coverage/wheel data. This is lower priority than combat rendering because it only affects editing.
6. Eventually retire obsolete config/payload fields in an explicit migration/protocol change. Removing reserved positions casually risks breaking old presets.

Splitting the large GuardConfigScreen/PunchyGuardCompat classes could make maintenance easier, but is not automatically a runtime optimization. Shared particle math/rendering is useful reuse, not duplication simply because it lives in several files.

## Validation and remaining limits

Passed: Java syntax parsing for all 78 sources; item rules/live tags/eligibility/assignment UI fixtures; picker math/cache/texture-release fixtures; actual retaliation and rapid Punchy ownership method fixtures; tracer groups, bounce/query bounds, lifetime/color/size/default migration fixtures; preset backward compatibility; amount allocation/shared caps; targeted server-only stun lookup, unchanged-status allocation, tooltip formatting, visible dialog layout and same-tick shield-break callback checks; 280,000 launch distribution samples.

The API fixtures compile selected actual classes/methods against test doubles. They do not replace a Java 21 NeoForge build, Minecraft rendering verification, GLSL driver compilation, sustained heap/native profiling or external-mod compatibility testing. No Gradle/game build was run here; build and test the supplied source in your Chromebook environment. The release remains beta.

## Beta-10 build correction

The initial source archive failed compileJava because damageStatus(boolean) was incorrectly removed. This corrected archive restores it and retains the current 78-file structure. A focused compilation now includes the actual toggle method and both helper calls, with label/color checks before and after clicking. This is still not a full NeoForge build.


## Beta 12 follow-up

The source now contains 80 Java files. GuardItemBlockCounts handles bounded exact-item assignments and precedence; GuardShieldExpansionCompat handles optional API detection, automatic Item Only Mode/HUD configuration and live item stats. The assignment parser caches one bounded list (8,192 characters), stores no world/entity references, and validates counts before server config changes. Reflection resolves once; lookups read the current item-stat map so reloads do not retain stale maps. Existing per-item durability stays with the item.

Targeted fixtures cover all seven bundled limits, explicit unlimited overrides, weapon fallback, absent-mod behavior, persistence deduplication, datapack map replacement, timing/movement/blast values, editor apply/remove/pause behavior and packet roundtrips. They also check large-slider drag quantization, precise wheel changes, Shift-wheel changes, 150 ms defaults and legacy 500 ms timing caps. The original audit's remaining runtime/integration limitations still apply.

## 1.54c-beta-4 follow-up

The current user's Chromebook gameplay test passes their low-end/GPU acceptance check for tested conditions. No GPU fault is demonstrated, and no effect quality was reduced.

This follow-up implements owned rule compilation, cached assignment drafts, stable assigned-row scrolling controls, server/client save failure recovery, consolidated server persistence, disk-backed migration preservation, bounded snapshots in both network directions, and grouped tracer bookkeeping. Exact old/new trail comparison passed 30,720 configurations. See release-notes/1.54c-beta-4.md for validation boundaries and remaining optional redesign work.

## 1.54c-beta-5 features

Parry Healing is an optional server setting, disabled by default. Healing occurs once in the successful-attempt transition, outside the follow-up safety branch. Continuous Guard was removed in 1.54c-beta-9; normal attack input, guarding, recharge and Punchy behavior are restored. No additional per-player request state remains.
