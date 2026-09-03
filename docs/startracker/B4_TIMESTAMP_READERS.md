# B4 — Every read of `SkyOrientation.timestampNanos`, and who the R2-A1(b) conflation fix breaks

Item R3-B4, 2026-09-04. **Report only — the proposed fix is NOT applied.**

## 1. Complete reader inventory (repo-wide, `app/src` main + test)

| # | Site | Kind | Verdict under R2-A1(b) semantics |
|---|------|------|----------------------------------|
| R1 | `ui/screens/CompassARScreen.kt:238-240` — `LaunchedEffect(skyOrientation.timestampNanos)` → `cameraFrameObserver.onSensorTimestamp(...)` (guarded `!= 0L`) | the only real read of the public field | **BROKEN on a stationary device** (staleness-inference reader; §2) |
| R2 | `ui/screens/CameraFrameObserver.kt:76` — debug log prints `sensorTsDelta = latestImageTimestampNanos - latestSensorTimestampNanos` | reads the observer's OWN volatile longs — but those longs are fed by R1 | **indirectly corrupted by R1's breakage** (§2) |
| R3 | `astro_engine/OrientationProvider.kt:207,248` — `updateQuaternion(..., timestampNanos)` | internal pass-through of a local/parameter, not a read of the public field | unaffected |
| R4 | tests | — none — `grep -rn timestampNanos app/src/test` returns nothing | unaffected |

No other reads exist (`timestampNanos` elsewhere in `app/src` is only the field definition, its
assignment at OrientationProvider.kt:409, `event.timestamp`/`imageInfo.timestamp` locals, and comments).

## 2. The breakage: stationary device + conflation = frozen sensor timestamp

R2-A1(b) deliberately EXCLUDED `timestampNanos` from `SkyOrientation.equals/hashCode`
(OrientationProvider.kt:25-33) so that `MutableStateFlow` conflation works again: a stationary
device keeps ticking the rotation-vector sensor (~SENSOR_DELAY_GAME rate), each tick emits a
content-identical `SkyOrientation`, and conflation drops it — no recomposition storm. Correct
and intended.

But that same conflation starves R1, which infers freshness FROM the timestamp:

1. Stationary device → sensor ticks → `_orientation.value = SkyOrientation(..., timestampNanos = t_n)`
   with content equal to the previous emission → **conflated away** → no state change.
2. No state change → no recomposition → `LaunchedEffect(skyOrientation.timestampNanos)` never
   relaunches → `cameraFrameObserver.onSensorTimestamp(...)` **stops being called**; the fed
   timestamp freezes at the last orientation *change*.
3. Consequence for R2 (the Phase 1 Task 3.4 "clock-domain cross-check"): in a flag-ON debug
   build the analyzer still receives frames, so `latestImageTimestampNanos` keeps advancing
   while `latestSensorTimestampNanos` is frozen → `sensorTsDelta` grows at wall-clock rate
   while the phone lies still, then snaps back at the first movement. The diagnostic reads as
   massive camera-vs-sensor clock drift exactly when the clocks are fine — i.e. it is
   meaningless after any stationary period, which is precisely when someone would eyeball it.

Severity today: **low but real** — the corrupted value is only printed in the flag-ON debug
analyzer (flag OFF: no frames, no log; B3 map §1), and R1's freeze has no functional effect
while the observer is inert. It becomes a live defect the moment PHASE6/7 wiring lands.

Note: the `!= 0L` guard in R1 handles the seed `SkyOrientation(0f, 45f, 0f)` correctly
(default `timestampNanos = 0L`); that part is fine.

## 3. Proposed fix (NOT applied)

Give the timestamp its own conflation-free channel instead of piggybacking on `SkyOrientation`:

```kotlin
// OrientationProvider — timestamps change every tick, so a Long StateFlow never conflates:
private val _sensorTimestampNanos = MutableStateFlow(0L)
val sensorTimestampNanos: StateFlow<Long> = _sensorTimestampNanos.asStateFlow()
// set in onSensorChanged() (OrientationProvider.kt:151, next to lastSensorTimestampNanos):
//     _sensorTimestampNanos.value = event.timestamp
```

```kotlin
// CompassARScreen — replace the LaunchedEffect(skyOrientation.timestampNanos) feed with:
LaunchedEffect(Unit) {
    orientationProvider.sensorTimestampNanos.collect {
        if (it != 0L) cameraFrameObserver.onSensorTimestamp(it)
    }
}
```

- `SkyOrientation.timestampNanos` stays as a plain field for one-shot consumers (per the
  R2-A1(b) comment) — equality semantics unchanged.
- Deliberately REJECTED alternative: re-adding `timestampNanos` to `equals/hashCode` —
  that reintroduces the original per-tick recomposition storm R2-A1(b) was filed against.
- Touches 2 files, ~8 lines, plus one regression test idea (feed N content-identical
  orientations → assert the timestamp flow emitted N times while the orientation flow
  conflated). Not applied this pass per the brief; parked for the real-Android-build batch
  alongside PHASE6/7 live wiring.
