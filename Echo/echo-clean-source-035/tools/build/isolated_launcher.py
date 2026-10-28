#!/usr/bin/env python3
"""Flatten official Minecraft/Fabric metadata with the exact resolved LWJGL runtime.

Outputs only an isolated directory. No accounts, existing profiles or game state are
read. 'provision' downloads/verifies public launcher libraries; it never starts Java.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request


def sha(path, algorithm="sha256"):
    return hashlib.new(algorithm, Path(path).read_bytes()).hexdigest()


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def maven_path(coordinate):
    group, artifact, version, *classifier = coordinate.split(":")
    filename = artifact + "-" + version + ("-" + classifier[0] if classifier else "") + ".jar"
    return "/".join([group.replace(".", "/"), artifact, version, filename])


def allowed(library, target):
    # Target-specific output: eliminate inherited launcher matching ambiguity.
    result = not library.get("rules")
    target_os, arch = target.split("-", 1)
    target_os = {"macos": "osx"}.get(target_os, target_os)
    aliases = {"aarch64": {"aarch64", "arm64"}, "x86_64": {"x86_64", "amd64"}}[arch]
    for rule in library.get("rules", []):
        if "features" in rule or "version" in rule.get("os", {}):
            raise ValueError("Unsupported library rule requires explicit review: " + str(rule))
        os_rule = rule.get("os", {})
        matches = ("name" not in os_rule or os_rule["name"] == target_os) and ("arch" not in os_rule or os_rule["arch"] in aliases)
        if matches:
            result = rule["action"] == "allow"
    return result


def prepare(args):
    missing = [name for name in ("pins.json", "minecraft.json", "fabric.json")
               if not (args.inputs / name).is_file()]
    if missing:
        raise SystemExit("Launcher metadata is unresolved ({} missing). Run "
                         "`python3 tools/build/launcher_inputs.py` with network access to "
                         "piston-meta.mojang.com and meta.fabricmc.net first.".format(", ".join(missing)))
    pins = json.loads((args.inputs / "pins.json").read_text())
    if pins.get("minecraft") != "26.2" or pins.get("obfuscated"):
        raise SystemExit("Launcher metadata pins are not for the unobfuscated 26.2 release")
    for name, expected in pins["files"].items():
        if sha(args.inputs / name) != expected:
            raise SystemExit("Launcher input hash mismatch: " + name)
    minecraft = json.loads((args.inputs / "minecraft.json").read_text())
    fabric = json.loads((args.inputs / "fabric.json").read_text())
    runtime = json.loads(args.runtime.read_text())
    native_records = []
    for path in sorted((args.native_resources / "META-INF/echo-native").glob("*.json")):
        record = json.loads(path.read_text())
        resource = record["output"]["resource"]
        relative = Path(resource)
        if relative.is_absolute() or ".." in relative.parts or not resource.startswith("native/" + runtime["target"] + "/"):
            raise SystemExit("Native receipt resource is outside the selected target")
        if record["target"] != runtime["target"] or sha(args.native_resources / relative) != record["output"]["sha256"]:
            raise SystemExit("Native launcher resource/receipt mismatch")
        native_records.append({"crate": record["crate"], "resource": resource, "sha256": record["output"]["sha256"],
                               "runtime": record.get("runtime"), "receipt_sha256": sha(path)})
    expected_native = {"renderer", "media", "lwjgl-compat"} if runtime["target"] == "windows-x86_64" else {"renderer", "lwjgl-compat"}
    if {r["crate"] for r in native_records} != expected_native:
        raise SystemExit("Launcher descriptor requires the exact source-built native component set")
    compatibility = json.loads(args.compatibility_receipt.read_text())
    if compatibility.get("status") != "passed" or compatibility.get("transformed_failures") != 0 or compatibility.get("original_failures") != 2 or compatibility.get("redirects") != 5:
        raise SystemExit("A real Mixin compatibility gate receipt is required")
    if minecraft["id"] != "26.2" or fabric["inheritsFrom"] != minecraft["id"]:
        raise SystemExit("Minecraft/Fabric input version mismatch")
    version = copy.deepcopy(minecraft)
    loader_version = pins.get("fabric_loader") or fabric["id"].split("-")[2]
    version["id"] = (f"echo-clean-{minecraft['id']}-fabric-{loader_version}"
                     f"-lwjgl-{runtime['lwjgl']}-{runtime['target']}")
    version["mainClass"] = fabric["mainClass"]
    # A complete descriptor, with no inheritsFrom: launchers must not merge the
    # vanilla 3.3.3 entries back in. Existing game and JVM arguments are retained.
    version.pop("inheritsFrom", None)
    for kind, arguments in fabric.get("arguments", {}).items():
        version.setdefault("arguments", {}).setdefault(kind, []).extend(arguments)
    selected = {}
    for library in minecraft["libraries"] + fabric["libraries"]:
        if library["name"].startswith("org.lwjgl:") or not allowed(library, runtime["target"]):
            continue
        library = copy.deepcopy(library)
        library.pop("rules", None)
        if "natives" in library:
            raise SystemExit("Unexpected legacy extraction library requires review: " + library["name"])
        if "downloads" not in library:
            path = maven_path(library["name"])
            library["downloads"] = {"artifact": {"path": path, "url": library.get("url", "https://libraries.minecraft.net/") + path}}
            for field in ("sha1", "sha256", "size"):
                if field in library:
                    library["downloads"]["artifact"][field] = library[field]
        # Fabric overrides the same group/artifact, preserving ordinary launcher behavior.
        key = ":".join(library["name"].split(":")[:2] + library["name"].split(":")[3:])
        selected[key] = library
    for artifact in runtime["artifacts"]:
        if sha(artifact["file"]) != artifact["sha256"]:
            raise SystemExit("Resolved runtime changed: " + artifact["file"])
        library = {"name": artifact["coordinate"], "downloads": {"artifact": {k: artifact[k] for k in ["path", "url", "sha1", "size"]}}}
        selected[artifact["coordinate"]] = library
        destination = args.output / "libraries" / artifact["path"]
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(artifact["file"], destination)
    version["libraries"] = list(selected.values())
    mods = []
    for mod in runtime["mods"]:
        if sha(mod["file"]) != mod["sha256"]:
            raise SystemExit("Resolved public launcher mod changed")
        destination = args.output / "instance" / "mods" / Path(mod["file"]).name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(mod["file"], destination)
        mods.append({"coordinate": mod["coordinate"], "path": str(destination.relative_to(args.output)), "sha256": sha(destination)})
    version_path = args.output / "versions" / version["id"] / (version["id"] + ".json")
    save(version_path, version)
    save(args.output / "launcher-contract.json", {
        "version": version["id"], "target": runtime["target"], "java": 25,
        "mainClass": version["mainClass"], "minecraft": minecraft["id"], "fabric": loader_version,
        "descriptor": str(version_path.relative_to(args.output)), "descriptor_sha256": sha(version_path),
        "runtime_input_sha256": sha(args.runtime), "metadata_pins": pins,
        "public_mods": mods, "isolated_game_directory": "instance",
        "default_backend": "OpenGL/GLFW", "lwjgl_version": runtime["lwjgl"],
        "source_built_native_resources": native_records,
        "optional_backends": {
            "SDL": "Bindings/native library supplied; backend selection remains the source-owned WindowSystem contract.",
            "Vulkan": "Bindings and VMA/shaderc supplied. Linux/Windows require a working Vulkan driver/loader. macOS receives the LWJGL MoltenVK native artifact; renderer/device/surface integration remains separately verified.",
            "MSL": "Original native emitMsl is unavailable; retained Java API throws its documented linkage failure.",
            "media": "Windows x86-64 SMTC only; other platforms retain MediaSession.AVAILABLE=false."
        },
        "release_gate": "Use only a separately completed/admitted clean jar; descriptor generation is not source completion or game launch evidence.",
        "minecraft_linkage_status": "Scoped real compatibility Mixins verified; complete production Mixin interactions and game execution remain unverified.",
        "minecraft_linkage_verification": {"sha256": sha(args.compatibility_receipt), "result": compatibility},
        "launch_policy": "Use this complete descriptor in a NEW isolated installation. Do not layer it over a vanilla/Fabric inherited profile, whose LWJGL 3.3.3 entries would return. No existing profile is modified."
    })
    print(json.dumps({"descriptor": str(version_path), "libraries": len(version["libraries"]), "lwjgl_artifacts": len(runtime["artifacts"])}))


def provision(args):
    contract = json.loads((args.output / "launcher-contract.json").read_text())
    descriptor = args.output / contract["descriptor"]
    if sha(descriptor) != contract["descriptor_sha256"]:
        raise SystemExit("Launcher descriptor changed")
    version = json.loads(descriptor.read_text())
    artifacts = []
    for library in version["libraries"]:
        artifact = library["downloads"]["artifact"]
        path = args.output / "libraries" / artifact["path"]
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists():
            request = urllib.request.Request(artifact["url"], headers={"User-Agent": "EchoCleanSourceBuild/1"})
            with urllib.request.urlopen(request, timeout=120) as response:
                payload = response.read()
            temporary = path.with_suffix(".download")
            temporary.write_bytes(payload)
            temporary.replace(path)
        if artifact.get("sha1") and sha(path, "sha1") != artifact["sha1"]:
            raise SystemExit("Launcher SHA-1 mismatch: " + str(path))
        if artifact.get("sha256") and sha(path) != artifact["sha256"]:
            raise SystemExit("Launcher SHA-256 mismatch: " + str(path))
        if artifact.get("size") and path.stat().st_size != artifact["size"]:
            raise SystemExit("Launcher size mismatch: " + str(path))
        artifacts.append({"coordinate": library["name"], "file": str(path.resolve()), "sha256": sha(path),
                          "sha1": sha(path, "sha1"), "url": artifact["url"],
                          "publisher_hash_available": bool(artifact.get("sha1"))})
    save(args.output / "provision-receipt.json", {"descriptor_sha256": sha(descriptor), "artifacts": artifacts,
                                                "game_launched": False, "application_jar": None})
    print(json.dumps({"provisioned_public_libraries": len(artifacts), "game_launched": False}))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    prepare_parser = commands.add_parser("prepare")
    prepare_parser.add_argument("--runtime", type=Path, required=True)
    prepare_parser.add_argument("--inputs", type=Path, required=True)
    prepare_parser.add_argument("--native-resources", type=Path, required=True)
    prepare_parser.add_argument("--compatibility-receipt", type=Path, required=True)
    prepare_parser.set_defaults(action=prepare)
    provision_parser = commands.add_parser("provision")
    provision_parser.set_defaults(action=provision)
    for sub in (prepare_parser, provision_parser):
        sub.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.action(args)


if __name__ == "__main__":
    main()
