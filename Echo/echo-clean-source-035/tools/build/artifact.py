"""Validate bytes and source/build provenance without loading application classes."""
from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[2]
ORIGINAL_MIXINS = [
    "ClientConnectionMixin", "PacketByteBufMixin", "PersistentProjectileEntityAccessor",
    "HandledScreenAccessor", "InputAccessor", "EntityAccessor", "FireworkRocketEntityAccessor",
    "KeyBindingAccessor", "LivingEntityAccessor", "LivingEntityRendererAccessor", "GameRendererAccessor",
    "MinecraftClientAccessor", "MouseAccessor", "ClientPlayerInteractionManagerAccessor",
    "SpecialGuiElementRendererAccessor", "PlayerEntityAccessor", "CameraMixin", "GameOptionsMixin",
    "WindowMixin", "PlayerListEntryMixin", "MinecraftClientMixin", "ClientPlayerInteractionManagerMixin",
    "PlayerEntityRendererMixin", "EntityTargetingMixin", "EntityRendererMixin", "FireworkRocketEntityMixin",
    "PlayerInventoryMixin", "ItemMixin", "ItemStackUsageMixin", "LivingEntityRotationMixin",
    "LivingEntityRenderRotationMixin", "ClientPlayerEntityMixin", "PlayerRotationMixin", "InputMixin",
    "InputUtilMixin", "KeyboardMixin", "KeyboardInputMixin", "KeyBindingMixin",
    "LunarInputCompatibilityMixin", "LunarKeyStateCompatibilityMixin", "TipTapShowKeyStateMixin",
    "MouseMixin", "TextRendererMixin", "HeldItemRendererMixin", "RenderTickCounterDynamicMixin",
    "ChunkRenderingDataPreparerMixin", "OrderedRenderCommandQueueImplMixin", "ChunkOcclusionDataBuilderMixin",
    "BlockRenderManagerMixin", "AbstractBlockAbstractBlockStateMixin", "EndCrystalEntityRendererMixin",
    "FluidRenderingMixin", "FogRendererMixin", "GameRendererMixin", "GuiRendererMixin", "GuiRenderStateMixin",
    "ScreenMixin", "InGameHudMixin", "WorldRendererFramebufferMixin", "WorldRendererEntityStateMixin",
    "LightmapTextureManagerMixin", "InGameOverlayRendererMixin", "SkyRenderingColorMixin",
    "SkyRenderingStarsMixin", "SoundSystemMixin", "ProjectileUtilMixin", "ClientWorldMixin",
]
COMPAT_MIXINS = ["NativeImageLegacyResizeMixin", "MinecraftLegacyDialogMixin",
                 "NarratorLegacyDialogMixin", "WindowLegacyDialogMixin"]
ENTRYPOINTS = {"main": ["dev.echo.client.bootstrap.EchoInitializer"],
               "preLaunch": ["dev.echo.client.EchoPreLaunch"]}
NATIVES = {"renderer": "native/macos-aarch64/libnaga_jni.dylib",
           "lwjgl-compat": "native/macos-aarch64/libecho_stb_legacy.dylib"}
CONFIGS = ["echo.mixins.json", "echo.lwjgl-compat.mixins.json"]


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_digest(value):
    return sha(json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode())


def safe_name(name):
    require(isinstance(name, str) and name and "\\" not in name and "\x00" not in name,
            "Invalid relative path")
    path = PurePosixPath(name)
    require(not path.is_absolute() and ".." not in path.parts and str(path) == name,
            "Noncanonical/escaping relative path: " + name)
    return name


def local_path(root, name):
    path = (root / safe_name(name)).resolve()
    require(path.is_relative_to(root.resolve()), "Source/evidence escapes checkout: " + name)
    return path


def verify_file(root, name, expected):
    require(isinstance(expected, str) and re.fullmatch("[0-9a-f]{64}", expected), "Invalid SHA-256")
    path = local_path(root, name)
    require(path.is_file() and file_sha(path) == expected, "Input SHA-256 mismatch: " + name)
    return path


def evidence(root, records):
    require(isinstance(records, list) and records, "Actual build evidence required")
    for record in records:
        path = verify_file(root, record["path"], record["sha256"])
        require(path.stat().st_size > 0, "Empty build evidence")


def members(archive):
    names = [i.filename for i in archive.infolist() if not i.is_dir()]
    require(len(names) == len(set(names)), "Duplicate archive member")
    for item in archive.infolist():
        safe_name(item.filename.rstrip("/"))
        require((item.external_attr >> 16) & 0o170000 != 0o120000, "Archive symlink forbidden")
    return set(names)


