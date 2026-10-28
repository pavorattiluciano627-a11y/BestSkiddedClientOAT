#!/usr/bin/env python3
"""Stage the pinned public Rust GNU std component in a build-owned sysroot."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import urllib.request


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def prepare(pins_file, output):
    pins = json.loads(pins_file.read_text())
    output.mkdir(parents=True, exist_ok=True)
    archive = output / (pins["component_sha256"] + ".tar.xz")
    if not archive.exists():
        # Optional cache is public compiler data only, always checksum verified.
        cache = os.environ.get("ECHO_RUST_STD_ARCHIVE")
        if cache:
            if sha(cache) != pins["component_sha256"]:
                raise ValueError("Rust standard-library archive checksum mismatch")
            shutil.copyfile(cache, archive)
        else:
            temporary = archive.with_suffix(".download")
            with urllib.request.urlopen(pins["component_url"], timeout=120) as response, temporary.open("wb") as stream:
                shutil.copyfileobj(response, stream)
            if sha(temporary) != pins["component_sha256"]:
                raise ValueError("Rust standard-library publisher checksum mismatch")
            temporary.replace(archive)
    if sha(archive) != pins["component_sha256"]:
        raise ValueError("Pinned Rust standard-library archive changed")
    sysroot = output / "sysroot"
    marker = "/lib/rustlib/" + pins["target"] + "/"
    files = []
    with tarfile.open(archive, "r:xz") as package:
        for member in package:
            start = member.name.find(marker)
            if start < 0 or not member.isfile():
                continue
            relative = Path(member.name[start + 1:])
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Unsafe Rust component entry")
            target = sysroot / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            data = package.extractfile(member).read()
            if target.exists() and target.read_bytes() != data:
                raise ValueError("Staged standard-library input changed")
            if not target.exists():
                target.write_bytes(data)
            files.append({"path": str(relative), "sha256": sha(target)})
    if not files or not list((sysroot / "lib/rustlib" / pins["target"] / "lib").glob("libstd-*.rlib")):
        raise ValueError("Rust component contains no target standard library")
    linker = shutil.which(pins["linker"])
    dlltool = shutil.which(pins["dlltool"])
    if not linker or not dlltool:
        raise ValueError("A real x86_64 MinGW linker and dlltool are required")
    tool_files = [{"tool": role, "path": str(Path(path).resolve()), "sha256": sha(path)}
                  for role, path in (("linker", linker), ("dlltool", dlltool))]
    imports = []
    for name in ("dllcrt2.o", "crtbegin.o", "crtend.o", "libgcc.a", "libgcc_eh.a", "libmingw32.a", "libmingwex.a", "libmsvcrt.a", "libkernel32.a", "libuserenv.a", "libws2_32.a"):
        path = Path(subprocess.check_output([linker, "-print-file-name=" + name], text=True).strip())
        if path.is_file():
            imports.append({"name": name, "path": str(path.resolve()), "sha256": sha(path)})
    receipt = {"target": pins["target"], "component_url": pins["component_url"], "component_sha256": sha(archive),
               "files": files, "tools": tool_files, "linker_inputs": imports,
               "linker_version": subprocess.check_output([linker, "--version"], text=True),
               "global_toolchain_modified": False}
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    wrapper = Path(__file__).with_name("deterministic_dlltool.py")
    # File-copy intake tools may not preserve executable bits; materialize the
    # reviewed helper into this build's toolchain directory with explicit mode.
    executable = output / "deterministic-dlltool"
    shutil.copyfile(wrapper, executable)
    executable.chmod(0o755)
    receipt["deterministic_dlltool"] = {"source": str(wrapper), "sha256": sha(wrapper), "executable": str(executable)}
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    flags = ["--sysroot", str(sysroot), "-C", "dlltool=" + str(executable), "-C", "link-arg=-Wl,--no-insert-timestamp"]
    return flags, {"CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER": linker, "ECHO_REAL_DLLTOOL": dlltool,
                   "ECHO_DLLTOOL_RECEIPTS": str(output / "dlltool-receipts")}, receipt
