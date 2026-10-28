//! Pure cache/timing policy recovered from the Windows implementation.
use std::collections::hash_map::DefaultHasher;
use std::hash::{Hash, Hasher};
use std::time::{Duration, Instant};

pub(crate) const ACTIVE_WINDOW: Duration = Duration::from_secs(3);
pub(crate) const ACTIVE_POLL: Duration = Duration::from_millis(250);
pub(crate) const IDLE_POLL: Duration = Duration::from_millis(500);
pub(crate) const MANAGER_RETRY: Duration = Duration::from_secs(5);
pub(crate) const MAX_THUMBNAIL_BYTES: u64 = 16 * 1024 * 1024;
const TICKS_PER_MILLISECOND: f64 = 10_000.0;

#[derive(Clone, Debug, PartialEq, Eq)]
pub(crate) struct TrackKey {
    pub app_id: String,
    pub title: String,
    pub artist: String,
    pub album_title: String,
}

#[derive(Clone, Debug)]
pub(crate) struct Sample {
    pub key: TrackKey,
    pub start_ticks: i64,
    pub end_ticks: i64,
    pub position_ticks: i64,
    pub updated_ticks: i64,
    pub rate: f64,
    pub playing: bool,
}

impl Sample {
    pub fn duration_ms(&self) -> f64 {
        self.end_ticks.wrapping_sub(self.start_ticks).max(0) as f64 / TICKS_PER_MILLISECOND
    }

    pub fn timing_changed(&self, previous: &Self) -> bool {
        self.start_ticks != previous.start_ticks
            || self.end_ticks != previous.end_ticks
            || self.position_ticks != previous.position_ticks
            || self.updated_ticks != previous.updated_ticks
            || self.playing != previous.playing
            || self.rate != previous.rate
    }

    pub fn observed_position(
        &self,
        previous: Option<&Self>,
        metadata_changed: bool,
        now_ticks: i64,
    ) -> f64 {
        // A new track with unchanged position AND update timestamp must not inherit
        // the preceding track's stale timeline (worker RVA 0x9100..0x9270).
        if metadata_changed
            && previous.is_some_and(|old| {
                old.position_ticks == self.position_ticks && old.updated_ticks == self.updated_ticks
            })
        {
            return 0.0;
        }
        let unchanged_update = !metadata_changed
            && previous.is_some_and(|old| old.updated_ticks == self.updated_ticks);
        let elapsed_ticks = if self.updated_ticks <= 0 || unchanged_update {
            0
        } else {
            now_ticks.wrapping_sub(self.updated_ticks.min(now_ticks))
        };
        self.position_ticks.wrapping_sub(self.start_ticks) as f64 / TICKS_PER_MILLISECOND
            + if self.playing {
                elapsed_ticks as f64 / TICKS_PER_MILLISECOND * positive_rate(self.rate)
            } else {
                0.0
            }
    }
}

fn positive_rate(rate: f64) -> f64 {
    if rate.is_finite() && rate > 0.0 {
        rate
    } else {
        1.0
    }
}

fn bounded(position: f64, duration: f64) -> f64 {
    let position = position.max(0.0);
    if duration > 0.0 {
        position.min(duration)
    } else {
        position
    }
}

#[derive(Clone, Debug)]
pub(crate) struct Timeline {
    anchor: Instant,
    base_ms: f64,
    correction_ms: f64,
    duration_ms: f64,
    rate: f64,
    playing: bool,
}

impl Timeline {
    pub fn new(position: f64, duration: f64, rate: f64, playing: bool, now: Instant) -> Self {
        let duration = duration.max(0.0);
        Self {
            anchor: now,
            base_ms: bounded(position, duration),
            correction_ms: 0.0,
            duration_ms: duration,
            rate: positive_rate(rate),
            playing,
        }
    }

    pub fn position(&self, now: Instant) -> f64 {
        let position = if self.playing {
            let elapsed_ms = now.saturating_duration_since(self.anchor).as_secs_f64() * 1000.0;
            self.base_ms
                + elapsed_ms * self.rate
                + self.correction_ms.signum() * self.correction_ms.abs().min(elapsed_ms * 0.1)
        } else {
            self.base_ms
        };
        bounded(position, self.duration_ms)
    }

    pub fn position_millis(&self, now: Instant) -> i64 {
        self.position(now) as i64
    }
    pub fn duration_millis(&self) -> i64 {
        self.duration_ms as i64
    }

