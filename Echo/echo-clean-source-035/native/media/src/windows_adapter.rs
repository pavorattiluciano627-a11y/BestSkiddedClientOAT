//! Actual Windows SMTC calls and the six original JNI exports.
use crate::state::*;
use jni::objects::JClass;
use jni::sys::{jbyteArray, jlong, jstring};
use jni::JNIEnv;
use std::ptr;
use std::sync::{Mutex, MutexGuard, Once};
use std::thread;
use std::time::{Instant, SystemTime, UNIX_EPOCH};
use windows::core::Result;
use windows::Media::Control::{
    GlobalSystemMediaTransportControlsSession as Session,
    GlobalSystemMediaTransportControlsSessionManager as Manager,
    GlobalSystemMediaTransportControlsSessionMediaProperties as Properties,
    GlobalSystemMediaTransportControlsSessionPlaybackStatus as PlaybackStatus,
};
use windows::Storage::Streams::DataReader;
use windows::Win32::System::Com::{CoInitializeEx, COINIT_MULTITHREADED};

static START: Once = Once::new();
static CACHE: Mutex<Cache> = Mutex::new(Cache {
    metadata: None,
    timeline: None,
    last_access: None,
    revision: 0,
});

fn cache() -> MutexGuard<'static, Cache> {
    // The original getters take the cache even if the mutex was poisoned.
    CACHE
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

fn read_cache<T>(read: impl FnOnce(&Cache) -> T) -> T {
    let value = {
        let mut cache = cache();
        cache.touch();
        read(&cache)
    };
    // Read/clone first, spawn second, matching the original first-call behavior.
    START.call_once(|| {
        let _ = thread::Builder::new()
            .name("media-session".into())
            .spawn(worker);
    });
    value
}

fn playing(session: &Session) -> bool {
    session
        .GetPlaybackInfo()
        .and_then(|info| info.PlaybackStatus())
        .is_ok_and(|status| status == PlaybackStatus::Playing)
}

// Preserve the original HSTRING-to-UTF-8 conversion before comparing app IDs.
#[allow(clippy::cmp_owned)]
fn select(manager: &Manager, previous: Option<&Sample>) -> Option<Session> {
    let current = manager.GetCurrentSession().ok();
    let sessions: Vec<Session> = manager
        .GetSessions()
        .map(|sessions| sessions.into_iter().collect())
        .unwrap_or_default();
    let previous = previous.and_then(|old| {
        sessions
            .iter()
            .find(|session| {
                session
                    .SourceAppUserModelId()
                    .is_ok_and(|id| id.to_string() == old.key.app_id)
            })
            .cloned()
    });
    choose_session(previous, current, &sessions, playing)
}

fn read_sample(session: &Session) -> Result<(Sample, Properties)> {
    let properties = session.TryGetMediaPropertiesAsync()?.join()?;
    let key = TrackKey {
        app_id: session.SourceAppUserModelId()?.to_string(),
        title: properties.Title()?.to_string(),
        artist: properties.Artist()?.to_string(),
        album_title: properties.AlbumTitle()?.to_string(),
    };
    let timeline = session.GetTimelineProperties()?;
    let playback = session.GetPlaybackInfo()?;
    let start_ticks = timeline.StartTime()?.Duration;
    let end_ticks = timeline.EndTime()?.Duration;
    let position_ticks = timeline.Position()?.Duration;
    let updated_ticks = timeline.LastUpdatedTime()?.UniversalTime;
    let playing = playback.PlaybackStatus()? == PlaybackStatus::Playing;
    let rate = playback
        .PlaybackRate()
        .and_then(|rate| rate.Value())
        .unwrap_or(1.0);
    Ok((
        Sample {
            key,
            start_ticks,
            end_ticks,
            position_ticks,
            updated_ticks,
            rate,
            playing,
        },
        properties,
    ))
}

