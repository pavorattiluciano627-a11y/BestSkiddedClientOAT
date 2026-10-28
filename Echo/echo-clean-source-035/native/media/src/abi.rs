//! Compile-time checks for the six Java static-native signatures.
//!
//! A JNI environment and class reference are the two implicit arguments. The
//! "system" convention is Win64 on the supported target, for MSVC and GNU alike.
use crate::windows_adapter;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jlong, jstring};
use jni::JNIEnv;

type LongGetter = for<'env, 'class> extern "system" fn(JNIEnv<'env>, JClass<'class>) -> jlong;
type StringGetter = for<'env, 'class> extern "system" fn(JNIEnv<'env>, JClass<'class>) -> jstring;
type BytesGetter = for<'env, 'class> extern "system" fn(JNIEnv<'env>, JClass<'class>) -> jbyteArray;

const _: LongGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_revision;
const _: StringGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_title;
const _: StringGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_artist;
const _: BytesGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_cover;
const _: LongGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_positionMillis;
const _: LongGetter = windows_adapter::Java_dev_echo_client_media_MediaSession_durationMillis;
