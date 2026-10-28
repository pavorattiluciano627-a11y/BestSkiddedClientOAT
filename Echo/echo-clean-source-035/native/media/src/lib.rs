//! Windows x86-64 SMTC adapter reconstructed from the six original JNI exports.
//! Non-Windows builds expose no media JNI symbols; they compile only policy tests.

#[cfg(any(test, all(target_os = "windows", target_arch = "x86_64")))]
mod state;

#[cfg(all(target_os = "windows", target_arch = "x86_64"))]
mod windows_adapter;

#[cfg(all(target_os = "windows", target_arch = "x86_64"))]
mod abi;
