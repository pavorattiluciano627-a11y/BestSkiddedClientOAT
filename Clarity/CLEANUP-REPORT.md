# Clarity 1.1.5b — de-licence / RAT audit report

**Artifact:** `clarity-1.1.5b-clean.jar` — 1,491 unique entries, 1,136 classes, 322 assets,
15,835,513 bytes, `testzip OK`, sha256 `fbfa3c7d123f7aa6…`
**Built for:** Fabric Loader ≥ 0.19.5, Minecraft ~26.1.2, Java 25+, Fabric API
**Reproducible:** any run of `rebuild_jar.py` on the same input yields byte-identical output
(deterministic timestamps, sorted entries, fixed compression).

---

## 1. What the drop actually is

`Official stage 0.jar` is **not** a mod-only agent package. It is a complete Fabric mod:

| Field | Value |
|---|---|
| `id` | `clarity` |
| `version` | `1.1.5b` |
| `name` | Clarity |
| `license` | CC0-1.0 |
| `environment` | `client` |
| entrypoint | `need.clarity.Clarity` (`implements ModInitializer`) |
| mixins | `clarity.mixins.json`, `required: true`, `compatibilityLevel: JAVA_25`, 112 mixins |
| depends | `fabricloader >=0.19.5`, `minecraft ~26.1.2`, `java >=25`, `fabric-api *` |

The jar as shipped is **structurally corrupt**: local file headers have zeroed
`compressed_size`/`uncompressed_size` and every entry is terminated by a data descriptor.
`unzip` reports *"overlapped components (possible zip bomb)"*; `testzip()` fails on the first
entry; `unzip -p` cannot read `fabric.mod.json` or `clarity.mixins.json`; **0 of 350 assets**
extract. This is packaging damage, not encryption — all bytes are present. Recovery therefore
scans local headers and inflates each entry up to the next `PK\x03\x04` signature, discarding
the trailing `PK\x07\x08` descriptors.

`OfficialLoader.jar` (492,022 B, 50 entries) is a valid jar and is a **Java agent**, not needed
for play:

```
Premain-Class: need.clarity.agent.ClarityAgent
Main-Class:    need.clarity.agent.loader.Loader
Can-Retransform-Classes: true
```

---

## 2. Authentication — removed

Clarity ships a paid-licence system. It lives **entirely** in `need/clarity/agent/`
(149 classes, 879 KiB) and works like this:

1. `agent.loader.LoaderPanel` — Swing loader window. `SERVER_WS_URL =
   "wss://104-156-154-148.sslip.io/load"`.
2. `agent.loader.Injector` — computes a hardware fingerprint (`computeHwid`), then uses
   `com.sun.tools.attach.VirtualMachine` to attach to the running JVM and calls
   `loadAgent(jar, wsUrl \t licenceKey \t hwid \t uuid \t build \t statusFile)`.
3. `agent.net.ClarityConnect` — opens the WebSocket and sends `licenceKey`, `hwid`, `uuid`,
   `pid`. The server replies with a **signed class manifest**, verified with
   `verify(PublicKey, …)` before any payload class is defined.
4. `ClarityAgent` / `ClarityTransformer` / `WatermarkSlot` + bundled ASM perform the
   retransformation and watermarking.

**The mod does not need any of it.** `need.clarity.Clarity.onInitialize` is a plain
`ModInitializer` and never touches `need.clarity.agent`; `ClarityBootstrap` reaches the mod
through `Class.forName("need.clarity.Clarity")` plus a reflective `onInitialize` call. There is
an offline path already.

**Action taken:** the whole package is dropped. `--strip-loader` removes
`need/clarity/agent/` (179 entries in total, including the agent manifest and bundled ASM).

**Why the whole package and not just the network classes:** `agent/` is self-referential
(its own ASM copy, its own loader graph). Per-feature stripping leaves dangling classes —
the first attempt with five narrow prefixes removed 30 classes and left 149 classes still
referencing `need/clarity/agent/`. Verified before deletion: **0 classes outside
`need/clarity/agent/` reference any class inside it**, so the cut is clean. Result: 1,315 →
1,136 classes, and 0 occurrences of `licenceKey` / `ClarityConnect` / `computeHwid` in the
rebuilt jar.

---

## 3. RAT audit — no remote-access trojan found

Searched for the things that make a client a RAT:

| Vector | Result |
|---|---|
| Shell execution (`getRuntime().exec`, `ProcessBuilder` with user-controlled args) | **None.** No `getRuntime().exec`. 4 `ProcessBuilder` sites, all inspected: `playerctl` / `nowplaying-cli` media lookups and an OS file-manager reveal. |
| Keylogger / input capture | **None.** No global key hooks, no keystroke buffering, no clipboard exfiltration. |
| Browser cookies / saved logins (`Login Data`, `Cookies`, wallets) | **None.** No access to Chrome/Firefox/Edge profile paths, no `wallet.dat`, no crypto-wallet paths, no `%APPDATA%` scraping. |
| Registry / persistence / autorun | **None.** No registry writes, no scheduled task, no startup-folder install. |
| Arbitrary remote class loading | Only inside `agent/` (the licence manifest) — **removed**. |
| Hardcoded C2 in the mod proper | **None** after the presence fix below. |

**Residual outbound endpoints in the clean jar, all deliberate and explained:**

