#!/usr/bin/env python3
"""Read the built PE's actual export/import tables without loading its code."""
import argparse
import hashlib
import json
from pathlib import Path
import struct

PREFIX='Java_dev_echo_client_media_MediaSession_'
EXPECTED={PREFIX+n for n in ('revision','title','artist','cover','positionMillis','durationMillis')}
# BCryptPrimitives/ProcessPrng is a documented Windows system API (Windows 8+):
# https://learn.microsoft.com/en-us/windows/win32/seccng/processprng
SYSTEM_DLLS={'advapi32.dll','bcrypt.dll','bcryptprimitives.dll','combase.dll','crypt32.dll','dbghelp.dll','gdi32.dll',
             'imm32.dll','kernel32.dll','kernelbase.dll','msvcrt.dll','ntdll.dll','ole32.dll',
             'oleaut32.dll','powrprof.dll','psapi.dll','rpcrt4.dll','runtimeobject.dll','secur32.dll',
             'shell32.dll','shlwapi.dll','ucrtbase.dll','user32.dll','userenv.dll','version.dll',
             'winmm.dll','wintrust.dll','ws2_32.dll','windows.storage.dll'}

def inspect(path, expected_exports=EXPECTED, require_smtc=True):
    return inspect_bytes(path.read_bytes(), str(path), expected_exports, require_smtc)


def inspect_bytes(data, label, expected_exports=EXPECTED, require_smtc=True):
    def u16(o):return struct.unpack_from('<H',data,o)[0]
    def u32(o):return struct.unpack_from('<I',data,o)[0]
    def u64(o):return struct.unpack_from('<Q',data,o)[0]
    def string(o):
        end=data.index(b'\0',o)
        return data[o:end].decode('ascii')
    assert data[:2]==b'MZ'
    pe=u32(0x3c);assert data[pe:pe+4]==b'PE\0\0'
    coff=pe+4;machine=u16(coff);sections_count=u16(coff+2);optional_size=u16(coff+16);characteristics=u16(coff+18)
    optional=coff+20;magic=u16(optional);assert magic==0x20b,'Expected PE32+ image'
    sections=[]
    for i in range(sections_count):
        o=optional+optional_size+i*40
        sections.append({'name':data[o:o+8].split(b'\0')[0].decode('ascii'),
                         'virtual_size':u32(o+8),'rva':u32(o+12),'raw_size':u32(o+16),'raw_offset':u32(o+20),
                         'characteristics':u32(o+36)})
    def offset(rva):
        if rva<u32(optional+60):return rva
        for section in sections:
            delta=rva-section['rva']
            if 0<=delta<max(section['virtual_size'],section['raw_size']):
                assert delta<section['raw_size'],'RVA points into uninitialized data'
                return section['raw_offset']+delta
        raise ValueError(f'Unmapped RVA {rva:x}')
    def rva_string(rva):return string(offset(rva))
    def directory(index):
        assert index<u32(optional+108)
        o=optional+112+index*8
        return u32(o),u32(o+4)
    export_rva,export_size=directory(0);assert export_rva
    e=offset(export_rva)
    ordinal_base=u32(e+16);functions_count=u32(e+20);names_count=u32(e+24)
    functions=offset(u32(e+28));names=offset(u32(e+32));ordinals=offset(u32(e+36))
    exports=[]
    for i in range(names_count):
        name=rva_string(u32(names+i*4));index=u16(ordinals+i*2);assert index<functions_count
        address=u32(functions+index*4)
        forwarder=rva_string(address) if export_rva<=address<export_rva+export_size else None
        executable=any(s['rva']<=address<s['rva']+s['virtual_size'] and s['characteristics']&0x20000000 for s in sections)
        exports.append({'name':name,'ordinal':ordinal_base+index,'rva':f'0x{address:08x}','forwarder':forwarder,'executable_code':bool(executable)})
    imports=[]
    import_rva,import_size=directory(1)
    if import_rva:
        o=offset(import_rva)
        while any(data[o:o+20]):
            lookup=u32(o) or u32(o+16);dll=rva_string(u32(o+12));entries=[];t=offset(lookup)
            while value:=u64(t):
                entries.append({'ordinal':value&0xffff} if value&(1<<63) else {'name':string(offset(value)+2)})
                t+=8
            imports.append({'dll':dll,'symbols':entries});o+=20
    delay_rva,delay_size=directory(13)
    names_set={x['name'] for x in exports}
    assert machine==0x8664 and characteristics&0x2000,'Not an AMD64 DLL'
    assert names_set==expected_exports,{'missing':sorted(expected_exports-names_set),'unexpected':sorted(names_set-expected_exports)}
    assert all(x['executable_code'] and x['forwarder'] is None for x in exports)
    assert not delay_rva and not delay_size,'Delay imports require explicit additional review'
    symbols={s['name'] for lib in imports for s in lib['symbols'] if 'name' in s}
    if require_smtc:
        assert 'CoInitializeEx' in symbols,'Windows COM implementation was not linked'
        assert 'RoGetActivationFactory' in symbols,'WinRT activation code was not linked'
    activation='Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager'
    activation_encodings=[encoding for encoding in ('utf-8','utf-16-le') if activation.encode(encoding) in data]
    if require_smtc:
        assert activation_encodings,'Expected SMTC activation class is missing'
    extra=[x['dll'] for x in imports if x['dll'].lower() not in SYSTEM_DLLS and not x['dll'].lower().startswith(('api-ms-win-','ext-ms-win-'))]
    return {'path':label,'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data),
            'machine':'AMD64 (0x8664)','format':'PE32+','is_dll':True,'characteristics':f'0x{characteristics:04x}',
            'coff_timestamp':u32(coff+4),'entrypoint_rva':f'0x{u32(optional+16):08x}',
            'image_base':f'0x{u64(optional+24):016x}','exports':exports,'imports':imports,
            'non_system_dependencies':extra,'smtc_activation_class_present':bool(activation_encodings),
            'smtc_activation_class_encodings':activation_encodings,
            'com_initialization_import_present':'CoInitializeEx' in symbols,'winrt_activation_import_present':'RoGetActivationFactory' in symbols,
            'exact_canonical_exports':True,'delay_imports_present':False,'code_executed':False}

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('dll',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    result=inspect(a.dll);a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ('imports','exports')},indent=2))
