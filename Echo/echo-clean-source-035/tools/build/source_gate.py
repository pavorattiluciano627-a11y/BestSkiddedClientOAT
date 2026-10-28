#!/usr/bin/env python3
"""Fail-closed clean-source and archive checks. Never loads application classes.

These checks reject known reconstruction shortcuts. A passing static audit is
not itself behavioral parity: release additionally requires complete source-bound
coverage and independently produced passing verification receipts.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
import sys
import zipfile
from pathlib import Path


class GateError(ValueError):
    """A source or archive input violates the clean build contract."""


SOURCE_SUFFIXES = {".java", ".rs"}
TEXT_RESOURCES = {
    ".json", ".mcmeta", ".properties", ".txt", ".md", ".svg", ".glsl",
    ".vert", ".frag", ".geom", ".comp", ".fsh", ".vsh", ".wgsl",
    ".accesswidener", ".cfg", ".toml", ".css", ".html", ".csv",
}
MEDIA_RESOURCES = {".png", ".jpg", ".jpeg", ".ttf", ".otf", ".woff", ".woff2", ".ogg", ".wav"}
OPAQUE_SUFFIXES = {".class", ".jar", ".zip", ".jasm", ".jcod", ".asm", ".dll", ".so", ".dylib", ".exe", ".bin", ".dat", ".wasm", ".pyc", ".o", ".a", ".spv"}
BLOCKED_NAMES = (
    "hack/", "dev/dov/", "dev/jadyen/drm/", "echo/runtime/strings/StringTable",
    "echo/runtime/OfflineHelperDispatcher", "echo/runtime/HelperSupport",
    "echo/runtime/resources/RecordStore", "echo/runtime/resources/MirageStreamHandler",
    "echo/linkage/", "echo/indy/", "echo/rebuild/",
)
DISPATCH_TYPES = {"MutableCallSite", "VolatileCallSite", "ConstantCallSite"}
FORBIDDEN_METHODS = {
    "defineClass", "defineHiddenClass", "defineHiddenClassWithClassData",
    "invokeWithArguments", "decryptString", "decodeStringTable", "resolveEncryptedMember",
}
STANDARD_BOOTSTRAPS = {
    ("java/lang/invoke/LambdaMetafactory", "metafactory"),
    ("java/lang/invoke/LambdaMetafactory", "altMetafactory"),
    ("java/lang/invoke/StringConcatFactory", "makeConcat"),
    ("java/lang/invoke/StringConcatFactory", "makeConcatWithConstants"),
    ("java/lang/runtime/ObjectMethods", "bootstrap"),
    ("java/lang/runtime/SwitchBootstraps", "typeSwitch"),
    ("java/lang/runtime/SwitchBootstraps", "enumSwitch"),
}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def strip_comments(text: str, strip_strings: bool = False) -> str:
    """Retain literals while dropping comments, or blank both for code-only checks."""
    tokens = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
    return tokens.sub(lambda match: " " * len(match[0]) if match[0].startswith(("//", "/*")) or strip_strings else match[0], text)


def check_source(text: str, label: str) -> list[str]:
    """Check first-party source only; upstream Rhino is a legitimate JS interpreter."""
    errors = []
    # Java processes Unicode escapes before tokenization, including in comments.
    if label.endswith(".java"):
        text = re.sub(r"\\u+([0-9a-fA-F]{4})", lambda m: chr(int(m[1], 16)), text)
    code = strip_comments(text, strip_strings=True)
    literal_code = strip_comments(text)
    if re.search(r"\bpackage\s+(?:hack|dev\.dov)(?:\s*;|\.)", code):
        errors.append(f"{label}: retained obfuscated first-party package")
    for prefix in BLOCKED_NAMES:
        dotted = prefix.replace("/", ".")
        if dotted in code or prefix in code:
            errors.append(f"{label}: retained obfuscator/reference namespace {prefix}")
    for name in DISPATCH_TYPES | FORBIDDEN_METHODS:
        if re.search(r"\b" + name + r"\b", code):
            errors.append(f"{label}: hidden application dispatch/code loading: {name}")
    checks = {
        r"\b(?:class|method|field)_\d+\b": "raw intermediary Minecraft symbol",
        r"\b(?:EncryptedStringDispatcher|StringDecryptor|IndyRuntime|LinkageRuntime|HandleDispatcher|BytecodeInterpreter|ApplicationInterpreter|LegacyRegistry|OpaqueDispatcher)\b": "application obfuscation infrastructure",
        r"\b(?:Cipher|SecretKeySpec|DESKeySpec)\b": "runtime decryption (move recovered strings/logic into ordinary source)",
        r"\b(?:invokeExact|invoke)\s*\(": "MethodHandle invocation outside reviewed native ABI source",
        r"\b(?:MethodHandle|MethodHandles)\b": "MethodHandle infrastructure outside reviewed native ABI source",
        r"\b(?:include_bytes|global_asm|asm)\s*!": "embedded binary/assembly in Rust source",
        r"\b(?:todo|unimplemented)\s*!": "unfinished Rust implementation",
        r'\bthrow\s+new\s+(?:UnsupportedOperationException|IllegalStateException)\s*\(\s*"(?:[^"]*(?:not implemented|decompil|TODO|unresolved|placeholder))': "unfinished Java implementation",
    }
    for pattern, reason in checks.items():
        inspected = literal_code if "throw" in pattern else code
        if re.search(pattern, inspected, re.IGNORECASE if "throw" in pattern else 0):
            errors.append(f"{label}: {reason}")
    if re.search(r"\b(?:Base64\s*\.\s*getDecoder|Base64\s*\.\s*getMimeDecoder)\s*\(\s*\)\s*\.\s*decode\s*\(\s*\"[A-Za-z0-9+/=]{128,}", literal_code):
        errors.append(f"{label}: encoded opaque application payload")
    if re.search(r"\b(?:byte|char|short|int|long)\s*\[\s*\]\s*\w+\s*=\s*\{(?:\s*(?:-?\d+[Ll]?|0x[0-9a-fA-F]+[Ll]?)\s*,){64,}", code):
        errors.append(f"{label}: opaque numeric table requires readable source representation")
    return errors


def safe_path(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or Path(relative).is_absolute():
        raise GateError(f"Expected a project-relative path, got {relative!r}")
    candidate = root / relative
    if candidate.is_symlink() or not candidate.resolve().is_relative_to(root.resolve()):
        raise GateError(f"Input escapes clean-source project: {relative}")
    if not candidate.is_file():
        raise GateError(f"Missing input: {relative}")
    return candidate


def verified_input(root: Path, record: dict, allowed_suffixes: set[str] | None = None) -> Path:
    path = safe_path(root, record["path"])
    if allowed_suffixes and path.suffix not in allowed_suffixes:
        raise GateError(f"Expected readable source/evidence, got {record['path']}")
    if sha256(path) != record.get("sha256"):
        raise GateError(f"Stale or missing SHA-256: {record['path']}")
    return path


def verify_generated_native_output(root: Path, record: dict) -> None:
    """Recognize one hash-bound source-built owner output, never a source input."""
    output = verified_input(root, record, {".dll", ".so", ".dylib"})
    contract = json.loads(verified_input(root, record["build_contract"], {".json"}).read_text())
    if contract.get("artifact", {}).get("path") != record["path"] or contract["artifact"].get("sha256") != record["sha256"]:
        raise GateError("Generated native output does not match its source-build contract")
    if contract.get("original_binary_copied_or_linked") is not False or not contract.get("status", "").startswith("source_built_"):
        raise GateError("Generated native output must have an explicit source-built contract")
    module = contract.get("source_module", "")
    if not (root / module).resolve().is_relative_to((root / "native").resolve()):
        raise GateError("Generated native source module must be under native/")
    if not output.resolve().is_relative_to((root / module / "dist").resolve()):
        raise GateError("Reviewed owner output must be in its native module's dist directory")
    hashes = contract.get("source_hashes", {})
    if not {"Cargo.toml", "Cargo.lock"} <= hashes.keys() or not any(name.endswith(".rs") for name in hashes):
        raise GateError("Generated native contract lacks Rust/Cargo provenance")
    sources = {}
    for name, digest in hashes.items():
        relative = module + "/" + name
        source = verified_input(root, {"path": relative, "sha256": digest}, {".rs", ".toml", ".lock"})
        if not source.resolve().is_relative_to((root / module).resolve()):
            raise GateError("Generated native contract source escapes its module")
        sources[relative] = digest
    verify_receipt(root, record["verification"], "generated-native:" + record["path"], sources)


def audit(root: Path) -> list[str]:
    errors = []
    manifest = json.loads((root / "completion.json").read_text())
    abi_reviews = {item["path"]: item for item in manifest.get("native_abi_sources", [])}
    generated_native = {item["path"]: item for item in manifest.get("generated_native_outputs", [])}
    inspected = 0
    for folder in (root / "src/main", root / "native"):
        if not folder.exists():
            continue
        for path in sorted(folder.rglob("*")):
            relative = path.relative_to(root).as_posix()
            folder_relative = path.relative_to(folder)
            # Cargo output and crate-level analysis/test/tool directories are not
            # production inputs. Do not exempt similarly named directories inside
            # Java resources or a Rust src/ tree. Cargo src/ and build.rs stay audited.
            if folder == root / "native" and (
                folder_relative.parts[:1] == ("target",)
                or len(folder_relative.parts) > 1 and folder_relative.parts[1] in {"target", "tests", "tools", "analysis"}
            ):
                continue
            if path.is_symlink():
                errors.append(f"{relative}: symlink input is not self-contained")
                continue
            if not path.is_file():
                continue
            suffix = path.suffix.lower()
            if relative in generated_native:
                try:
                    verify_generated_native_output(root, generated_native[relative])
                except (GateError, KeyError, ValueError) as error:
                    errors.append(f"{relative}: {error}")
                continue
            if suffix in OPAQUE_SUFFIXES:
                errors.append(f"{relative}: opaque binary/assembly source input")
            elif suffix in SOURCE_SUFFIXES:
                inspected += 1
                source_errors = check_source(path.read_text(encoding="utf-8"), relative)
                if relative in abi_reviews:
                    try:
                        review = abi_reviews[relative]
                        verified_input(root, review, SOURCE_SUFFIXES)
                        verified_input(root, review["review"], {".md", ".json"})
                        if review.get("purpose") != "native-abi":
                            raise GateError("Native ABI review must declare purpose=native-abi")
                        source_errors = [error for error in source_errors if "outside reviewed native ABI source" not in error]
                    except (GateError, KeyError) as error:
                        errors.append(f"{relative}: {error}")
                errors.extend(source_errors)
            elif suffix in TEXT_RESOURCES or path.name in {"LICENSE", "NOTICE", "Cargo.lock"} or "licenses" in folder_relative.parts:
                try:
                    text = path.read_text(encoding="utf-8")
                    if "\x00" in text:
                        errors.append(f"{relative}: binary data disguised as text")
                    if suffix == ".accesswidener" and text.splitlines()[0] not in {"accessWidener v1 official", "accessWidener v2 official"}:
                        errors.append(f"{relative}: access widener must use the official namespace required by 26.x")
                except (UnicodeError, IndexError):
                    errors.append(f"{relative}: invalid textual resource")
            elif suffix in MEDIA_RESOURCES:
                # Legitimate image/font/audio assets are data, not hidden executable payloads.
                magic = path.read_bytes()[:8]
                if magic.startswith((b"PK\x03\x04", b"\xca\xfe\xba\xbe", b"MZ", b"\x7fELF", b"\x00asm")):
                    errors.append(f"{relative}: executable/archive disguised as media")
            else:
                errors.append(f"{relative}: unclassified source/resource type")
    # Build scripts use ordinary Loom, never assembly or recovered inputs.
    for pattern in ("*.gradle", "gradle/*.gradle"):
        for path in root.glob(pattern):
            if not path.is_file():
                continue
            code = strip_comments(path.read_text(), strip_strings=False)
            forbidden = re.search(r"(?i)(asmtools|\.jasm\b|graft(?:Hybrid|Methods|ing)|recovered-classes\.jar|echo-client-recreated|echo-reconstruction[/\\]|typed-indy-recovery|(?:compileOnly|implementation|runtimeOnly|modImplementation)\s+files\s*\()", code)
            if forbidden:
                errors.append(f"{path.relative_to(root)}: forbidden production build input/stage: {forbidden[0]}")
    print(f"Source policy inspected {inspected} first-party Java/Rust files; {len(errors)} violations")
    return errors


def verify_receipt(root: Path, record: dict, expected_id: str, source_hashes: dict[str, str]) -> None:
    path = verified_input(root, record, {".json"})
    receipt = json.loads(path.read_text())
    if receipt.get("status") != "passed" or expected_id not in receipt.get("covers", []):
        raise GateError(f"Verification receipt must pass and cover {expected_id}: {record['path']}")
    if not receipt.get("checks") or not receipt.get("command"):
        raise GateError(f"Verification receipt needs substantive checks and reproduction command: {record['path']}")
    if not receipt.get("observed") or receipt.get("observed") != receipt.get("expected"):
        raise GateError(f"Receipt must include matching nonempty expected/observed results: {record['path']}")
    for name, digest in source_hashes.items():
        if receipt.get("source_sha256", {}).get(name) != digest:
            raise GateError(f"Receipt is not bound to current functional source {name}: {record['path']}")


def complete(root: Path) -> list[str]:
    errors = []
    manifest = json.loads((root / "completion.json").read_text())
    contract = json.loads((root / "gradle/source-coverage.json").read_text())
    if manifest.get("schema") != 1 or manifest.get("status") != "complete" or manifest.get("unresolved"):
        errors.append("Clean functional source is incomplete; completion.json must retain unresolved work until verified.")
    if not list((root / "src/main/java").rglob("*.java")):
        errors.append("No first-party Java sources; refusing to produce an empty mod.")
    for section in ("features", "settings", "components"):
        expected = set(contract[section] if section == "components" else (item["id"] for item in contract[section]))
        records = manifest.get(section, [])
        actual = [item.get("id") for item in records]
        if len(actual) != len(set(actual)) or set(actual) != expected:
            errors.append(f"{section}: need all {len(expected)} distinct contract IDs; got {len(actual)} (missing {len(expected - set(actual))})")
        for record in records:
            identity = f"{section}:{record.get('id')}"
            try:
                if not record.get("sources"):
                    raise GateError(f"{identity}: no functional source evidence")
                hashes = {}
                for source in record["sources"]:
                    source_path = verified_input(root, source, SOURCE_SUFFIXES)
                    if not (source_path.is_relative_to(root / "src/main/java") or source_path.is_relative_to(root / "native")):
                        raise GateError(f"{identity}: functional source must be in production Java/Rust roots")
                    hashes[source["path"]] = source["sha256"]
                verify_receipt(root, record["verification"], identity, hashes)
            except (GateError, KeyError, ValueError) as error:
                errors.append(str(error))
    try:
        metadata = json.loads(safe_path(root, "src/main/resources/fabric.mod.json").read_text())
        entries = metadata.get("entrypoints", {}).get("main", [])
        if entries != ["dev.echo.client.bootstrap.EchoInitializer"] or metadata.get("entrypoints", {}).get("preLaunch") != ["dev.echo.client.EchoPreLaunch"]:
            raise GateError("The source-owned main/preLaunch entrypoints are required exactly once")
        if metadata.get("entrypoints", {}).get("client"):
            raise GateError("Do not duplicate the source-owned initializer as client")
        source = safe_path(root, "src/main/java/dev/echo/client/bootstrap/EchoInitializer.java")
        text = strip_comments(source.read_text(), strip_strings=True)
        if not re.search(r"\bonInitialize\s*\(\s*\)\s*\{\s*[^\s}]", text):
            raise GateError("Source-owned main initializer is missing/nonfunctional")
        depends = metadata.get("depends", {})
        required = {"minecraft": "26.2", "fabricloader": ">=0.20.1", "fabric-api": ">=0.161.0+26.2", "java": ">=25"}
        for key, version in required.items():
            if depends.get(key) != version:
                raise GateError(f"fabric.mod.json must declare {key}: {version}")
    except (GateError, ValueError, KeyError) as error:
        errors.append(str(error))
    return errors


class ClassFile:
    """Minimal read-only constant-pool/member/bootstrap parser, no ASM dependency."""

    def __init__(self, data: bytes):
        self.data, self.offset = data, 0
        if self.take(4) != b"\xca\xfe\xba\xbe":
            raise GateError("Invalid JVM class header")
        self.minor, self.major = self.u2(), self.u2()
        self.pool: list[tuple | None] = [None]
        size = self.u2()
        while len(self.pool) < size:
            tag = self.u1()
            if tag == 1:
                value = (tag, self.take(self.u2()).decode("utf-8", errors="replace"))
            elif tag in (3, 4):
                value = (tag, self.take(4))
            elif tag in (5, 6):
                value = (tag, self.take(8))
            elif tag in (7, 8, 16, 19, 20):
                value = (tag, self.u2())
            elif tag in (9, 10, 11, 12, 17, 18):
                value = (tag, self.u2(), self.u2())
            elif tag == 15:
                value = (tag, self.u1(), self.u2())
            else:
                raise GateError(f"Unknown JVM constant pool tag {tag}")
            self.pool.append(value)
            if tag in (5, 6):
                self.pool.append(None)
        self.flags = self.u2()
        self.name = self.class_name(self.u2())
        self.superclass = self.u2()
        for _ in range(self.u2()):
            self.u2()
        self.fields = self.members()
        self.methods = self.members()
        self.attributes = self.attributes_read()
        if self.offset != len(data):
            raise GateError("Unexpected trailing classfile data")

    def take(self, count: int) -> bytes:
        end = self.offset + count
        if end > len(self.data):
            raise GateError("Truncated classfile")
        result = self.data[self.offset:end]
        self.offset = end
        return result

    def u1(self) -> int:
        return self.take(1)[0]

    def u2(self) -> int:
        return struct.unpack(">H", self.take(2))[0]

    def u4(self) -> int:
        return struct.unpack(">I", self.take(4))[0]

    def utf(self, index: int) -> str:
        item = self.pool[index]
        if item is None or item[0] != 1:
            raise GateError("Expected UTF8 constant")
        return item[1]

    def class_name(self, index: int) -> str:
        return self.utf(self.pool[index][1])

    def attributes_read(self) -> dict[str, bytes]:
        result = {}
        for _ in range(self.u2()):
            name = self.utf(self.u2())
            result[name] = self.take(self.u4())
        return result

    def members(self) -> list[dict]:
        result = []
        for _ in range(self.u2()):
            result.append({"flags": self.u2(), "name": self.utf(self.u2()),
                           "descriptor": self.utf(self.u2()), "attributes": self.attributes_read()})
        return result

    def bootstraps(self) -> list[tuple[str, str]]:
        data = self.attributes.get("BootstrapMethods", b"\x00\x00")
        offset = 0

        def next_short():
            nonlocal offset
            result = struct.unpack_from(">H", data, offset)[0]
            offset += 2
            return result

        result = []
        for _ in range(next_short()):
            handle = self.pool[next_short()]
            reference = self.pool[handle[2]]
            name_and_type = self.pool[reference[2]]
            result.append((self.class_name(reference[1]), self.utf(name_and_type[1])))
            for _ in range(next_short()):
                next_short()
        return result


def check_class(data: bytes, label: str, remapped: bool = False, native_abi: bool = False) -> list[str]:
    errors = []
    parsed = ClassFile(data)
    if parsed.major != 65 or parsed.minor != 0:
        errors.append(f"{label}: production Java must be Java 21 without preview flags")
    for item in parsed.pool:
        if item is None:
            continue
        if item[0] == 7:
            name = parsed.utf(item[1])
            if any(name.startswith(prefix) for prefix in BLOCKED_NAMES):
                errors.append(f"{label}: retained obfuscator type {name}")
            if name.rsplit("/", 1)[-1] in DISPATCH_TYPES:
                errors.append(f"{label}: custom dispatch class {name}")
            if not remapped and re.search(r"net/minecraft/class_\d+", name):
                errors.append(f"{label}: development classes must use official 26.x names")
        if item[0] in (10, 11):
            owner = parsed.class_name(item[1])
            name = parsed.utf(parsed.pool[item[2]][1])
            if name in FORBIDDEN_METHODS or (owner == "java/lang/invoke/MethodHandle" and name in {"invoke", "invokeExact"} and not native_abi):
                errors.append(f"{label}: hidden code loading/dispatch {owner}.{name}")
            if owner.startswith("javax/crypto/"):
                errors.append(f"{label}: application runtime decryption {owner}.{name}")
    for bootstrap in parsed.bootstraps():
        if bootstrap not in STANDARD_BOOTSTRAPS:
            errors.append(f"{label}: custom application bootstrap {bootstrap[0]}.{bootstrap[1]}")
    return errors


def compiled(root: Path) -> list[str]:
    """Audit javac output before any normal Fabric remapping takes place."""
    errors = []
    manifest = json.loads((root / "completion.json").read_text())
    classes = sorted((root / "build/classes/java/main").rglob("*.class"))
    if not classes:
        errors.append("No source-compiled application classes")
    for path in classes:
        data = path.read_bytes()
        parsed = ClassFile(data)
        source_index = parsed.attributes.get("SourceFile")
        if not source_index:
            errors.append(f"{path.name}: missing source attribution")
            continue
        source_name = parsed.utf(struct.unpack(">H", source_index)[0])
        source = "src/main/java/" + str(Path(parsed.name).parent / source_name)
        if not (root / source).is_file():
            errors.append(f"{path.name}: missing ordinary source {source}")
        reviewed = any(item["path"] == source for item in manifest.get("native_abi_sources", []))
        errors.extend(check_class(data, path.name, native_abi=reviewed))
    print(f"Compiled-source audit inspected {len(classes)} named Java classes")
    return errors


def archive(root: Path, path: Path) -> list[str]:
    errors = []
    manifest = json.loads((root / "completion.json").read_text())
    native = {item["archive_path"]: item for item in manifest.get("native_artifacts", [])}
    allowed_libraries = {}
    for project in ("jsvg", "rhino"):
        jars = list((root / "third-party" / project / "build/libs").glob("*.jar"))
        for jar in jars:
            if not jar.name.endswith("-sources.jar"):
                allowed_libraries[sha256(jar)] = project
    java_count = 0
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            errors.append("Release archive contains duplicate entries")
        for name in names:
            if name.endswith("/"):
                continue
            if name.startswith("/") or ".." in Path(name).parts:
                errors.append(f"Unsafe archive entry {name}")
                continue
            data = jar.read(name)
            suffix = Path(name).suffix.lower()
            if suffix == ".class":
                java_count += 1
                parsed = ClassFile(data)
                source_name = parsed.attributes.get("SourceFile")
                if not source_name:
                    errors.append(f"{name}: missing source attribution")
                    continue
                source = parsed.utf(struct.unpack(">H", source_name)[0])
                relative = "src/main/java/" + str(Path(parsed.name).parent / source)
                if not (root / relative).is_file():
                    errors.append(f"{name}: no ordinary first-party Java source {relative}")
                reviewed_abi = any(item["path"] == relative for item in manifest.get("native_abi_sources", []))
                errors.extend(check_class(data, name, remapped=True, native_abi=reviewed_abi))
            elif suffix == ".jar":
                # Loom adds generated Fabric metadata to included non-mod JARs;
                # compare every original entry instead of expecting whole-JAR equality.
                import io
                accepted = False
                with zipfile.ZipFile(io.BytesIO(data)) as nested:
                    for project in allowed_libraries.values():
                        for original in (root / "third-party" / project / "build/libs").glob("*.jar"):
                            if original.name.endswith("-sources.jar"):
                                continue
                            with zipfile.ZipFile(original) as upstream:
                                expected = {n: upstream.read(n) for n in upstream.namelist() if not n.endswith("/")}
                                actual = {n: nested.read(n) for n in nested.namelist() if not n.endswith("/")}
                            if all(actual.get(n) == value for n, value in expected.items()) and set(actual) - set(expected) <= {"fabric.mod.json"}:
                                accepted = True
                if not accepted:
                    errors.append(f"{name}: unrecognized nested executable dependency")
            elif suffix in {".dll", ".so", ".dylib"}:
                record = native.get(name)
                if not record or hashlib.sha256(data).hexdigest() != record.get("sha256"):
                    errors.append(f"{name}: native artifact lacks source-built hash provenance")
                else:
                    try:
                        sources = {item["path"]: sha256(verified_input(root, item, SOURCE_SUFFIXES)) for item in record["sources"]}
                        if not sources:
                            raise GateError(f"{name}: no Rust source provenance")
                        verify_receipt(root, record["verification"], f"native:{name}", sources)
                    except (GateError, KeyError) as error:
                        errors.append(str(error))
            elif suffix in OPAQUE_SUFFIXES:
                errors.append(f"{name}: opaque/assembly payload in release")
            elif suffix not in TEXT_RESOURCES | MEDIA_RESOURCES and name not in {"META-INF/MANIFEST.MF", "LICENSE", "NOTICE"}:
                errors.append(f"{name}: unclassified release resource")
        if "fabric.mod.json" not in names:
            errors.append("Missing Fabric metadata")
    if java_count == 0:
        errors.append("No first-party classes; empty mod is forbidden")
    print(f"Release audit inspected {java_count} first-party classes")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("audit", "complete", "compiled", "archive"))
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--archive", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    try:
        if args.mode == "audit":
            errors = audit(root)
        elif args.mode == "complete":
            errors = complete(root)
        elif args.mode == "compiled":
            errors = compiled(root)
        else:
            if args.archive is None:
                parser.error("archive mode requires --archive")
            errors = archive(root, args.archive)
    except (GateError, OSError, KeyError, ValueError, struct.error, zipfile.BadZipFile) as error:
        errors = [f"Invalid verification input: {error}"]
    for error in errors:
        print(f"CLEAN-SOURCE GATE: {error}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
