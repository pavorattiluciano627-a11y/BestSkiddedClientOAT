//! Ordinary JNI ABI forwarding only; the resampling algorithm is official STB1.
use std::ffi::{c_int, c_void};

extern "C" {
    fn stbir_resize_uint8(input: *const u8, input_width: c_int, input_height: c_int,
        input_stride: c_int, output: *mut u8, output_width: c_int, output_height: c_int,
        output_stride: c_int, channels: c_int) -> c_int;
}

/// Exact `(JIIIJIIII)I` JNI ABI used by LegacyImageResize.resizeUint8.
///
/// # Safety
/// The Java caller supplies valid live pixel addresses and dimensions/strides.
/// This intentionally retains the former raw LWJGL operation's preconditions.
/// JNIEnv/jclass are opaque JNI handles; neither is dereferenced or retained.
#[no_mangle]
pub unsafe extern "system" fn Java_dev_echo_compat_lwjgl_LegacyImageResize_resizeUint8(
    _environment: *mut c_void, _class: *mut c_void,
    input: i64, input_width: i32, input_height: i32, input_stride: i32,
    output: i64, output_width: i32, output_height: i32, output_stride: i32,
    channels: i32,
) -> i32 {
    stbir_resize_uint8(input as usize as *const u8, input_width, input_height,
        input_stride, output as usize as *mut u8, output_width, output_height,
        output_stride, channels)
}