def macho_cpus(data):
    if len(data) < 8:
        return []
    if data[:4] in (b"\xcf\xfa\xed\xfe", b"\xce\xfa\xed\xfe"):
        return [struct.unpack_from("<I", data, 4)[0]]
    if data[:4] in (b"\xfe\xed\xfa\xcf", b"\xfe\xed\xfa\xce"):
        return [struct.unpack_from(">I", data, 4)[0]]
    if data[:4] in (b"\xca\xfe\xba\xbe", b"\xca\xfe\xba\xbf"):
        count = struct.unpack_from(">I", data, 4)[0]
        stride = 32 if data[:4] == b"\xca\xfe\xba\xbf" else 20
        if count <= 20 and len(data) >= 8 + count * stride:
            return [struct.unpack_from(">I", data, 8 + i * stride)[0] for i in range(count)]
    return []


def config_check(archive, names, filename, package, common, client, plugin=None):
    config = json.loads(archive.read(filename))
    require(config.get("package") == package and config.get("required") is True,
            "Wrong required Mixin package: " + filename)
    require(config.get("compatibilityLevel") == "JAVA_21" and
            config.get("injectors", {}).get("defaultRequire") == 1, "Weakened Mixin requirements")
    require(config.get("mixins", []) == common and config.get("client", []) == client and
            not config.get("server"), "Original/compatibility Mixin inventory or order changed")
    require(config.get("plugin") == plugin, "Mixin plugin changed")
    for name in common + client:
        require(package.replace(".", "/") + "/" + name + ".class" in names, "Configured Mixin class absent: " + name)
    if plugin:
        require(plugin.replace(".", "/") + ".class" in names, "Mixin plugin class absent")
    if config.get("refmap"):
        require(config["refmap"] in names, "Configured refmap absent")


