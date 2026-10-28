#!/usr/bin/env python3
"""Verify source-built library overlays and source archive completeness offline."""

import argparse
import hashlib
import json
import sys
import zipfile
from pathlib import Path

from source_gate import ClassFile, GateError


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True, type=Path)
    parser.add_argument("--package", required=True)
    parser.add_argument("--expected", required=True, type=int)
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--sources-jar", required=True, type=Path)
    args = parser.parse_args()
    sources = sorted((args.project / "src/main/java").rglob("*.java"))
    if len(sources) != args.expected:
        raise GateError("Wrong number of readable library patches")
    classes = args.project / "build/classes/java/main"
    provenance = {item["source"]: item for item in json.loads((args.project.parent / "source-provenance.json").read_text())}
    proof = []
    with zipfile.ZipFile(args.jar) as artifact, zipfile.ZipFile(args.sources_jar) as source_archive:
        if len(artifact.namelist()) != len(set(artifact.namelist())):
            raise GateError("Duplicate classes/resources in patched library")
        java_entries = [name for name in source_archive.namelist() if name.endswith(".java")]
        if len(java_entries) <= len(sources):
            raise GateError("Sources JAR must retain all public upstream source, not just patches")
        for source in sources:
            relative = source.relative_to(args.project / "src/main/java").as_posix()
            if hashlib.sha256(source.read_bytes()).hexdigest() != provenance[relative]["patched_sha256"]:
                raise GateError(f"Readable patch changed from retained semantic provenance: {relative}")
            original = args.project.parent / "upstream-src" / relative
            if hashlib.sha256(original.read_bytes()).hexdigest() != provenance[relative]["upstream_sha256"]:
                raise GateError(f"Upstream source provenance changed: {relative}")
            if source_archive.read(relative) != source.read_bytes():
                raise GateError(f"Sources archive does not contain patched source: {relative}")
            compiled = classes / relative.replace(".java", ".class")
            parsed = ClassFile(compiled.read_bytes())
            helpers = [method for method in parsed.methods if method["name"] == "echo$offlineLocalUrl"]
            if len(helpers) != 1 or helpers[0]["flags"] != 0x000A:
                raise GateError(f"Ordinary private static Java helper expected, without synthetic rewrite: {relative}")
            if parsed.major != 52:
                raise GateError("Library patches must retain Java 8 runtime compatibility")
            proof.append({"source": relative, "sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                          "class_sha256": hashlib.sha256(compiled.read_bytes()).hexdigest(),
                          "helper_flags": helpers[0]["flags"]})
        count = 0
        for compiled in classes.rglob("*.class"):
            relative = compiled.relative_to(classes).as_posix()
            if artifact.read(relative) != compiled.read_bytes():
                raise GateError(f"Packaged class differs from javac output: {relative}")
            count += 1
        for name in artifact.namelist():
            if name.endswith(".class") and not name.startswith(args.package):
                raise GateError(f"Non-upstream class in third-party artifact: {name}")
    output = args.project / "build/reports/source-patches.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({"status": "passed", "source_patches": proof,
                                 "compiled_classes": count, "upstream_source_files": len(java_entries)}, indent=2) + "\n")
    print(f"Verified {args.expected} ordinary source patches, {count} source-built classes, {len(java_entries)} upstream sources: {args.project.name}")


if __name__ == "__main__":
    try:
        main()
    except (GateError, OSError, KeyError, zipfile.BadZipFile) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
