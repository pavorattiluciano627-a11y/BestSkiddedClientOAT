//! JNI reconstruction of the exports actually present in the supplied Naga DLL.
//!
//! Module handles are owning Box pointers. Callers must preserve their lifetime,
//! use the matching free routine once, and not share freed/foreign pointers.
//! The original library does not export emitMsl; neither does this crate.

use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jint, jlong, jstring};
use jni::JNIEnv;
use naga::valid::{Capabilities, ModuleInfo, ValidationFlags, Validator};
use naga::{Handle, Module};
use std::ptr;

mod glsl;

fn throw_runtime(env: &mut JNIEnv<'_>, message: impl AsRef<str>) {
    // The original helper ignores ThrowNew's Result after dropping its error.
    let _ = env.throw_new("java/lang/RuntimeException", message.as_ref());
}

fn java_string(env: &JNIEnv<'_>, value: &str) -> jstring {
    env.new_string(value)
        .map(JString::into_raw)
        .unwrap_or(ptr::null_mut())
}

fn read_string(env: &mut JNIEnv<'_>, value: &JString<'_>) -> String {
    env.get_string(value).map(String::from).unwrap_or_default()
}

fn handle<T>(index: jint) -> Handle<T> {
    // src/handles.rs converts a signed integer through serde and expects success.
    // In particular, a negative index is a Rust panic, not "invalid type handle".
    serde_json::from_str(&index.to_string()).expect("invalid handle index")
}

/// Frees an owning module pointer; zero is a no-op, matching RVA 0x67a4.
///
/// # Safety
/// A nonzero argument must be a live pointer returned by this library's parseWgsl.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_moduleFree(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
) {
    if module != 0 {
        drop(Box::from_raw(module as *mut Module));
    }
}

/// Frees an owning validation-info pointer; zero is a no-op, matching RVA 0x6aeb.
///
/// # Safety
/// A nonzero argument must be a live pointer returned by this library's validate.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_moduleInfoFree(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    info: jlong,
) {
    if info != 0 {
        drop(Box::from_raw(info as *mut ModuleInfo));
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_render_shader_naga_Naga_parseWgsl(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    source: JString<'_>,
) -> jlong {
    let source = read_string(&mut env, &source);
    match naga::front::wgsl::parse_str(&source) {
        Ok(module) => Box::into_raw(Box::new(module)) as jlong,
        Err(error) => {
            throw_runtime(&mut env, error.emit_to_string(&source));
            0
        }
    }
}

/// # Safety
/// `module` must point to a live Module allocated by this library.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_validate(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
    flags: jint,
    capabilities: jlong,
) -> jlong {
    let module = &*(module as *const Module);
    let mut validator = Validator::new(
        ValidationFlags::from_bits_truncate(flags as u8),
        Capabilities::from_bits_truncate(capabilities as u32),
    );
    match validator.validate(module) {
        Ok(info) => Box::into_raw(Box::new(info)) as jlong,
        Err(error) => {
            throw_runtime(&mut env, format!("{error:?}"));
            0
        }
    }
}

/// # Safety
/// Both pointers must be live and the info must be validation of this module.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_emitSpv(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
    info: jlong,
    flags: jint,
) -> jbyteArray {
    let options = naga::back::spv::Options {
        flags: naga::back::spv::WriterFlags::from_bits_truncate(flags as u32),
        ..Default::default()
    };
    match naga::back::spv::write_vec(
        &*(module as *const Module),
        &*(info as *const ModuleInfo),
        &options,
        None,
    ) {
        Ok(words) => {
            let bytes: Vec<u8> = words.into_iter().flat_map(u32::to_le_bytes).collect();
            env.byte_array_from_slice(&bytes)
                .map(|array| array.into_raw())
                .unwrap_or(ptr::null_mut())
        }
        Err(error) => {
            throw_runtime(&mut env, format!("{error:?}"));
            ptr::null_mut()
        }
    }
}

/// # Safety
/// Both pointers must be live and the info must be validation of this module.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_emitGlsl(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
    info: jlong,
    version: jint,
    stage: jint,
    entry_point: JString<'_>,
) -> jstring {
    // The original converts the Java string before rejecting the stage.
    let entry_point = read_string(&mut env, &entry_point);
    let stage = match stage {
        0 => naga::ShaderStage::Vertex,
        1 => naga::ShaderStage::Fragment,
        2 => naga::ShaderStage::Compute,
        _ => {
            throw_runtime(&mut env, "invalid stage");
            return ptr::null_mut();
        }
    };
    match glsl::emit(
        &*(module as *const Module),
        &*(info as *const ModuleInfo),
        version as u16,
        stage,
        entry_point,
    ) {
        Ok(json) => java_string(&env, &json),
        Err(error) => {
            throw_runtime(&mut env, error);
            ptr::null_mut()
        }
    }
}

/// # Safety
/// `module` must point to a live Module allocated by this library.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_numGlobalVariables(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
) -> jint {
    (&*(module as *const Module)).global_variables.len() as jint
}

/// # Safety
/// `module` must point to a live Module allocated by this library.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_typeJson(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
    index: jint,
) -> jstring {
    let module = &*(module as *const Module);
    let Some(value) = module.types.get_handle(handle(index)).ok() else {
        throw_runtime(&mut env, "invalid type handle");
        return ptr::null_mut();
    };
    match serde_json::to_string(value) {
        Ok(json) => java_string(&env, &json),
        Err(error) => {
            throw_runtime(&mut env, error.to_string());
            ptr::null_mut()
        }
    }
}

/// # Safety
/// `module` must point to a live Module allocated by this library.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_render_shader_naga_Naga_globalVariableJson(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    module: jlong,
    index: jint,
) -> jstring {
    let module = &*(module as *const Module);
    let Some(value) = module.global_variables.try_get(handle(index)).ok() else {
        throw_runtime(&mut env, "invalid global handle");
        return ptr::null_mut();
    };
    match serde_json::to_string(value) {
        Ok(json) => java_string(&env, &json),
        Err(error) => {
            throw_runtime(&mut env, error.to_string());
            ptr::null_mut()
        }
    }
}
