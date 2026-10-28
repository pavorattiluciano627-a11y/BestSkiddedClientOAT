# Launcher inputs (generated)

`minecraft.json`, `fabric.json` and `pins.json` are **generated**, not authored. They are the
Mojang version document and the Fabric loader profile for the Minecraft/Loader pair pinned in
`gradle.properties`, plus the SHA-256 of each document.

Generate them with network access to `piston-meta.mojang.com` and `meta.fabricmc.net`:

```sh
python3 tools/build/launcher_inputs.py
```

The script reads `minecraft_version` and `loader_version` from `gradle.properties`, verifies that
the version document is the requested release, refuses a Fabric profile that inherits a different
Minecraft version, and records whether the version still publishes obfuscation mappings
(26.x must not).

`tools/build/isolated_launcher.py` consumes these files and fails with an actionable message when
they are absent or when they describe anything other than the unobfuscated 26.2 release.
