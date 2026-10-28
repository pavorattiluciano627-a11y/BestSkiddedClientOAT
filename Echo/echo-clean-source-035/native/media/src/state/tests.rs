use super::*;

fn sample(title: &str) -> Sample {
    Sample {
        key: TrackKey {
            app_id: "test.app".into(),
            title: title.into(),
            artist: "Test artist".into(),
            album_title: "Test album".into(),
        },
        start_ticks: 0,
        end_ticks: 1_000_000_000,
        position_ticks: 100_000_000,
        updated_ticks: 200_000_000,
        rate: 1.0,
        playing: true,
    }
}

#[test]
fn selection_preserves_playing_previous_then_current_then_first_playing() {
    let sessions = [1, 2, 3];
    assert_eq!(
        choose_session(Some(1), Some(2), &sessions, |_| true),
        Some(1)
    );
    assert_eq!(
        choose_session(Some(1), Some(2), &sessions, |s| *s >= 2),
        Some(2)
    );
    assert_eq!(
        choose_session(Some(1), Some(2), &sessions, |s| *s == 3),
        Some(3)
    );
    assert_eq!(
        choose_session(Some(1), Some(2), &sessions, |_| false),
        Some(1)
    );
    assert_eq!(choose_session(None, Some(2), &sessions, |_| false), Some(2));
    assert_eq!(choose_session(None, None, &sessions, |_| false), None);
}

#[test]
fn unavailable_cache_has_no_fabricated_metadata_or_timeline() {
    let mut cache = Cache::default();
    let mut worker = WorkerState::default();
    assert!(cache.metadata.is_none());
    assert!(cache.timeline.is_none());
    assert_eq!(cache.revision, 0);
    worker.clear(&mut cache);
    assert_eq!(cache.revision, 0);
}

#[test]
fn worker_activity_boundary_and_cadences_match_original() {
    let now = Instant::now();
    let mut cache = Cache::default();
    assert!(!cache.active(now));
    cache.last_access = Some(now);
    assert!(cache.active(now + Duration::from_millis(2999)));
    assert!(!cache.active(now + Duration::from_secs(3)));
    cache.touch();
    assert!(cache.active(Instant::now()));
    assert_eq!(ACTIVE_POLL, Duration::from_millis(250));
    assert_eq!(IDLE_POLL, Duration::from_millis(500));
    assert_eq!(MANAGER_RETRY, Duration::from_secs(5));
    assert_eq!(MAX_THUMBNAIL_BYTES, 16_777_216);
}

#[test]
fn timing_uses_windows_ticks_and_ignores_future_or_repeated_timestamps() {
    let mut current = sample("A");
    current.start_ticks = 10_000_000;
    assert_eq!(current.duration_ms(), 99_000.0);
    assert_eq!(current.observed_position(None, true, 205_000_000), 9500.0);
    assert_eq!(current.observed_position(None, true, 100_000_000), 9000.0);
    assert_eq!(
        current.observed_position(Some(&current), false, 250_000_000),
        9000.0
    );
    assert_eq!(
        current.observed_position(Some(&current), true, 250_000_000),
        0.0
    );
    current.updated_ticks = 0;
    assert_eq!(current.observed_position(None, true, 250_000_000), 9000.0);
    current.playing = false;
    assert_eq!(current.observed_position(None, true, 250_000_000), 9000.0);
}

#[test]
fn correction_slews_at_one_tenth_elapsed_and_snaps_strictly_over_1500_ms() {
    let now = Instant::now();
    let mut timeline = Timeline::new(1000.0, 100_000.0, 1.0, true, now);
    timeline.update(2500.0, 100_000.0, 1.0, true, now);
    assert_eq!(timeline.position(now), 1000.0);
    assert_eq!(timeline.position(now + Duration::from_secs(1)), 2100.0);
    assert_eq!(timeline.position(now + Duration::from_secs(20)), 22500.0);
    let mut hard = Timeline::new(1000.0, 100_000.0, 1.0, true, now);
    hard.update(2500.01, 100_000.0, 1.0, true, now);
    assert_eq!(hard.position(now), 2500.01);
    let mut backwards = Timeline::new(5000.0, 100_000.0, 1.0, true, now);
    backwards.update(4500.0, 100_000.0, 1.0, true, now);
    assert_eq!(backwards.position(now + Duration::from_secs(1)), 5900.0);
}

