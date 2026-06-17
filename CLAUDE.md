# WaxableItemFrames — project notes

Fabric mod that lets you **wax a filled item frame**: right-clicking a non-empty item frame with
honeycomb locks it (sets the vanilla `fixed` flag, like a glued frame), and right-clicking a waxed
frame with an axe removes the wax. Empty frames are unaffected. Multi-version via Stonecutter.
Server + client (`"environment": "*"`), `main` entrypoint (the work is entirely in a mixin), no
config UI (no YACL/ModMenu), no access wideners.

## Naming / layout (placeholders for the `backport` and `update` skills)

| Placeholder | Value |
|-------------|-------|
| `<modid>` | `waxableitemframes` |
| `<mod-id>` (mixins.json prefix / jar artifact / fabric id) | `waxableitemframes` |
| `<modpkg>` | `com.breadmoirai.waxableitemframes` |

(`<modid>` and `<mod-id>` are identical here — the fabric mod id, jar `base.archivesName`, package
suffix, and `mixins.json` prefix are all the un-hyphenated `waxableitemframes`.)

- **Shared main code:** `src/main/java/com/breadmoirai/waxableitemframes/`
  - `Waxableitemframes.java` — `ModInitializer` with an empty `onInitialize()` (all behaviour is the
    mixin; the entrypoint just exists so Fabric loads the mod).
  - `mixin/v21_8/ItemFrameEntityMixin.java` and `mixin/v26_1/ItemFrameEntityMixin.java` — the
    waxing logic, split into **versioned packages** because `ItemFrame.interact` gained a `Vec3`
    hit-location parameter in MC 26.x (see divergences below), so the `@Inject(method = "interact",
    at = @At("HEAD"), cancellable = true)` callback signature differs. Both `@Shadow` the inherited
    `fixed` flag (declared on `HangingEntity`). On the server only (`level().isClientSide()` guard):
    if the frame is non-empty and not fixed, honeycomb → wax on (consume one, play
    `SoundEvents.HONEYCOMB_WAX_ON`, `LevelEvent.PARTICLES_AND_SOUND_WAX_ON`, `fixed = true`); if
    fixed and held item is an `AxeItem`, axe → wax off (`SoundEvents.AXE_WAX_OFF`,
    `LevelEvent.PARTICLES_WAX_OFF`, `fixed = false`). Both paths `cir.setReturnValue(SUCCESS)`.
    The bodies are identical apart from the extra `Vec3 hitPos` callback parameter on `v26_1`.
    Both classes compile on every version; each version's `mixins.json` selects the right one (no
    stonecutter conditions or swaps needed — selection is purely via the per-version `mixins.json`).
  - `mixin/ItemFrameAccessor.java` — `@Accessor("fixed")` interface exposing the non-public
    `fixed` flag. **Used only by the game tests** to assert the waxed state; `@Accessor` remaps
    automatically across the named (1.21.x) and official/un-obfuscated (26.x) toolchains, so no
    access widener is needed.
- **Mappings:** official Mojang mappings (converted from the original Yarn source).
- **No access wideners** (no `.accesswidener` files, no `accessWidener` key in `fabric.mod.json`,
  no `loom.accessWidenerPath`). `@Shadow`/`@Accessor` on `fixed` cover the only non-public access.
- **Mixins config:** `versions/<v>/src/main/resources/waxableitemframes.mixins.json` (real
  per-version files; `mixins` array lists `ItemFrameAccessor` plus the version-appropriate
  `v21_8.ItemFrameEntityMixin` (1.21.x) or `v26_1.ItemFrameEntityMixin` (26.x), alphabetically
  sorted; `compatibilityLevel` `JAVA_21`).

## Versions

Supported: **1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2** (`vcsVersion = 26.2`).

- **1.21.x line** (1.21.8–1.21.11): normal `fabric-loom-remap` + Mojang-mappings path, Java 21,
  `build.gradle.kts`, `transformUnnamedVars` on switch.
- **26.x line** (26.1, 26.1.1, 26.1.2, 26.2): un-obfuscated / JDK-25 toolchain, registered in
  `settings.gradle.kts` via `versions("26.2", "26.1.2", "26.1.1", "26.1").buildscript("build.unobf.gradle.kts")`
  (plain `fabric-loom`, no Mojang mappings, `restoreUnnamedVars` on switch, `jar` not `remapJar`).
  The 26.1.x patches are API-identical to 26.1, so there is no Java source divergence between them.