| Endpoint | Purpose |
|---|---|
| `login.microsoftonline.com/consumers/oauth2/v2.0/{authorize,token}` | Alt manager — Microsoft account login |
| `user.auth.xboxlive.com/user/authenticate`, `xsts.auth.xboxlive.com/xsts/authorize`, `auth.xboxlive.com` | Xbox Live / XSTS token exchange for those alts |
| `api.minecraftservices.com/authentication/login_with_xbox`, `/minecraft/profile` | Minecraft session + profile lookup |
| `localhost:7420` / `127.0.0.1:7420` | Local IPC to the bundled media bridge |

The alt manager is a normal, user-initiated OAuth flow talking only to Microsoft's own hosts.
It is kept. Delete `need/clarity/alts/**` and `need/clarity/friends/online/MojangAuth*` if you
want it gone.

### The one real phone-home (fixed)

`need/clarity/friends/FriendSettings.java:155` declares `"Online Features"` default **`true`**,
and `need/clarity/friends/online/PresenceClient.java:42` hardcodes

```java
public static final String DEFAULT_URL = "wss://104-156-154-148.sslip.io";
```

So a stock build dials the vendor's relay **on launch, without asking**, and sends a presence
payload: your status, whether you're online, **the server you are currently on**, and your
friend/request preferences (`PresenceClient.statusMessage`, `friendsMessage`, `ping`).

This is not code execution — it is telemetry you did not opt into, over the same relay the
licence system talks to. It is **disabled** in the clean build: the host literals and the
`169.128.191.166` / `169-128-191-166` aliases are replaced with same-length zeroed strings, so
the class constant pool layout is untouched and the host simply stops resolving. Four literals
patched across `FriendSettings.class` and `PresenceClient.class`. Belt and braces: also turn
`Online Features` off in **Friend Settings** to keep the whole subsystem local.

---

## 4. Minecraft 26.2 — does not work, by design

The jar declares `minecraft: ~26.1.2`, so Fabric will refuse to load it on a 26.2 instance
before a single mixin runs. That is the correct outcome: the source is 26.1.2-shaped and
**21 mixin injections are already broken against 26.2** (checked with `verify_mixins.py` against
the 26.2 class index). Fatal, i.e. `require = 1` and would crash on launch:

| Target | Broken member |
|---|---|
| `Minecraft` | `setScreen` (`SetScreenMixin`, `AntiGuiCloseMixin` inject; `MainMenuRedirectMixin` `@ModifyVariable`) |
| `ScreenEffectRenderer` | `renderFire` ×2 |
| `LevelRenderer` | `renderLevel(GraphicsResourceAllocator, …)`, `extractVisibleEntities(Camera, Frustum, DeltaTracker, LevelRenderState)`, `lambda$addMainPass$0` |
| `ItemInHandRenderer` | `renderArmWithItem` ×5 (`@Inject` ×2, `@Redirect` ×2, `@WrapOperation`) |
| `Gui` | `displayScoreboardSidebar`, `extractCrosshair`, `extractConfusionOverlay`, `extractPortalOverlay`, `extractSpyglassOverlay`, `extractScoreboardSidebar`, `extractVignette` |

The signature drift is real, not cosmetic — e.g. the 26.2 `lambda$addMainPass$0` takes a
`FeatureRenderDispatcher$PreparedFrame` where 26.1.2 took a `ResourceHandle`, so the injected
locals shift. Optional (`require = 0`, silent no-op): `PlayerTabOverlay.render` /
`renderTablistScore` in `NameProtectScoreboardMixin`.

**Conclusion:** shipping this as a 26.2 build would produce a jar that crashes on launch. It is
shipped for 26.1.2, where it loads. A 26.2 port is a separate job: 21 injections to re-anchor,
mostly in the render pipeline, plus a Loom build to re-verify.

---

## 5. How to rebuild / verify

```bash
python3 tools/clarity/rebuild_jar.py \
  "Clarity/discord.gg-clarityclient-source-main/Official stage 0.jar" \
  out/clarity-1.1.5b-clean.jar \
  --strip-loader --neutralise-presence
```

`rebuild_jar.py` scans local headers, inflates each entry up to the next `PK\x03\x04`, strips
trailing `PK\x07\x08` descriptors, dedupes repeated names, drops `need/clarity/agent/`,
zeroes the presence literals, and writes a deterministic jar (`date_time` 1980-02-01, deflated,
sorted). `.github/workflows/clarity-release.yml` runs exactly this and fails the build unless:
zip integrity OK, agent package absent, no `licenceKey`/`ClarityConnect`/`computeHwid`, no
presence IP, no vendor relay, `need/clarity/Clarity` entrypoint intact, `fabric.mod.json` and
`clarity.mixins.json` present.

**Install:** drop `clarity-1.1.5b-clean.jar` into `mods/` on a Fabric **26.1.2** profile with
Fabric API. No loader wrapper, no key, no attach.

### Source-tree note
`Client Source/need/clarity/**` and `Client Source/source/need/clarity/**` are **byte-identical
duplicates** (`diff -rq` returns 0 lines). Edit one, not both. The drop contains no Gradle or
Maven build, so the shipped jar is the practical artifact; a from-source build needs a Loom
26.1.2 project written from scratch.