#[test]
fn paused_timeline_does_not_advance_or_apply_small_pending_correction() {
    let now = Instant::now();
    let mut timeline = Timeline::new(5000.5, 6000.9, 2.0, false, now);
    timeline.update(5100.0, 6000.9, 0.0, false, now);
    assert_eq!(
        timeline.position_millis(now + Duration::from_secs(20)),
        5000
    );
    assert_eq!(timeline.duration_millis(), 6000);
}

#[test]
fn invalid_rates_fall_back_to_one_and_positions_clamp_to_duration() {
    let now = Instant::now();
    for rate in [0.0, -1.0, f64::NAN, f64::INFINITY, f64::NEG_INFINITY] {
        let timeline = Timeline::new(-2.0, 1500.0, rate, true, now);
        assert_eq!(timeline.position(now + Duration::from_secs(1)), 1000.0);
        assert_eq!(timeline.position(now + Duration::from_secs(2)), 1500.0);
    }
    let rate = f64::from_bits(1);
    assert_eq!(positive_rate(rate), rate);
    let timeline = Timeline::new(1000.0, 0.0, 2.0, true, now);
    assert_eq!(timeline.position(now + Duration::from_secs(2)), 5000.0);
}

#[test]
fn metadata_changes_and_covers_bump_revision_but_timeline_updates_do_not() {
    let now = Instant::now();
    let mut cache = Cache::default();
    let mut worker = WorkerState::default();
    let first = sample("A");
    worker.publish(&mut cache, first.clone(), None, first.updated_ticks, now);
    assert_eq!(cache.revision, 1);
    let metadata = cache.metadata.as_ref().unwrap();
    assert_eq!(metadata.title, "A");
    assert_eq!(metadata.artist, "Test artist");
    assert!(metadata.cover.is_none());
    let mut next = first.clone();
    next.position_ticks += 1_000_000;
    worker.publish(&mut cache, next, None, first.updated_ticks, now);
    assert_eq!(cache.revision, 1);
    worker.publish_thumbnail(&mut cache, vec![1, 2, 3]);
    assert_eq!(cache.revision, 2);
    worker.publish_thumbnail(&mut cache, vec![1, 2, 3]);
    assert_eq!(cache.revision, 2);
    worker.publish_thumbnail(&mut cache, vec![]);
    assert_eq!(cache.revision, 3);
    assert_eq!(cache.metadata.as_ref().unwrap().cover, Some(Vec::new()));
    let mut changed = first.clone();
    changed.key.album_title = "Second album".into();
    worker.publish(&mut cache, changed, None, first.updated_ticks, now);
    assert_eq!(cache.revision, 4);
    assert!(cache.metadata.as_ref().unwrap().cover.is_none());
    worker.clear(&mut cache);
    assert_eq!(cache.revision, 5);
    assert!(cache.metadata.is_none() && cache.timeline.is_none());
    worker.clear(&mut cache);
    assert_eq!(cache.revision, 5);
}

#[test]
fn thumbnail_retries_are_one_three_six_seconds_and_one_per_poll() {
    let now = Instant::now();
    let mut cache = Cache::default();
    let mut worker = WorkerState::default();
    let current = sample("A");
    worker.publish(
        &mut cache,
        current.clone(),
        Some(vec![1]),
        current.updated_ticks,
        now,
    );
    assert!(!worker.thumbnail_due(now + Duration::from_millis(999)));
    assert!(worker.thumbnail_due(now + Duration::from_secs(1)));
    assert!(!worker.thumbnail_due(now + Duration::from_secs(2)));
    assert!(worker.thumbnail_due(now + Duration::from_secs(3)));
    assert!(worker.thumbnail_due(now + Duration::from_secs(6)));
    assert!(!worker.thumbnail_due(now + Duration::from_secs(7)));
    worker.clear(&mut cache);
    worker.publish(
        &mut cache,
        current.clone(),
        None,
        current.updated_ticks,
        now,
    );
    assert!(worker.thumbnail_due(now + Duration::from_secs(10)));
    assert_eq!(worker.cover_deadlines.len(), 2);
}

#[test]
fn stale_timeline_is_zeroed_when_track_changes_and_revision_wraps() {
    let now = Instant::now();
    let mut cache = Cache::default();
    let mut worker = WorkerState::default();
    let first = sample("A");
    worker.publish(&mut cache, first.clone(), None, first.updated_ticks, now);
    cache.revision = u64::MAX;
    let mut second = first.clone();
    second.key.title = "B".into();
    worker.publish(&mut cache, second, None, first.updated_ticks, now);
    assert_eq!(cache.revision, 0);
    assert_eq!(cache.timeline.as_ref().unwrap().position_millis(now), 0);
}