def validate(jar_path, provenance_path, expected_jar, expected_provenance, root=ROOT):
    require(re.fullmatch("[0-9a-f]{64}", expected_jar or "") and
            re.fullmatch("[0-9a-f]{64}", expected_provenance or ""), "Explicit artifact and provenance SHA-256 required")
    require(file_sha(jar_path) == expected_jar, "Explicit client SHA-256 mismatch")
    raw = Path(provenance_path).read_bytes()
    require(sha(raw) == expected_provenance, "Explicit provenance SHA-256 mismatch")
    ledger = json.loads(raw)
    require(ledger.get("schema") == "echo-clean-runtime-artifact/v1" and
            ledger.get("mode") == "java-rust-source" and ledger.get("namespace") == "official",
            "Clean Java/Rust official-namespace provenance required")
    require(ledger.get("artifact_sha256") == expected_jar, "Provenance artifact binding differs")
    sources = ledger["source_files"]
    require(isinstance(sources, dict) and sources and
            canonical_digest(sources) == ledger.get("source_set_sha256"), "Source-set digest differs")
    for name, digest in sources.items():
        require(not name.endswith((".class", ".jar", ".jasm")), "Opaque input is not source")
        verify_file(root, name, digest)
    build = ledger["build"]
    # 26.x is unobfuscated: there is no remapping stage, so the artifact is the
    # compiler output itself and the attestation records `remapper: none`.
    require(build.get("application_compiler") == "javac" and build.get("java_release") == 25 and
            build.get("remapper") == "none" and build.get("application_bytecode_grafts") is False and
            build.get("opaque_application_inputs") is False, "Clean compiler attestation required")
    require(isinstance(build.get("commands"), list) and len(build["commands"]) >= 1 and
            all(isinstance(c, list) and c and all(isinstance(a, str) for a in c) for c in build["commands"]),
            "Actual compile commands required")
    evidence(root, ledger["build_evidence"])
    with zipfile.ZipFile(jar_path) as archive:
        names = members(archive)
        require(set(ledger["archive_entries"]) == names, "Archive-entry inventory differs")
        for name in names:
            payload = archive.read(name)
            require(sha(payload) == ledger["archive_entries"][name], "Archive-entry SHA-256 mismatch: " + name)
            require(not name.startswith(("hack/", "dev/dov/", "echo/bootstrap/", "echo/runtime/", "compatcheck/")) and
                    not name.endswith((".jasm", ".dll", ".exe", ".so")) and
                    "hybrid-build" not in name and "test-only" not in name, "Ineligible application member: " + name)
            require(not payload.startswith(b"\xca\xfe\xba\xbe") or name.endswith((".class", ".dylib")),
                    "Class bytes disguised as resource: " + name)
        classes = {n for n in names if n.endswith(".class")}
        require(classes and set(ledger["class_sources"]) == classes, "Class/source inventory differs")
        for name, source in ledger["class_sources"].items():
            require(source in sources and source.endswith(".java"), "Class has no ordinary Java source: " + name)
            require(archive.read(name).startswith(b"\xca\xfe\xba\xbe"), "Invalid class file: " + name)
        metadata = json.loads(archive.read("fabric.mod.json"))
        require(metadata.get("schemaVersion") == 1 and metadata.get("id") == "echoloader" and
                metadata.get("environment") == "client", "Wrong mod identity")
        require(metadata.get("depends") == {"minecraft": "26.2", "fabricloader": ">=0.20.1",
                "fabric-api": ">=0.161.0+26.2", "java": ">=25"}, "Pinned Fabric/Minecraft/Java dependencies differ")
        require(metadata.get("entrypoints") == ENTRYPOINTS, "Clean entrypoints differ")
        for entries in ENTRYPOINTS.values():
            require(entries[0].replace(".", "/") + ".class" in classes, "Entrypoint class absent")
        require(metadata.get("mixins") == CONFIGS, "Actual 67+4 Mixin configurations required")
        config_check(archive, names, CONFIGS[0], "dev.echo.client.mixin", ORIGINAL_MIXINS[:2],
                     ORIGINAL_MIXINS[2:], "dev.echo.client.bootstrap.EchoMixinPlugin")
        config_check(archive, names, CONFIGS[1], "dev.echo.compat.lwjgl.mixin", [], COMPAT_MIXINS)
        require(metadata.get("accessWidener") == "echo.accesswidener", "Access widener absent")
        header = archive.read("echo.accesswidener").decode().splitlines()[0].split()
        require(header in (["accessWidener", "v2", "official"], ["accessWidener", "v1", "official"]),
                "Application widener is not in the official 26.x namespace")
        nested = {n for n in names if n.endswith(".jar")}
        require(nested == set(ledger["nested_dependencies"]) == {j["file"] for j in metadata.get("jars", [])},
                "Nested dependency registration/provenance differs")
        dependency_markers = set()
        for name, record in ledger["nested_dependencies"].items():
            require(record.get("kind") == "source-built-third-party" and record.get("source_files") and
                    all(p in sources for p in record["source_files"]), "Nested dependency source provenance absent")
            evidence(root, record["build_evidence"])
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as dependency:
                dep_names = members(dependency)
                require(not any(n.startswith(("dev/echo/", "hack/", "dev/dov/", "echo/bootstrap/")) for n in dep_names),
                        "First-party code hidden in dependency")
                dependency_markers.update(dep_names & {"org/mozilla/javascript/Context.class", "com/github/weisj/jsvg/SVGDocument.class"})
        require(len(dependency_markers) == 2, "Actual Rhino and JSVG dependency JARs required")
        require({n for n in names if n.startswith("native/")} == set(NATIVES.values()), "Exact macOS native set required")
        native_result = []
        for crate, resource in NATIVES.items():
            receipt_name = "META-INF/echo-native/" + crate + ".json"
            receipt = json.loads(archive.read(receipt_name))
            data = archive.read(resource)
            require(receipt.get("target") == "macos-aarch64" and receipt.get("crate") == crate and
                    receipt["output"]["resource"] == resource and receipt["output"]["sha256"] == sha(data),
                    "Native receipt/resource/hash mismatch")
            require(0x0100000C in macho_cpus(data), "Native is not macOS ARM64")
            require(receipt.get("source_files"), "Native source hashes absent")
            for relative, digest in receipt["source_files"].items():
                source = str(PurePosixPath(receipt.get("source_directory", "")) / relative)
                require(sources.get(source) == digest, "Native source not bound to candidate: " + source)
            native_result.append({"resource": resource, "sha256": sha(data), "receipt_sha256": sha(archive.read(receipt_name))})
    # Recheck container bytes after inspection to reject concurrent publication.
    require(file_sha(jar_path) == expected_jar, "Client changed during validation")
    return {"path": str(Path(jar_path).resolve()), "sha256": expected_jar,
            "provenance_path": str(Path(provenance_path).resolve()), "provenance_sha256": expected_provenance,
            "source_set_sha256": ledger["source_set_sha256"], "source_files": len(sources),
            "classes": len(classes), "archive_entries": len(names), "original_mixins": len(ORIGINAL_MIXINS),
            "compatibility_mixins": len(COMPAT_MIXINS), "native_resources": native_result,
            "entrypoints": metadata["entrypoints"], "candidate_only": True}
