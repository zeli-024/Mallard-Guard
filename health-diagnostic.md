# Source health review — 1.54e

The beta-9 baseline compiled successfully on Java 21 / NeoForge 1.21.1 and passed local gameplay testing. The 1.54e release preserves its gameplay, rendering and assets, with updated mob tracer/impact color defaults. This review does not establish results for sustained heap/native profiling or every external mod combination.

## Release status

Version 1.54e is a release package of the tested beta-9 source. Release metadata, upload classification, documentation and mob color defaults are updated. Config revision is 90; client preset revision 89 and payload protocol 68 are unchanged. Existing saved custom values are preserved. Existing buffer-constructor deprecation warnings remain non-fatal.

## Correctness and maintenance

- Editable config values have 102 explicit read/write bindings. Save ownership matches the existing rules, shield, mob, damage and policy groups, including settings shared by multiple groups. Array and map snapshots are independent copies; undo and partial save acknowledgement preserve draft edits.
- Controls bind to config identities or persisted preset slots. Numeric bounds are recorded at declaration and reused by sliders and packet validation. Visible labels no longer resolve config values.
- The enforced tracer intensity sliders previously indexed the shorter general-settings array. They now resolve the tracer setting itself.
- Shared sliders retain coarse dragging, precise wheel/arrow changes, Shift precision, and common rendering. A zero-width numeric range cannot produce a division by zero.
- Historical config notices and preset parsing are separated from current declarations and behavior. Existing keys, reserved positions and migrations are retained.
- Main menu search uses one immutable catalogue instead of rebuilding its entries for every query. Regular and perfect hitlag entries match the current menu.
- Literal tooltips use shared line breaks. Existing concise descriptions and specialized editor help remain available.
- Unused imports, redundant forwarding helpers, whitespace and obsolete art instructions are removed. License and contributor attribution files are retained.

## Performance

Combat, movement, collision, animation, item matching, mob decisions and rendering are preserved. The refactor targets maintenance and avoidable UI work; it does not establish a measured frame-rate improvement.

The dominant variable particle costs remain active head count, block collision queries, tracer square groups and visible geometry. Existing shared burst/active limits and the shared atlas batch remain. Histories use bounded primitive arrays; the tracer tail control remains limited to four ticks. Full-screen capture and filtering remain GPU bandwidth costs. Diagnostic collision profiling remains opt-in.

Item assignment uses compiled exact IDs and live group rules, with bounded transient caching and owner-held compiled lists. Group-heavy rules can still cost more than exact IDs; no per-tick file reads or registry-wide searches were added. Config transfer and disk saves are infrequent operations, outside the particle/render loop.

Longer Flying Sparks and Debris defaults can retain more simultaneous particles at the same burst rate. The existing global head limit still applies. Tracer lifetime, tail drawing and effect quality are unchanged.

## Retention and cleanup

| State | Lifecycle / limit | Review |
| --- | --- | --- |
| Server player combat state | Logout, death/respawn and shutdown | UUID ownership and explicit removal retained |
| Server palette cache | One entry per connected player; logout/shutdown removal | Immutable RGB records; no entity/world references in entries |
| Client policy/palettes | Disconnect and inactive-session reset | Immediately restores personal settings and clears palettes/pending feedback |
| Config transfers | 2 MiB per transfer, 16 concurrent uploads, 15-second timeout | Byte arrays released on completion, rejection, timeout and disconnect/shutdown |
| Mob state/projectile defense | Weak entity keys, leave/shutdown cleanup | State holds target UUIDs rather than target entities |
| Custom particles | Finite lifetimes, active caps, stale/world pruning and disconnect clear | Shared state lock remains around tick/render/history mutation |
| Hitlag callbacks | Bounded queue; death/disconnect clear | Pending actions do not survive the local combat session |
| UI drafts/previews | Screen lifetime; snapshots copy mutable values | No static screen/draft cache introduced |
| GPU buffers and picker textures | Replacement/resize/close/session cleanup | Bounded cached resources; no accumulating creation path identified |
| Config edit transactions | try/finally cleanup | ThreadLocal entries, backups and queued notifications released |

No obvious unbounded entity/world retention was identified in these reviewed paths. This is not proof that every external mod combination is leak-free. Sustained heap/native profiling is the way to verify accumulation during play.

## Defaults and compatibility

Flying Sparks: 20 ticks. Spark Debris: 30 ticks. Spark Tracers: 12 ticks. Weapon and shield perfect windows: 2 ticks. Regular windows: 6 ticks. Existing intensity, color, equipment, healing and animation defaults otherwise remain.

Config/preset revision: 89. Payload protocol: 68. The wire layouts are unchanged, but the newer preset revision requires matching clients and servers. Existing saved custom values are preserved; resets use the new defaults.

## Source checks completed

- Structure and argument checks across all 91 Java source files.
- Exact executable-source comparison for retained combat, motion, collision, rendering, optional-mod hooks and mixins; all resource bytes retained.
- Wire record arities, existing serializers, four released transfer buffers and upload limits checked.
- All 102 draft bindings and every existing server save group checked against the previous source.
- Page/control overloads, direct cross-class access, stable control references and numeric-bound registration checked.
- Public packet record ownership, pending-update type ownership and unqualified calls in the page builder checked explicitly.
- New defaults, identical legacy preset parsing logic and disconnect/shutdown cleanup checked.
- Cleanup/export shell syntax and source archive integrity checked.

The baseline build and gameplay checks passed. Source and packaging checks cover the updated mob defaults, default-change notices, tooltip values, preserved gameplay/resource bytes and release classification. No additional compilation or game execution was performed for this package.

## Local verification

Check reset/undo/apply and closing unsaved drafts; enforce each client category and reconnect; use the tracer enforcement sliders; exercise item assignments and both click buttons; rapidly parry with and without Punchy; die/respawn and change dimensions; check multiplayer colors and hitlag feedback; verify mob guarding and gear. Compare long-running memory behavior after repeated world joins and preview opening/closing.
