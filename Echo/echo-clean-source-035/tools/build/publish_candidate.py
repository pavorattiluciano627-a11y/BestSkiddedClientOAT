#!/usr/bin/env python3
"""Bind the freshly compiled candidate to local editable sources and native receipts."""
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import struct
import zipfile
from source_gate import ClassFile
from artifact import validate

ROOT = Path(__file__).resolve().parents[2]
load = lambda p: json.loads(p.read_text())
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
rel = lambda p: str(p.resolve().relative_to(ROOT))
pin = lambda p: {'path': rel(p), 'sha256': sha(p)}

def main():
    compile = load(ROOT / 'build/reports/candidate/compile.json')
    # 26.x has no remap stage: the compiler's own archive is the artifact.
    jar = ROOT / compile['named_jar']; assert sha(jar) == compile['named_sha256']
    files = dict(compile['source_files'])
    for p, digest in files.items(): assert sha(ROOT / p) == digest
    by_source = {str(Path(p).relative_to('src/main/java')): p for p in files}
    evidence = [pin(ROOT / 'build/reports/candidate' / f) for f in ['compile.json', 'compile-native-command.json']]
    for name in ('build.gradle', 'settings.gradle', 'gradle.properties'):
        files[name] = sha(ROOT / name)
    library_sources = {}; library_evidence = {}
    for library in ('jsvg', 'rhino'):
        base = ROOT / 'third-party' / library
        paths = list((base / 'src/main/java').rglob('*.java')) + [p for p in (base / 'licenses').rglob('*') if p.is_file()]
        paths += [base / 'build.gradle', ROOT / 'third-party/source-provenance.json', ROOT / 'third-party/provenance.json']
        library_sources[library] = [rel(p) for p in paths]
        files.update({rel(p): sha(p) for p in paths})
        report = base / 'build/reports/source-patches.json'; assert load(report)['status'] == 'passed'
        library_evidence[library] = [pin(report), pin(ROOT / 'build/reports/candidate/compile-native-command.json')]
    entries = {}; classes = {}; nested = {}
    with zipfile.ZipFile(jar) as archive:
        names = [i.filename for i in archive.infolist() if not i.is_dir()]; assert len(names) == len(set(names))
        for name in names:
            data = archive.read(name); entries[name] = hashlib.sha256(data).hexdigest()
            if name.endswith('.class'):
                parsed = ClassFile(data); filename = parsed.utf(struct.unpack('>H', parsed.attributes['SourceFile'])[0])
                classes[name] = by_source[str(PurePosixPath(parsed.name).parent / filename)]
            if name.endswith('.jar'):
                with zipfile.ZipFile(io.BytesIO(data)) as dep:
                    lib = 'jsvg' if 'com/github/weisj/jsvg/SVGDocument.class' in dep.namelist() else 'rhino'
                    assert not any(p.startswith(('dev/echo/', 'hack/', 'dev/dov/')) for p in dep.namelist())
                nested[name] = {'kind': 'source-built-third-party', 'source_files': library_sources[lib], 'build_evidence': library_evidence[lib]}
        for crate in ('renderer', 'lwjgl-compat'):
            native = json.loads(archive.read('META-INF/echo-native/' + crate + '.json'))
            assert hashlib.sha256(archive.read(native['output']['resource'])).hexdigest() == native['output']['sha256']
            for path, digest in native['source_files'].items():
                name = str(PurePosixPath(native.get('source_directory', '')) / path)
                assert sha(ROOT / name) == digest; files[name] = digest
            report = ROOT / ('build/echo-native/macos-aarch64/renderer-receipt.json' if crate == 'renderer' else 'build/echo-stb-legacy-native/macos-aarch64/build-receipt.json')
            evidence.append(pin(report))
        for p in (ROOT / 'native/renderer/licenses').rglob('*'):
            if p.is_file(): files[rel(p)] = sha(p)
    baseline = load(ROOT / 'provenance/baseline-java.json')
    changes = sorted(p for p in set(baseline) | set(compile['source_files']) if baseline.get(p) != compile['source_files'].get(p))
    source_digest = hashlib.sha256(json.dumps(files, sort_keys=True, separators=(',', ':'), ensure_ascii=True).encode()).hexdigest()
    ledger = {'schema': 'echo-clean-runtime-artifact/v1', 'mode': 'java-rust-source', 'namespace': 'official',
        'artifact_sha256': sha(jar), 'source_files': files, 'source_set_sha256': source_digest, 'archive_entries': entries,
        'class_sources': classes, 'build_evidence': evidence, 'nested_dependencies': nested,
        'build': {'application_compiler': 'javac', 'java_release': 25, 'remapper': 'none',
            'application_bytecode_grafts': False, 'opaque_application_inputs': False,
            'commands': [load(ROOT / 'build/reports/candidate/compile-native-command.json')['command']]},
        'classification': 'unapproved-candidate-verification', 'production_approved': False,
        'baseline_java_unchanged': not changes, 'changed_java_files': changes, 'fresh_artifact_runtime_tested': False}
    sidecar = ROOT / 'build/candidate/provenance.json'; sidecar.write_text(json.dumps(ledger, sort_keys=True, indent=2) + '\n')
    checked = validate(jar, sidecar, sha(jar), sha(sidecar), root=ROOT)
    handoff = {'jar': rel(jar), 'jar_sha256': sha(jar), 'provenance': rel(sidecar), 'provenance_sha256': sha(sidecar),
        'application_sources': len(compile['source_files']), 'classes': len(classes), 'classification': ledger['classification'],
        'baseline_java_unchanged': not changes, 'static_contract_validation_passed': True, 'runtime_tested': False}
    (ROOT / 'build/candidate/HANDOFF.json').write_text(json.dumps(handoff, indent=2) + '\n')
    (ROOT / 'build/reports/candidate/static-validation.json').write_text(json.dumps(checked, indent=2) + '\n')
    print(json.dumps(handoff, indent=2))

if __name__ == '__main__': main()
