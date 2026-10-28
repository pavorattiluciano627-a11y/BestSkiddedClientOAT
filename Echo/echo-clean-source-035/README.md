# Echo — editable source candidate

This package contains **977 readable application Java sources**, the real renderer/UI/assets,
native Rust/JNI projects, licensed STB1 source, and the JSVG/Rhino source patches and licenses.
No reconstruction checkout, shard directory, original application binary, or class graft is needed.

## Build

Tested host: macOS ARM64. Install **JDK 21**, **Python 3**, Rust **1.97.1**, and Xcode command-line tools.
Gradle 9.4 and public Maven/Cargo dependencies are downloaded or reused from checksum-verified caches.

```sh
rustup toolchain install 1.97.1
python3 tools/build_candidate.py
```

Output: `build/candidate/echo-clean-candidate-intermediary.jar` and
`build/candidate/provenance.json`. This is an **unapproved verification candidate**, not a release.
The builder runs ordinary Gradle compilation/native builds, then normal Loom remapping in a
separate bounded-heap project. Every application class remains in the input; a checked
inheritance/mapping closure reduces only remapper resolution memory.

The helper runs these local Gradle stages and records their commands before publishing provenance:

```sh
python3 tools/with_heavy_tool.py -- ./gradlew --no-daemon --max-workers=1 prepareCandidate
python3 tools/build/remap_closure.py
python3 tools/with_heavy_tool.py -- ./gradlew --no-daemon --max-workers=1 -p verification-remap candidateJar
python3 tools/build/publish_candidate.py
```

Java heaps stay at or below 512 MiB; native builds use one Cargo job, with the documented
4096 MiB Naga/media and 512 MiB STB limits. `ECHO_RUST_TOOLCHAIN` may select another installed
alias **only if its compiler release and commit match the pins** (for example, `stable`).

## Edit

Application code is in `src/main/java`; assets and actual 67+4 Mixin configurations are in
`src/main/resources`. Edit Java and rerun the builder. It records the actual input hashes
and identifies differences from `provenance/baseline-java.json`; edited builds do not inherit
the original runtime result. Native/dependency source changes require corresponding explicit
pin updates in `gradle/*inputs.json`, `native/media/BUILD-CONTRACT.json`, or
`third-party/source-provenance.json`.

Native projects: `native/renderer`, `native/lwjgl-compat`, `native/media`.
Windows media is Windows-only. The included target/toolchain contracts support reviewed GNU
cross-build inputs on POSIX hosts; Windows loading/SMTC and other host builds are not claimed tested.
Native resources are built from source, not copied from an original client.

## Readiness and limits

The exact rebuilt035 JAR and sidecar named in `provenance/runtime-ui-035.json`
**passed the specified real-world/dashboard UI flow** in run `clean-xocxorj6`:
98 unique allocations, zero skips, 97 registrations, six categories and null bootstrap failure;
two actual category/feature selections; animated close in **204.458292 ms at scale 1**;
a real world framebuffer after closing; all 98 feature-enabled flags unchanged; exit 0 in
21.37 seconds. The test used a **hidden native window** and only the disposable
GameOptions change `pauseOnLostFocus=false`. No close-flag override, native close request,
public flag-setter call or focus-stealing action occurred.

Before-world, before-dashboard and after-dashboard snapshots each observed **477 nonnull
exact-type metadata objects / 498 declarations / 548 declared slots / 579 actual list entries**,
with zero read errors or identity collisions. Raw comparisons remain **1682 match,
23 different-or-unresolved, 20 symbolic, 1 different-or-unreadable**. Separate analysis
explains the 24 flagged aggregates as 16 symbolic-reference aggregates, seven original
postInitialize selection vectors and one AutoCrystal size-one selection with unread Identifier.
The 31 extras are source-attributed 7/15/9 to InventoryActionScheduler/AnchorController/
ScaffoldController; direct runtime equality to provider fields was not measured.
**This is a specific UI-flow pass, not full raw-default, gameplay, global-fidelity or release approval.**

The earlier034 title/bootstrap evidence remains in `provenance/runtime-title-034.json`.
This035 source includes the reviewed ASSOC1466 and ASSOC1773 initializer corrections;
the034 artifact and failed visible035 attempts are preserved. Their unexplained close-flag
origin is not retrospectively claimed. Build-time sidecar flags remain immutable; the new
runtime receipt records the later test of that exact hash pair. Different rebuilt artifacts
require their own verification.

`build`, `assemble`, production `jar`/`remapJar`, and game-launch tasks retain the incomplete
release gate. Use the explicit candidate build above for source review/testing. The retained
source policy is a review aid; named JDK reflection/native APIs still require their documented
scope review and are not silently approved by a successful compile.

### Unresolved original source domains

The encoded-text factory's AccessRegistry JSON bytes and index-derived seed remain
dynamic: **no admitted actual payload/consumer contract is closed**. Multiplier
`1103515245` is supported only in the recorded key domain, not globally. No runtime
application decoder, `Chars` substitution, guessed plaintext or feature-table binding was added.

ScriptManager's six original first-stage string slots and the original
`new File(decodedString).toPath()` → map → engine effect order are accounted for,
but its admissible constructor long/char tuple and directory value remain unbound.
In particular, **`Echo/scripts` is not proved to be that constructor's directory**.
Title/bootstrap success and current UI checks do not prove these paths unused or
close their source/key/cache/initialization conditions. Details are in
`provenance/residual-source-limitations.json`.

Public dependencies are pinned; their licenses and source provenance are under `third-party/`.
Complete editable JSVG/Rhino source snapshots are in `third-party/dependency-sources/`;
the build compiles the seven local patches over verified public upstream libraries.
Loom's local Fabric remap derivatives use relocation-dependent names; public inputs
remain checksum-verified and actual generated classpath hashes are recorded per build.
Archive ordering/timestamps are deterministic. Native linker UUID/signature metadata
may vary with build location; each rebuilt candidate has its own hash-bound provenance.
Historical obfuscation-removal/cache/initialization conditions remain open. No all-feature or
all-setting parity claim follows from the title-screen milestone.