The `vcsVersion` (newest version) holds the shared `src/`. Use the **`update`** skill to add a
newer MC version (it becomes the new `vcsVersion`) and **`backport`** for an older one; both read
the placeholders above.

### API divergences handled

- **`ItemFrame.interact` gained a `Vec3` parameter at 26.x** — `interact(Player, InteractionHand)`
  → `interact(Player, InteractionHand, Vec3)`. This changes the `@Inject` callback signature, so the
  mixin is split into `mixin/v21_8/ItemFrameEntityMixin.java` (2-arg) and
  `mixin/v26_1/ItemFrameEntityMixin.java` (3-arg, unused `Vec3 hitPos`); each version's `mixins.json`
  picks one. The game tests call `interact` through a single version-conditional helper
  (`//? if >=26.1 { 3-arg } else { 2-arg }`) in `WaxableItemFramesGameTests`.
- **`GameTestHelper.fail` takes a `Component`** (not `String`) — tests route failures through a
  `fail(helper, String)` helper that wraps with `Component.literal(...)` (stable across versions).

### Still-stable surface to watch on future updates

- `HangingEntity.fixed` field name, `LevelEvent.PARTICLES_AND_SOUND_WAX_ON` /
  `LevelEvent.PARTICLES_WAX_OFF` constants, `SoundEvents.{HONEYCOMB_WAX_ON,AXE_WAX_OFF}`,
  `AxeItem`, and `GameTestHelper.makeMockPlayer(GameType)` — all unchanged across 1.21.8–26.2.
- `stonecutter-swaps.gradle.kts` is currently empty (no cross-version symbol renames needed yet).

## Build (WSL2 / Windows filesystem)

Run via the Windows wrapper (`./gradlew` fails on WSL2). For task names **with spaces**, pass them
unwrapped — nesting quotes yields `Task '"Set' not found`:

```bash
cmd.exe /c gradlew.bat :1.21.8:compileJava
cmd.exe /c gradlew.bat "Set active project to 1.21.8"
cmd.exe /c gradlew.bat buildAndCollect   # builds every version into build/libs/<mod.version>/
```

- `genSources` for a version is the reliable way to verify mixin `@Inject`/`@Accessor` targets
  (compilation does **not** validate `@At` target strings).

## Game tests (server-side, headless)

Server game tests live in
`src/test/java/com/breadmoirai/waxableitemframes/testmod/WaxableItemFramesGameTests.java`
(Fabric `@net.fabricmc.fabric.api.gametest.v1.GameTest` + vanilla `GameTestHelper`), registered via
the `fabric-gametest` entrypoint in `src/test/resources/fabric.mod.json`. They are **server-side and
headless** — they run on every version (no client/display needed). The gametest API comes
transitively from `fabric-api`. `build.gradle.kts` wires a `gameTest` server run + `src/test` source
set; `runGameTest` is enabled via `-Dfabric-api.gametest` and writes `build/junit.xml`.

```bash
cmd.exe /c gradlew.bat "Set active project to 1.21.8"
cmd.exe /c gradlew.bat :1.21.8:runGameTest   # one version
cmd.exe /c gradlew.bat runGameTest           # all versions (also run in CI before publish)
```

Each test spawns a real `ItemFrame` (attached to a stone block), gives a mock player a held item,
calls `frame.interact(player, hand)` directly, and asserts the `fixed` flag (via `ItemFrameAccessor`)
and honeycomb consumption:
- `honeycombWaxesFilledFrame` — honeycomb on a filled frame → `fixed`, one honeycomb consumed.
- `axeRemovesWaxFromFrame` — wax, then axe → `fixed` cleared.
- `honeycombDoesNotWaxEmptyFrame` — empty frame stays unwaxed (the core guard).
- `axeDoesNotWaxUnwaxedFrame` — an axe never *adds* wax.

## Adding versions

Use the **`update`** skill for a newer MC version (becomes the new `vcsVersion`) and the
**`backport`** skill for an older one. Both read this file for the placeholders above.
