//! Compiles unmodified, licensed STB1. The generated TU contains directives only.
use std::{env, fs, path::PathBuf};

fn main() {
    let header = PathBuf::from("../../third-party/stb-legacy/stb_image_resize.h");
    println!("cargo:rerun-if-changed={}", header.display());
    println!("cargo:rerun-if-changed=build.rs");
    let out = PathBuf::from(env::var_os("OUT_DIR").expect("Cargo OUT_DIR"));
    let unit = out.join("stb_implementation.c");
    // LWJGL 3.3.3 also disables STBIR_ASSERT. Allocation here is private libc
    // scratch memory; caller-owned pixels never cross allocator ownership.
    fs::write(&unit, "#define STBIR_ASSERT(x)\n#define STB_IMAGE_RESIZE_IMPLEMENTATION\n#include \"stb_image_resize.h\"\n")
        .expect("write STB compilation unit");
    let mut build = cc::Build::new();
    build.file(&unit).include("../../third-party/stb-legacy").warnings(false);
    // Preserve ordinary IEEE evaluation: no fast-math or FP contraction that
    // changes STB1's scalar filter coefficients/byte rounding between compilers.
    if env::var("CARGO_CFG_TARGET_ENV").as_deref() == Ok("msvc") {
        build.flag("/fp:strict");
    } else {
        build.flag("-fno-fast-math").flag("-ffp-contract=off");
    }
    build.compile("echo_stb1");
    if env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("macos") {
        println!("cargo:rustc-cdylib-link-arg=-Wl,-install_name,@rpath/libecho_stb_legacy.dylib");
    }
    if env::var("CARGO_CFG_TARGET_FAMILY").as_deref() == Ok("unix") {
        println!("cargo:rustc-link-lib=m");
    }
}
