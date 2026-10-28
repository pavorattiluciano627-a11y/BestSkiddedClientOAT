#!/usr/bin/env python3
"""Build reviewed, pinned Rust inputs into the exact resources used by Java loaders."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import struct
import subprocess
import sys
import time
import zipfile
from windows_gnu_toolchain import prepare as prepare_windows_gnu
from pe_native import inspect_bytes as inspect_pe

NAGA_EXPORTS = {"Java_dev_echo_render_shader_naga_Naga_" + name for name in
                ("moduleFree", "moduleInfoFree", "parseWgsl", "validate", "emitGlsl", "emitSpv",
                 "numGlobalVariables", "typeJson", "globalVariableJson")}


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def bounded_cargo(command, environment, log, rss_limit_mib):
    """Enforce the authorized cold Cargo budget; Java tasks keep their own heap caps."""
    peak = 0
    exceeded = False
    peak_cpu = 0.0
    with log.open("w") as stream:
        process = subprocess.Popen(command, env=environment, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        tracked = {process.pid}
        while process.poll() is None:
            raw = subprocess.check_output(["ps", "-axo", "pid=,ppid=,pgid=,rss=,pcpu="], text=True, env=dict(os.environ, LC_ALL="C"))
            rows = [line.split() for line in raw.splitlines() if len(line.split()) == 5]
            while True:
                found = {int(r[0]) for r in rows if int(r[0]) in tracked or int(r[1]) in tracked or int(r[2]) == process.pid}
                if found <= tracked:
                    break
                tracked |= found
            children = [r for r in rows if int(r[0]) in tracked]
            rss = sum(int(r[3]) for r in children)
            cpu = sum(float(r[4]) for r in children)
            peak_cpu = max(peak_cpu, cpu)
            peak = max(peak, rss)
            if rss > rss_limit_mib * 1024 or cpu > 200:
                exceeded = True
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                break
            time.sleep(0.1)
        code = process.wait()
    measurement = {"aggregate_peak_rss_kib": peak, "rss_limit_kib": rss_limit_mib * 1024,
                   "rss_limit_exceeded": peak > rss_limit_mib * 1024, "cpu_limit_exceeded": peak_cpu > 200,
                   "sampled_peak_cpu_percent": round(peak_cpu, 2), "cargo_jobs": 1, "cpu_budget": 2,
                   "sampling": "POSIX Cargo process-tree RSS/CPU, 100 ms", "exit_code": code}
    write_json(log.with_suffix(".resources.json"), measurement)
    return code, measurement


def binary_architecture(data):
    if data[:4] == b"\xcf\xfa\xed\xfe":
        if struct.unpack_from("<I", data, 12)[0] != 6:
            raise ValueError("Mach-O output is not a linked dynamic library")
        return "Mach-O", {0x100000c: "aarch64", 0x1000007: "x86_64"}.get(struct.unpack_from("<I", data, 4)[0])
    if data[:4] == b"\x7fELF":
        if struct.unpack_from("<H", data, 16)[0] != 3:
            raise ValueError("ELF output is not a shared library")
        return "ELF", {183: "aarch64", 62: "x86_64"}.get(struct.unpack_from("<H", data, 18)[0])
    if data[:2] == b"MZ":
        offset = struct.unpack_from("<I", data, 0x3c)[0]
        if data[offset:offset + 4] == b"PE\0\0":
            if not struct.unpack_from("<H", data, offset + 22)[0] & 0x2000:
                raise ValueError("PE output is not a DLL")
            return "PE", {0xaa64: "aarch64", 0x8664: "x86_64"}.get(struct.unpack_from("<H", data, offset + 4)[0])
    raise ValueError("Not a linked supported native library")


def check_binary(data, target):
    kind, arch = binary_architecture(data)
    expected = "Mach-O" if target.startswith("macos-") else "PE" if target.startswith("windows-") else "ELF"
    if kind != expected or arch != target.split("-", 1)[1]:
        raise ValueError(f"Wrong native architecture: {kind}/{arch}, expected {target}")
    return {"format": kind, "architecture": arch}


def build(args):
    if os.environ.get("ECHO_HEAVY_TOOL_LOCK_HELD") != "1":
        raise SystemExit("Run through ROOT/tools/with_heavy_tool.py")
    pins = json.loads(args.pins.read_text())
    pins["rust_toolchain"] = os.environ.get("ECHO_RUST_TOOLCHAIN", pins["rust_toolchain"])
    rustc = subprocess.check_output(["rustc", "+" + pins["rust_toolchain"], "-Vv"], text=True)
    if f"release: {pins['rust_release']}\n" not in rustc or f"commit-hash: {pins['rust_commit']}\n" not in rustc:
        raise SystemExit("Rust toolchain drift: install/select the reviewed compiler from native-inputs.json")
    targets = json.loads(args.targets.read_text())
    target = targets[args.target]
    if args.crate == "media" and not target["media"]:
        raise SystemExit("Media is implemented only for Windows x86-64; no unavailable-platform stub may be packaged")
    spec = pins["crates"][args.crate]
    if "owner_contract" in spec:
        owner = args.root / spec["owner_contract"]["path"]
        if digest(owner) != spec["owner_contract"]["sha256"]:
            raise SystemExit("Native owner contract changed; reviewed intake required")
        if json.loads(owner.read_text())["source_hashes"] != spec["files"]:
            raise SystemExit("Native source pins differ from the owner contract")
    source = args.root / spec["directory"]
    expected_files = set(spec["files"])
    actual_files = {str(p.relative_to(source)) for p in (source / "src").rglob("*.rs")}
    actual_files |= {p for p in ("Cargo.toml", "Cargo.lock", "build.rs") if (source / p).is_file()}
    if actual_files != expected_files:
        raise SystemExit("Native input set changed; review and renew native-inputs.json")
    snapshot = args.output / "inputs" / args.crate
    for name, sha in spec["files"].items():
        original = source / name
        if digest(original) != sha:
            raise SystemExit(f"Native source hash changed: {original}")
        copied = snapshot / name
        copied.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, copied)
        if digest(copied) != sha:
            raise SystemExit(f"Native input changed during copy: {original}")
    # No owner target directory is read or written. Cargo --locked forbids lock updates.
    cargo_output = args.output / "cargo" / args.crate
    command = ["cargo", "+" + pins["rust_toolchain"], "build", "--locked", "--release", "--lib", "--message-format=json-render-diagnostics",
               "--manifest-path", str(snapshot / "Cargo.toml"), "--target-dir", str(cargo_output),
               "--target", target["rust"], "--jobs", "1"]
    environment = dict(os.environ, CARGO_INCREMENTAL="0", CARGO_BUILD_JOBS="1", RAYON_NUM_THREADS="2")
    for name in ("RUSTC_WRAPPER", "RUSTC_WORKSPACE_WRAPPER", "RUSTC_BOOTSTRAP", "CARGO_ENCODED_RUSTFLAGS"):
        environment.pop(name, None)
    environment.update({key: value for key, value in pins["build_profile"].items() if key.startswith("CARGO_")})
    # Avoid checkout paths in Rust debug/panic locations. Linker/toolchain/SDK remain receipted.
    environment.pop("CARGO_ENCODED_RUSTFLAGS", None)
    environment["RUSTFLAGS"] = f"--remap-path-prefix={snapshot}=/echo-native/{args.crate} " + f"--remap-path-prefix={Path.home() / '.cargo'}=/cargo"
    toolchain_receipt = None
    if target["rust"] == "x86_64-pc-windows-gnu":
        flags, extra_environment, toolchain_receipt = prepare_windows_gnu(args.pins.parent / "windows-gnu-toolchain.json", args.output / "toolchains/windows-gnu")
        flags = [f"--remap-path-prefix={snapshot}=/echo-native/{args.crate}", f"--remap-path-prefix={Path.home() / '.cargo'}=/cargo", *flags]
        environment.pop("RUSTFLAGS", None)
        environment["CARGO_ENCODED_RUSTFLAGS"] = "\x1f".join(flags)
        environment.update(extra_environment, SOURCE_DATE_EPOCH="0")
    args.output.mkdir(parents=True, exist_ok=True)
    log = args.output / (args.crate + "-cargo.log")
    code, measurement = bounded_cargo(command, environment, log, args.rss_limit_mib)
    cargo_artifacts = []
    for line in log.read_text().splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            print(line)
            continue
        if event.get("reason") == "compiler-artifact":
            cargo_artifacts.append({"package_id": event["package_id"], "target": event["target"]["name"],
                "kind": event["target"]["kind"], "fresh": event["fresh"], "features": event["features"],
                "files": [{"path": name, "sha256": digest(name)} for name in event["filenames"] if Path(name).is_file()]})
        elif event.get("reason") == "compiler-message":
            print(event["message"].get("rendered", event["message"]["message"]))
    if code:
        raise SystemExit(code if code > 0 else 128 - code)
    filename = target["prefix"] + spec["library"] + target["suffix"]
    artifact = cargo_output / target["rust"] / "release" / filename
    architecture = check_binary(artifact.read_bytes(), args.target)
    pe_receipt = None
    if args.target == "windows-x86_64":
        expected_exports = set(spec["jni_exports"]) if args.crate == "media" else NAGA_EXPORTS
        pe_receipt = inspect_pe(artifact.read_bytes(), str(artifact), expected_exports, args.crate == "media")
        if pe_receipt["non_system_dependencies"]:
            raise SystemExit("Native DLL requires unprovisioned non-system dependencies: " + str(pe_receipt["non_system_dependencies"]))
        write_json(args.output / (args.crate + "-pe.json"), pe_receipt)
    resource = f"native/{args.target}/{filename}"
    destination = args.output / "resources" / resource
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, destination)
    license_receipts = {}
    for name, expected_sha in spec.get("licenses", {}).items():
        license_source = source / name
        if digest(license_source) != expected_sha:
            raise SystemExit("Native license input changed: " + str(license_source))
        license_resource = f"META-INF/licenses/echo-native/{args.crate}/{Path(name).name}.txt"
        license_output = args.output / "resources" / license_resource
        license_output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(license_source, license_output)
        license_receipts[license_resource] = digest(license_output)
    receipt = {"crate": args.crate, "target": args.target, "rust_target": target["rust"],
               "source_files": spec["files"], "source_directory": spec["directory"],
               "rustc": rustc, "command": command, "rustflags": environment.get("CARGO_ENCODED_RUSTFLAGS", environment.get("RUSTFLAGS")),
               "toolchain": toolchain_receipt, "pe": pe_receipt,
               "resources": measurement,
               "build_profile": pins["build_profile"],
               "licenses": license_receipts,
               "cargo_artifacts": cargo_artifacts,
               "output": {"resource": resource, "sha256": digest(destination), **architecture},
               "msl_export": False if args.crate == "renderer" else None,
               "scope": "linked source-built native artifact; whole-client admission remains independently gated"}
    # Archive metadata is independent of checkout/build paths. The external
    # receipt retains full invocation details for review and reproduction.
    packaged_receipt = {key: receipt[key] for key in ("crate", "target", "rust_target", "source_files", "source_directory", "output", "msl_export", "scope", "build_profile", "licenses")}
    packaged_receipt.update(rust_release=pins["rust_release"], rust_commit=pins["rust_commit"])
    if pe_receipt:
        packaged_receipt["runtime"] = {"import_dlls": sorted({v["dll"].lower() for v in pe_receipt["imports"]}),
            "non_system_dependencies": pe_receipt["non_system_dependencies"], "delay_imports_present": False,
            "minimum_os": "Windows 10 1809" if args.crate == "media" else "Windows 10",
            "architecture": "x86_64", "system_requirements": "Target Windows API-set schema, UCRT, and system DLLs; media additionally requires SMTC/WinRT.",
            "live_load_verified": False}
    write_json(args.output / "resources" / "META-INF" / "echo-native" / (args.crate + ".json"), packaged_receipt)
    write_json(args.output / (args.crate + "-receipt.json"), receipt)
    print(json.dumps(receipt["output"], sort_keys=True))


def verify_archive(args):
    targets = json.loads(args.targets.read_text())
    target = targets[args.target]
    expected = {"renderer": "naga_jni", "lwjgl-compat": "echo_stb_legacy"}
    if target["media"]:
        expected["media"] = "media_session"
    with zipfile.ZipFile(args.archive) as jar:
        found = set()
        for crate, library in expected.items():
            name = f"native/{args.target}/{target['prefix']}{library}{target['suffix']}"
            receipt = json.loads(jar.read(f"META-INF/echo-native/{crate}.json"))
            data = jar.read(name)
            check_binary(data, args.target)
            if receipt["target"] != args.target or receipt["output"]["resource"] != name:
                raise SystemExit("Native receipt target/resource mismatch")
            if hashlib.sha256(data).hexdigest() != receipt["output"]["sha256"]:
                raise SystemExit("Native archive hash mismatch")
            if args.target == "windows-x86_64":
                expected_exports = ({"Java_dev_echo_client_media_MediaSession_" + n for n in ("revision", "title", "artist", "cover", "positionMillis", "durationMillis")} if crate == "media" else
                                    {"Java_dev_echo_compat_lwjgl_LegacyImageResize_resizeUint8"} if crate == "lwjgl-compat" else NAGA_EXPORTS)
                pe = inspect_pe(data, name, expected_exports, crate == "media")
                if pe["non_system_dependencies"] or sorted({v["dll"].lower() for v in pe["imports"]}) != receipt["runtime"]["import_dlls"]:
                    raise SystemExit("Packaged PE dependencies differ from the verified runtime contract")
            found.add(name)
        packaged = {n for n in jar.namelist() if n.startswith("native/") and not n.endswith("/")}
        if packaged != found:
            raise SystemExit(f"Unexpected/missing native resources: {packaged ^ found}")
    print(json.dumps({"archive_sha256": digest(args.archive), "resources": sorted(found), "verified": True}))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    compiler = commands.add_parser("build")
    compiler.add_argument("--root", type=Path, required=True)
    compiler.add_argument("--pins", type=Path, required=True)
    compiler.add_argument("--output", type=Path, required=True)
    compiler.add_argument("--crate", choices=["renderer", "media"], required=True)
    compiler.add_argument("--rss-limit-mib", type=int, choices=[512,4096], default=512)
    compiler.set_defaults(action=build)
    archive = commands.add_parser("verify-archive")
    archive.add_argument("--archive", type=Path, required=True)
    archive.set_defaults(action=verify_archive)
    for sub in (compiler, archive):
        sub.add_argument("--target", required=True)
        sub.add_argument("--targets", type=Path, required=True)
    args = parser.parse_args()
    args.action(args)


if __name__ == "__main__":
    main()
