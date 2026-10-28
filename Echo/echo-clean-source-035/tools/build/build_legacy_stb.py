#!/usr/bin/env python3
"""Build the pinned readable Rust + genuine third-party STB1 compatibility native."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import struct
import subprocess
import time
from windows_gnu_toolchain import prepare as prepare_windows_gnu
from pe_native import inspect_bytes as inspect_pe


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def check_binary(data, target):
    if data[:4] == b"\xcf\xfa\xed\xfe" and struct.unpack_from("<I", data, 12)[0] == 6:
        kind = "macos"
        arch = {0x100000c: "aarch64", 0x1000007: "x86_64"}.get(struct.unpack_from("<I", data, 4)[0])
    elif data[:4] == b"\x7fELF" and struct.unpack_from("<H", data, 16)[0] == 3:
        kind = "linux"
        arch = {183: "aarch64", 62: "x86_64"}.get(struct.unpack_from("<H", data, 18)[0])
    elif data[:2] == b"MZ":
        offset = struct.unpack_from("<I", data, 0x3c)[0]
        if data[offset:offset + 4] != b"PE\0\0" or not struct.unpack_from("<H", data, offset + 22)[0] & 0x2000:
            raise SystemExit("Not a linked PE DLL")
        kind = "windows"
        arch = {0xaa64: "aarch64", 0x8664: "x86_64"}.get(struct.unpack_from("<H", data, offset + 4)[0])
    else:
        raise SystemExit("Not a supported linked shared library")
    if f"{kind}-{arch}" != target:
        raise SystemExit(f"Native target mismatch: {kind}-{arch}, expected {target}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--pins", type=Path, required=True)
    parser.add_argument("--targets", type=Path, required=True)
    parser.add_argument("--target", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--toolchain-pins", type=Path)
    args = parser.parse_args()
    if os.environ.get("ECHO_HEAVY_TOOL_LOCK_HELD") != "1":
        raise SystemExit("Run through ROOT/tools/with_heavy_tool.py")
    pins = json.loads(args.pins.read_text())
    pins["rust_toolchain"] = os.environ.get("ECHO_RUST_TOOLCHAIN", pins["rust_toolchain"])
    target = json.loads(args.targets.read_text())[args.target]
    source = args.root
    actual = {str(p.relative_to(source)) for directory in ("native/lwjgl-compat", "third-party/stb-legacy")
              for p in (source / directory).rglob("*") if p.is_file()}
    if actual != set(pins["files"]):
        raise SystemExit("STB compatibility source inventory drift")
    snapshot = args.output / "inputs"
    snapshot.mkdir(parents=True, exist_ok=True)
    existing = {str(p.relative_to(snapshot)) for p in snapshot.rglob("*") if p.is_file()}
    if existing - actual:
        raise SystemExit("Unexpected existing snapshot inputs; use a fresh output directory")
    for name, expected in pins["files"].items():
        original = source / name
        if sha(original) != expected:
            raise SystemExit(f"STB compatibility source hash drift: {name}")
        copied = snapshot / name
        copied.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, copied)
        if sha(copied) != expected:
            raise SystemExit(f"Snapshot changed while copying: {name}")
    rustc = subprocess.check_output(["rustc", "+" + pins["rust_toolchain"], "-Vv"], text=True)
    if f"release: {pins['rust_release']}\n" not in rustc or f"commit-hash: {pins['rust_commit']}\n" not in rustc:
        raise SystemExit("Pinned Rust compiler changed")
    environment = dict(os.environ)
    # Compiler/linker paths for cross builds may be configured. Semantic flags
    # cannot silently override the reviewed C and Rust build configuration.
    for key in list(environment):
        if key.startswith(("CFLAGS", "CXXFLAGS", "HOST_CFLAGS", "TARGET_CFLAGS", "CARGO_PROFILE_")) or key in ("CARGO_ENCODED_RUSTFLAGS", "RUSTFLAGS"):
            del environment[key]
    environment.update(CARGO_INCREMENTAL="0", CARGO_BUILD_JOBS="1", RAYON_NUM_THREADS="2", CC_ENABLE_DEBUG_OUTPUT="1")
    environment.pop("RUSTC_WRAPPER", None)
    environment.pop("RUSTC_WORKSPACE_WRAPPER", None)
    environment["RUSTFLAGS"] = f"--remap-path-prefix={snapshot}=/echo-native/lwjgl-compat --remap-path-prefix={Path.home() / '.cargo'}=/cargo"
    toolchain = None
    if target["rust"] == "x86_64-pc-windows-gnu":
        if args.toolchain_pins is None:
            raise SystemExit('Windows GNU source build requires the pinned toolchain contract')
        flags, extra_environment, toolchain = prepare_windows_gnu(args.toolchain_pins, args.output / "toolchains/windows-gnu")
        flags = [f"--remap-path-prefix={snapshot}=/echo-native/lwjgl-compat", f"--remap-path-prefix={Path.home() / '.cargo'}=/cargo", *flags]
        environment.pop("RUSTFLAGS", None)
        environment["CARGO_ENCODED_RUSTFLAGS"] = "\x1f".join(flags)
        environment.update(extra_environment, SOURCE_DATE_EPOCH="0")
        environment["CC_x86_64_pc_windows_gnu"] = extra_environment["CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER"]
        archiver = shutil.which('x86_64-w64-mingw32-ar')
        if not archiver:
            raise SystemExit('Windows GNU C dependency requires the real target archiver')
        environment["AR_x86_64_pc_windows_gnu"] = archiver
    cargo_output = args.output / "cargo"
    command = ["cargo", "+" + pins["rust_toolchain"], "build", "--locked", "--release", "--lib", "--jobs", "1",
               "--manifest-path", str(snapshot / "native/lwjgl-compat/Cargo.toml"), "--target-dir", str(cargo_output), "--target", target["rust"], "-vv"]
    log = args.output / "cargo.log"
    peak = 0
    limit = 512 * 1024
    with log.open("w") as output:
        process = subprocess.Popen(command, env=environment, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        while process.poll() is None:
            rows = subprocess.check_output(["ps", "-axo", "pgid=,rss="], text=True)
            rss = sum(int(fields[1]) for line in rows.splitlines() if len(fields := line.split()) == 2 and int(fields[0]) == process.pid)
            peak = max(peak, rss)
            if rss > limit:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                break
            time.sleep(0.05)
        code = process.wait()
    measured = {"aggregate_peak_rss_kib": peak, "rss_limit_kib": limit, "sample_interval_ms": 50,
                "rss_limit_exceeded": peak > limit, "exit_code": code, "cargo_jobs": 1}
    write(args.output / "resources-measured.json", measured)
    if code or peak > limit:
        raise SystemExit(f"Cargo build failed; see {log}")
    filename = target["prefix"] + "echo_stb_legacy" + target["suffix"]
    library = cargo_output / target["rust"] / "release" / filename
    check_binary(library.read_bytes(), args.target)
    resource = f"native/{args.target}/{filename}"
    destination = args.output / "resources" / resource
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(library, destination)
    packaged = {"crate": "lwjgl-compat", "target": args.target, "rust_target": target["rust"],
                "rust_release": pins["rust_release"], "rust_commit": pins["rust_commit"],
                "source_files": pins["files"], "output": {"resource": resource, "sha256": sha(destination)},
                 "algorithm": "official stb_image_resize v0.97, STB1 linear uint8 independent channels"}
    if args.target == "windows-x86_64":
        pe = inspect_pe(library.read_bytes(), str(library), {"Java_dev_echo_compat_lwjgl_LegacyImageResize_resizeUint8"}, False)
        if pe['non_system_dependencies']:
            raise SystemExit('Unprovisioned legacy STB DLL dependencies: ' + str(pe['non_system_dependencies']))
        write(args.output / 'pe.json', pe)
        packaged['runtime'] = {'import_dlls': sorted({v['dll'].lower() for v in pe['imports']}),
                              'non_system_dependencies': [], 'delay_imports_present': False, 'minimum_os': 'Windows 10',
                              'live_load_verified': False}
    write(args.output / "resources/META-INF/echo-native/lwjgl-compat.json", packaged)
    for name in ("LICENSE", "PROVENANCE.md"):
        resource_name = "LICENSE.txt" if name == "LICENSE" else name
        dst = args.output / "resources/META-INF/licenses/echo-native/lwjgl-compat" / resource_name
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(snapshot / "third-party/stb-legacy" / name, dst)
    write(args.output / "build-receipt.json", {**packaged, "command": command, "rustc": rustc, "toolchain": toolchain,
          "measurement": measured, "log_sha256": sha(log), "c_compiler_environment":
          {key: value for key, value in environment.items() if key.startswith(("CC", "AR", "CARGO_TARGET_"))}})
    print(json.dumps({"output": packaged["output"], "measurement": measured}, indent=2))


if __name__ == "__main__":
    main()
