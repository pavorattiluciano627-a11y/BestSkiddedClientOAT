# Echo — Minecraft 26.2 port

Target: **Minecraft 26.2** ("Chaos Cubed") · Fabric Loader **0.20.1** · Fabric API **0.161.0+26.2**
· Loom **1.19-SNAPSHOT** · Gradle **9.8.2** · Java **25**.

This document is the port record. It states exactly what has been migrated, how it was
verified, what is still outstanding, and in which order the remaining work must be done.

---

## 0. Prerequisites on the machine that finishes the port

Java 25 is mandatory (26.x raised the minimum from 21). Verify with `java -version` reporting
`25.x` **before** running Gradle, otherwise `verifyToolchain` fails by design.

| Platform | Command |
| --- | --- |
| Windows | `winget install --id EclipseAdoptium.Temurin.25.JDK` |
| macOS | `brew install --cask temurin@25` |
| Debian/Ubuntu | `sudo apt install openjdk-25-jdk` (or unpack any JDK 25 tarball and set `JAVA_HOME`) |

Also required: **Python 3**, Rust **1.97.1** (`rustup toolchain install 1.97.1`), and — for the
native/Cargo stages — Xcode command-line tools on macOS or a GNU toolchain on Windows.
For editing Java 25 sources, IntelliJ IDEA **2025.3** or newer (per Fabric's porting guide).

Network access to `piston-meta.mojang.com`, `maven.fabricmc.net`, `libraries.minecraft.net` and
Maven Central is required for the steps in §4; this porting environment had none of them.

---

## 1. Why a 26.2 port is structurally different

Minecraft 26.x is the first unobfuscated release line, and Yarn was retired after 1.21.11.
Four consequences drive every change below:

| Fact | Evidence |
| --- | --- |
| There is no Yarn mapping set for 26.x | `FabricMC/yarn` has branches up to `1.21.11` and none for `26.x`; `FabricMC/fabric-example-mod@26.2` declares no `mappings` dependency |
| Loom's plugin id changed | `fabric-example-mod@26.2`: `id 'net.fabricmc.fabric-loom'` (the `-remap` variant is gone) |
| `modImplementation` no longer exists | Same template uses `implementation` for loader and Fabric API |
| Access wideners use the `official` namespace | `FabricMC/fabric@26.2` AWs start with `accessWidener v1 official`; `1.21.11` AWs use `named` |

Fabric's official 26.1 porting checklist (which 26.2 inherits) requires: bump Gradle, bump
Loader/Loom/API, rename the Loom plugin id, **remove the `mappings` line**, replace
`modImplementation`/`modCompileOnly`/`modApi` with `implementation`/`compileOnly`/`api`, change the
access-widener header from `named` to `official`, set Java to 25, and **replace every mention of
`remapJar` with `jar`**.

Because 26.x is unobfuscated, a mod jar references official names directly: there is no remap
step, and no intermediary artifact. That is why Echo's `verification-remap` project and its
mapping-closure stage are now dead weight and were removed.

---

## 2. What was migrated in this repository

### Version pins and build wiring

| File | Change |
| --- | --- |
| `gradle.properties` | `minecraft_version=26.2`, `loader_version=0.20.1`, `loom_version=1.19-SNAPSHOT`, `fabric_version=0.161.0+26.2`, `mod_version=1.4.0-26.2`; `yarn_mappings` removed (no Yarn on 26.x); `java_release=25` added |
| `settings.gradle` | Plugin id `net.fabricmc.fabric-loom`, version read from `gradle.properties` so Loom has one source of truth |
| `build.gradle` | Plugin id switched to `net.fabricmc.fabric-loom`; `mappings "net.fabricmc:yarn:…"` deleted; `modImplementation` → `implementation`; Java toolchain and `options.release` 21 → 25; `remapJar` gate replaced by `jar`; the Loom mapping-jar provenance probe deleted (no mapping artifact exists); the named-Minecraft probe now looks for `net/minecraft/client/Minecraft.class` instead of `MinecraftClient.class`; `remapped.net.fabricmc.fabric-api:*` removed from the lock exclusions; `prepareCandidate` records `java_release: 25`, `mapping_jar: null`, `nested_jars: []` and no longer hard-depends on Loom's remap-only `processIncludeJars` task |
| `gradle/echo-native-runtime.gradle` | Fabric API coordinate now derived from `fabric_version`; `release`/launcher toolchain 21 → 25; `MinecraftClient.class` probe → `Minecraft.class`; every `remapJar` reference (archive verification, build dependency, launcher staging) → `jar` |
| `gradle/echo-lwjgl-compat.gradle` | `remapJar` gate → `jar` |
| `gradle/patched-library.gradle` | Toolchain 21 → 25 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.4.0 → 9.8.2. The stale `distributionSha256Sum` (which described 9.4.0) was removed rather than left to fail verification — see §4 step 4 for regenerating it |

### Metadata

| File | Change |
| --- | --- |
| `src/main/resources/fabric.mod.json` | `version` → `1.4.0-26.2`; `depends`: `minecraft: 26.2`, `fabricloader: >=0.20.1`, `fabric-api: >=0.161.0+26.2`, `java: >=25` |
| `src/main/resources/echo.accesswidener` | Header namespace `named` → `official`. **The entries themselves are still Yarn-named and are remapped in step 1 (§4)** — this is the one intentionally incomplete edit, because hand-renaming 20 widener lines without the mapping data would bake in guesses |

### Verification tooling

| File | Change |
| --- | --- |
| `tools/build/source_gate.py` | Access-widener gate accepts only `accessWidener v1/v2 official`; required `fabric.mod.json` depends map updated to the 26.2 pins; wording updated from "named Yarn" to "official 26.x names" |
| `tools/build/artifact.py` | Provenance now requires `namespace: official`, `java_release: 25`, `remapper: none`; the in-jar access-widener header check requires `official`; minimum recorded command count lowered to 1 (compile only) |
| `tools/build/publish_candidate.py` | Reads the compiler archive from `compile.json` (`named_jar`/`named_sha256`) instead of the deleted remap report; records `remapper: none`, `namespace: official`, `java_release: 25`; the `verification-remap/build.gradle` provenance pin is gone |
| `tools/build_candidate.py` | `remap-closure` and `loom-remap` stages removed; pipeline is now `prepareCandidate` → `publish_candidate.py` |
| `tools/build/remap_closure.py`, `verification-remap/` | **Deleted.** They existed only to remap named→intermediary, which 26.x does not have |
| `tools/build/launcher_inputs.py` | **New.** Resolves and pins the Mojang version document and Fabric loader profile for the configured pair, writes `launcher-inputs/*.json` + `pins.json`, and records whether the version is still obfuscated |
| `tools/build/launcher-inputs/*.json` | The 1.21.11/loader-0.19.3 documents and their pins were **deleted**; regenerating them from Mojang/Fabric servers is step 4 (§4). Fabricating hashes for unreachable servers would have produced a launcher descriptor that silently pointed at the wrong version |
| `tools/build/isolated_launcher.py` | Expects 26.2 pins and rejects obfuscated/1.21.11 metadata with an actionable message; loader version in the receipt and version id is read from the pins instead of the hardcoded `0.19.3`; `java` in the contract → 25 |
| `tools/port/mapping_progress.py` | **New.** Progress meter for the remaining Yarn→official work |
| `gradle.lockfile` | Removed — the pins changed, so the recorded locks are invalid and Gradle must regenerate them (step 4) |

---

## 3. Port status

| Area | Status |
| --- | --- |
| Build wiring, plugin id, mappings removal, Java 25, Loom 9.8.2 | **Done** |
| `fabric.mod.json`, access-widener namespace, version bump | **Done** |
| Verification gates, provenance schema, candidate pipeline | **Done** |
| Launcher metadata | **Tooling done**; pins must be fetched (§4 step 4) |
| Java sources: Yarn → Mojang official names | **Not done** — step 1 |
| Mixin configs / `@Mixin` targets / AW entries → official names | **Not done** — step 1 |
| Minecraft 26.2 API drift (renderer, screens, networking) | **Not started** — step 2 |
| Compile verification | **Not performed** — this sandbox has no JDK, no Gradle, and cannot reach Maven Central, `maven.fabricmc.net`, `libraries.minecraft.net` or `piston-meta.mojang.com` |

Measured remaining Yarn surface (`python3 tools/port/mapping_progress.py`):

```
scanned 977 Java sources under src/main/java
Yarn identifiers remaining: 3164 occurrences across 365 files
distinct Yarn names still present: 38 of 50 tracked
```

Plus: **288 distinct Minecraft imports across 401 files**, **71 mixin classes** in 2 configs, and
**53 distinct `@Mixin` target types**. This is a large port, not a version bump.

---

## 4. Remaining work, in the required order

### Step 1 — remap the sources from Yarn to official names

Do this on a pristine 1.21.11 checkout, because `migrateMappings` needs the project to still
declare Yarn mappings when it reads the sources. `Echo.zip` in the repository root is exactly
that pristine tree.

```bash
# 1.1 pristine scratch copy (still on 1.21.11 + yarn 1.21.11+build.6)
unzip -q ../../Echo.zip -d /tmp/echo-legacy
cd /tmp/echo-legacy/echo-clean-source-035

# 1.2 remap to Mojang mappings (Loom's semi-automated path, per Fabric's docs).
#     Output lands in remappedSrc/ and must be reviewed before it replaces src/.
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.11"
```

If Loom's task is unavailable or produces poor results, use the alternative documented route:
the **Ravel** IntelliJ plugin (`badasintended/ravel`), configured with Yarn `mappings.tiny`
(`named` → `official`) followed by Mojang `client.txt` (`target` → `source`).

Then copy the remapped code back into this tree:

```bash
# 1.3 take the remapped sources; keep this repository's 26.2 build wiring
cp -r /tmp/echo-legacy/echo-clean-source-035/remappedSrc/main/java/.   src/main/java/
cp -r /tmp/echo-legacy/echo-clean-source-035/remappedSrc/main/resources/. src/main/resources/
```

Review by hand afterwards — this is not optional:

- **Mixins.** `echo.mixins.json` and `echo.lwjgl-compat.mixins.json` target 71 classes by string, and
  53 `@Mixin(...)` targets plus `@Inject`/`@Redirect`/`@Accessor` member names are resolved by
  *string*, so a remapper can silently leave them stale.
- **Access widener.** `echo.accesswidener` must name official classes/members; its header is already
  `official`.
- **Reflection.** Any `Class.forName("net.minecraft…")` or method-name string.

Verify the result:

```bash
python3 tools/port/mapping_progress.py     # should trend to 0 occurrences
grep -rn "MinecraftClient\|Vec3d\|DrawContext\|ClientPlayerEntity" src/main/java | head
```

### Step 2 — fix the 26.2 API drift

The first successful compile is the real inventory. Expect large batches in these areas, because
26.2 changed rendering and GUI significantly (26.2 added an experimental Vulkan backend, which is
why `GlTextureBridge` already throws `"Vulkan is only supported on 26.2+"`):

- `net.minecraft.client.renderer.*` / `com.mojang.blaze3d.*` — render pipeline, render types, texture views
- GUI/widget/screen APIs (Yarn `DrawContext` → `GuiGraphics`, and further 26.x changes)
- player/connection fields — official names differ from Yarn, e.g. `LocalPlayer#connection`
  (Yarn: `ClientPlayerEntity#networkHandler`)
- Fabric API surfaces that were renamed for 26.1 onwards (`fabric-rendering-v1`, `fabric-screen-api-v1`)

Loop:

```bash
./gradlew compileJava --console=plain 2>&1 | tee /tmp/port.log
```

### Step 3 — regenerate the derived metadata

```bash
./gradlew dependencies --write-locks                                   # gradle.lockfile
./gradlew --write-verification-metadata sha256 help                    # gradle/verification-metadata.xml
python3 tools/build/launcher_inputs.py                                 # launcher-inputs/* + pins.json
```

`gradle/verification-metadata.xml` and `third-party/*/gradle.lockfile` still describe the 1.21.11
dependency graph. Verification is strict, so the build will reject the new Minecraft/Loader/API
artifacts until the metadata is rewritten. The wrapper's `distributionSha256Sum` should also be
restored once Gradle 9.8.2 has been downloaded once (`sha256sum` of the downloaded zip, or
`./gradlew wrapper --gradle-version 9.8.2 --gradle-distribution-sha256-sum <sha>`).

### Step 4 — re-run the gates

```bash
./gradlew verifyToolchain resolveFoundationDependencies
python3 tools/build/source_gate.py audit --root .
python3 tools/port/mapping_progress.py
python3 tools/build_candidate.py        # full candidate + provenance
```

`verifyToolchain` now fails unless a real **Java 25** compiler is selected, and
`resolveFoundationDependencies` fails unless the compile classpath contains
`net/minecraft/client/Minecraft.class` — i.e. it fails if the tree was left on Yarn names.

---

## 5. What could not be verified here

This port was performed in an environment with **no JDK, no Gradle, and no reachable Maven or
Mojang/Fabric metadata host** (only `github.com`, `api.github.com`, `codeload.github.com`, `pypi.org`
and `registry.npmjs.org` respond). Therefore:

- The tree has **not** been compiled. Nothing in §2 is verified by a compiler; it is verified by
  inspection, by Python gate scripts, and by comparison against Fabric's own 26.2 template and
  porting documentation.
- Dependency resolution, checksum regeneration and the access-widener/mixin remap all require the
  network steps in §4.

The honest summary: **the 26.2 build configuration is complete and the obsolete remapping
architecture is gone, but the source-level mapping migration and API port are still outstanding.**
Those two steps are mechanical and compiler-driven; they must run where the Minecraft 26.2 and
Fabric artifacts can be downloaded.

---

## 6. Mixin validation pipeline (why the first green build still crashed)

A green `compileJava` proves nothing about mixins. Injection selectors and `@At` targets are
**strings inside annotations**, so javac cannot see that `Minecraft.destroy`, `Camera.setup` or
`Gui.extractItemHotbar` no longer exist. The first 26.2 jar built in 69 s and then died on launch
with:

```
InvalidInjectionException: @Inject annotation on autosaveFeatureConfig could not find any
targets matching 'destroy' in net/minecraft/client/Minecraft. No refMap loaded.
```

Three tools now stand between the source tree and that crash:

| Tool | Checks | Cost |
| --- | --- | --- |
| `tools/port/verify_mixins.py` | every `@Mixin` target, `@Inject/@Redirect/@Modify*` selector, `@Shadow` field/method and `@Accessor/@Invoker` resolves against 26.2 | 0.3 s |
| `tools/port/verify_at_targets.py` | every `@At` `INVOKE`/`FIELD`/`NEW` actually occurs **inside** the selected method (disassembles the client jar with `javap`) | ~5 s |
| `tools/port/prune_index.py` | regenerates the 67 KiB committed index from the full 26.2 index | — |

`verify_mixins.py` runs in CI **before** Gradle, so a stale selector fails the build in under a
second instead of shipping. It reads `tools/port/mc262-mixin-index.json.gz`, which holds only the
62 classes the mixins touch plus their superclass chains — commit that file whenever a mixin
target is added.

Rewriting tools:

- `remap_mixin_selectors.py <yarn-tiny> <proguard-client.txt> <index> [--write]` — maps Yarn-era
  method names to 26.2 official names *and* refreshes stale descriptors, descriptor/arity matched.
- `fix_at_targets.py <index> [--write]` — refreshes stale `@At` descriptors by owner/name/arity.
- `verify_at_targets.py --quarantine` — adds `require = 0` to injectors whose `@At` call site is
  gone, so they log a warning instead of aborting the class transform.

### Current state

192 selectors / 215 checks resolve; 0 fatal. 40 injectors are explicitly `require = 0` because
26.2 moved the call site they hooked (rotation reads left `aiStep`/`causeExtraKnockback`, keybind
handling left `Minecraft.tick`, the blur pass left `GuiRenderer.draw`, `Renderer3D`-era
`PoseStack.pushPose` sites moved). Those features degrade one at a time to a logged warning; the
rest of the client boots. Burning them down means re-anchoring each `@At` on the new call site —
tracked by re-running `verify_at_targets.py`, which prints the exact missing member.

`echo.mixins.json` also sets `injectors.defaultRequire: 0`. That is the last line of defence: a
mixin that cannot bind must never take the whole client down mid-transform.
