# Source responsibilities

The combat engine and existing compatibility adapters retain their behavior. Related responsibilities now have explicit owners instead of sharing editable screen state or packet transport state.

| Area | Owner | Responsibility |
| --- | --- | --- |
| Combat | GuardState, GuardCombatEffects, GuardRetaliation, GuardMobState | Guard windows, damage outcomes, retaliation and mob behavior |
| Config declarations | GuardConfig, GuardClientSettings, particle config classes | Persisted keys, defaults, ranges and client category membership |
| Numeric metadata | GuardSettingRanges, GuardControlSettings | Shared declared bounds, stable control identities, display scaling and drag precision |
| Config compatibility | GuardConfigMigration, GuardPresetMigration | Historical default notices, old file keys and serialized preset formats |
| Config transfer | GuardConfigTransfer | Size-limited uploads/snapshots, timeouts and session cleanup |
| Wire payloads | GuardPackets | Packet records, codecs, handlers and validated saves |
| Server colors | GuardPlayerPalettes | Per-player palettes, enforcement and join/leave publication |
| Client policy | GuardClientPolicy | Personal/enforced settings and connection-scoped palette cache |
| Config draft | GuardConfigDraft | Explicit read/write bindings, saved snapshots and save-group ownership |
| Config pages | GuardConfigPages, GuardConfigSearch | Page content and immutable search catalogue |
| Main screen | GuardConfigScreen | Navigation, common row controls, permissions, undo and save acknowledgements |
| Shared UI | GuardUi, GuardUiLayout, GuardSlider | Theme, field/button creation, layout, scrolling, tooltips and slider behavior |
| Particles | Existing particle behavior, history, spawner and renderer classes | Launch physics, bounce, animation, bounded histories and one atlas batch |
| Compatibility | GuardPunchyApi, PunchyGuardCompat, GuardShieldExpansionCompat, mixins | External APIs and renderer hooks |

Use explicit `Control` or preset-slot bindings when adding a control. Display labels are presentation; they must not resolve a config value. Declare numeric bounds through GuardSettingRanges so controls and validation share them. Keep historical preset positions reserved. Add new drafts to GuardConfigDraft with their correct save groups. Editors use shared widgets, while retaining specialized previews and item-grid behavior.

Saved custom values remain intact. Reset uses current defaults. Server edits remain operator-only, and forced client categories remain locked. Client and server must run the same release.