    pub fn update(&mut self, position: f64, duration: f64, rate: f64, playing: bool, now: Instant) {
        let mut current = self.position(now);
        let mut error = position - current;
        let duration = duration.max(0.0);
        // Exactly 1500 ms remains a smooth correction; strictly greater snaps.
        if error.abs() > 1500.0 {
            current = bounded(position, duration);
            error = 0.0;
        }
        *self = Self {
            anchor: now,
            base_ms: current,
            correction_ms: error,
            duration_ms: duration,
            rate: positive_rate(rate),
            playing,
        };
    }
}

#[derive(Clone, Debug)]
pub(crate) struct Metadata {
    pub title: String,
    pub artist: String,
    pub cover: Option<Vec<u8>>,
}

#[derive(Default)]
pub(crate) struct Cache {
    pub metadata: Option<Metadata>,
    pub timeline: Option<Timeline>,
    pub last_access: Option<Instant>,
    pub revision: u64,
}

impl Cache {
    pub fn active(&self, now: Instant) -> bool {
        self.last_access
            .is_some_and(|last| now.saturating_duration_since(last) < ACTIVE_WINDOW)
    }
    pub fn touch(&mut self) {
        self.last_access = Some(Instant::now());
    }
    pub fn bump(&mut self) {
        self.revision = self.revision.wrapping_add(1);
    }
}

#[derive(Default)]
pub(crate) struct WorkerState {
    pub previous: Option<Sample>,
    pub cover_hash: Option<u64>,
    pub cover_deadlines: Vec<Instant>,
}

impl WorkerState {
    pub fn clear(&mut self, cache: &mut Cache) {
        if self.previous.take().is_some() {
            self.cover_hash = None;
            self.cover_deadlines.clear();
            cache.metadata = None;
            cache.timeline = None;
            cache.bump();
        }
    }

    pub fn publish(
        &mut self,
        cache: &mut Cache,
        sample: Sample,
        cover: Option<Vec<u8>>,
        now_ticks: i64,
        now: Instant,
    ) {
        let metadata_changed = self
            .previous
            .as_ref()
            .is_none_or(|old| old.key != sample.key);
        let position =
            sample.observed_position(self.previous.as_ref(), metadata_changed, now_ticks);
        if metadata_changed {
            self.cover_hash = cover.as_deref().map(cover_hash);
            // Stored descending and popped from the end; read at +1, +3, +6 s.
            self.cover_deadlines = [6, 3, 1]
                .map(|seconds| now + Duration::from_secs(seconds))
                .to_vec();
            cache.metadata = Some(Metadata {
                title: sample.key.title.clone(),
                artist: sample.key.artist.clone(),
                cover,
            });
            cache.timeline = Some(Timeline::new(
                position,
                sample.duration_ms(),
                sample.rate,
                sample.playing,
                now,
            ));
            cache.bump();
        } else if self
            .previous
            .as_ref()
            .is_some_and(|old| sample.timing_changed(old))
        {
            if let Some(timeline) = &mut cache.timeline {
                timeline.update(
                    position,
                    sample.duration_ms(),
                    sample.rate,
                    sample.playing,
                    now,
                );
            }
        }
        self.previous = Some(sample);
    }

    pub fn thumbnail_due(&mut self, now: Instant) -> bool {
        if self
            .cover_deadlines
            .last()
            .is_some_and(|deadline| *deadline <= now)
        {
            self.cover_deadlines.pop();
            true
        } else {
            false
        }
    }

    pub fn publish_thumbnail(&mut self, cache: &mut Cache, bytes: Vec<u8>) {
        let hash = cover_hash(&bytes);
        if self.cover_hash == Some(hash) {
            return;
        }
        self.cover_hash = Some(hash);
        if let Some(metadata) = &mut cache.metadata {
            metadata.cover = Some(bytes);
            cache.bump();
        }
    }
}

fn cover_hash(bytes: &[u8]) -> u64 {
    // Original RVA 0x9a00: zero-key SipHash-1-3 over slice length then bytes.
    let mut hasher = DefaultHasher::new();
    bytes.hash(&mut hasher);
    hasher.finish()
}

/// Previous-playing, current-playing, first-playing, previous, current.
/// `previous` must already have been matched by app ID in the current session list.
pub(crate) fn choose_session<T: Clone>(
    previous: Option<T>,
    current: Option<T>,
    sessions: &[T],
    mut playing: impl FnMut(&T) -> bool,
) -> Option<T> {
    if let Some(session) = previous.as_ref().filter(|session| playing(session)) {
        return Some(session.clone());
    }
    if let Some(session) = current.as_ref().filter(|session| playing(session)) {
        return Some(session.clone());
    }
    if let Some(session) = sessions.iter().find(|session| playing(session)) {
        return Some(session.clone());
    }
    previous.or(current)
}

#[cfg(test)]
mod tests;