fn thumbnail(properties: &Properties) -> Option<Vec<u8>> {
    let stream = properties
        .Thumbnail()
        .ok()?
        .OpenReadAsync()
        .ok()?
        .join()
        .ok()?;
    let size = stream.Size().ok()?;
    if !(1..=MAX_THUMBNAIL_BYTES).contains(&size) {
        return None;
    }
    let reader = DataReader::CreateDataReader(&stream).ok()?;
    let loaded = reader.LoadAsync(size as u32).ok()?.join().ok()?;
    let mut bytes = vec![0; loaded as usize];
    reader.ReadBytes(&mut bytes).ok()?;
    Some(bytes)
}

fn windows_now_ticks() -> i64 {
    // SystemTime before UNIX_EPOCH contributes zero, as in the original worker.
    const WINDOWS_TO_UNIX_EPOCH: u128 = 116_444_736_000_000_000;
    let since_unix = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default();
    (since_unix.as_nanos() / 100 + WINDOWS_TO_UNIX_EPOCH) as i64
}

fn poll(manager: &Manager, state: &mut WorkerState) {
    let Some(session) = select(manager, state.previous.as_ref()) else {
        state.clear(&mut cache());
        return;
    };
    // SMTC read failures retain the preceding published snapshot and timeline.
    let Ok((sample, properties)) = read_sample(&session) else {
        return;
    };
    if sample.key.title.is_empty() {
        state.clear(&mut cache());
        return;
    }
    let changed = state
        .previous
        .as_ref()
        .is_none_or(|old| old.key != sample.key);
    let now_ticks = windows_now_ticks();
    let cover = if changed {
        thumbnail(&properties)
    } else {
        None
    };
    let now = Instant::now();
    {
        let mut cache = cache();
        // Metadata resets anchor after the thumbnail read; timing-only updates
        // obtain their anchor while the cache lock is held (original RVA 0x9830).
        let anchor = if changed { now } else { Instant::now() };
        state.publish(&mut cache, sample, cover, now_ticks, anchor);
    }
    if state.thumbnail_due(Instant::now()) {
        if let Some(bytes) = thumbnail(&properties) {
            state.publish_thumbnail(&mut cache(), bytes);
        }
    }
}

fn worker() {
    // MTA initialization is attempted once; the original ignores this HRESULT.
    let _ = unsafe { CoInitializeEx(None, COINIT_MULTITHREADED) };
    let mut manager = None;
    let mut state = WorkerState::default();
    loop {
        if !cache().active(Instant::now()) {
            thread::sleep(IDLE_POLL);
            continue;
        }
        if manager.is_none() {
            manager = Manager::RequestAsync()
                .and_then(|operation| operation.join())
                .ok();
            if manager.is_none() {
                thread::sleep(MANAGER_RETRY);
                continue;
            }
        }
        if let Some(manager) = &manager {
            poll(manager, &mut state);
        }
        thread::sleep(ACTIVE_POLL);
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_revision(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jlong {
    read_cache(|cache| cache.revision as jlong)
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_title(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    read_cache(|cache| {
        cache
            .metadata
            .as_ref()
            .map(|metadata| metadata.title.clone())
    })
    .and_then(|title| env.new_string(title).ok())
    .map(|value| value.into_raw())
    .unwrap_or(ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_artist(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    read_cache(|cache| {
        cache
            .metadata
            .as_ref()
            .map(|metadata| metadata.artist.clone())
    })
    .and_then(|artist| env.new_string(artist).ok())
    .map(|value| value.into_raw())
    .unwrap_or(ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_cover(
    env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jbyteArray {
    read_cache(|cache| {
        cache
            .metadata
            .as_ref()
            .and_then(|metadata| metadata.cover.clone())
    })
    .and_then(|cover| env.byte_array_from_slice(&cover).ok())
    .map(|value| value.into_raw())
    .unwrap_or(ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_positionMillis(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jlong {
    read_cache(|cache| {
        cache
            .timeline
            .as_ref()
            .map_or(0, |timeline| timeline.position_millis(Instant::now()))
    })
}

#[no_mangle]
pub extern "system" fn Java_dev_echo_client_media_MediaSession_durationMillis(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jlong {
    read_cache(|cache| cache.timeline.as_ref().map_or(0, Timeline::duration_millis))
}
