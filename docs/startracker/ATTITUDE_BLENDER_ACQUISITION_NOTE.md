# AttitudeBlender: lock-acquisition snap — decision note (NO code changed)

Owner decision required. Discovered in remediation pass 1 when the vacuous
AttitudeBlenderTest.testSequentialCallSmoothness was first executed against the real
blend math (the flag had made every prior run passthrough).

## What the code does today

When a star lock is acquired (FULL_LOCK, fresh) and later blended each frame:

- output = slerp(existingFused, starSolved, blendFraction) with
  blendFraction = FULL_LOCK_BLEND_FRACTION × stalenessFactor = **0.9 × 1.0 = 0.9** on
  the first fresh frame (MARGINAL_LOCK: 0.5).
- So on the acquisition frame the attitude **jumps by 0.9× the discrepancy** between
  the fused estimate and the star solution — measured in the test: a 10° discrepancy
  produces a **9.0° single-frame jump** (aging path afterwards is smooth: max 2.05°
  step through staleness decay down to passthrough).
- recommendedMagWeight similarly steps straight to the floor (0.1) in one frame.

## What the config comments say the intent is

`StarTrackerConfig.FULL_LOCK_BLEND_FRACTION = 0.9` is commented as "strong dominance…
1.0 = fully star, 0.0 = fully existing fused. Default 0.9 = 90% star, 10% existing
(strong dominance)" and marked UNVALIDATED. I.e. heavy immediate trust in the star
solution IS the documented design intent; nothing in the config contemplates a ramp.

## UX consequence

If the sensor-fused attitude has drifted while the star tracker was unlocked (gyro
drift accumulates; think tens of degrees over a long NO_LOCK period), the first
successful re-lock makes the sky overlay/AR layer **snap by ~90% of that error in one
frame**. Large visible lurch; also any consumer reacting to magWeight steps sees a
one-frame discontinuity. The test that encoded "no jump > 5°" as a blanket expectation
was reconciled in pass 1 to assert acquisition-at-designed-magnitude + smooth aging
path — the current green test documents the snap, it does not endorse it.

## Option 1 — KEEP the instant acquisition

- Pros: simplest; the jump is bounded by the *actual* disagreement, i.e. it is a
  correction toward the (assumed) truth; no extra state; honest signal that a lock
  happened; consistent with "strong dominance" intent; a ramp would display a known-
  wrong attitude longer.
- Cons: visible lurch proportional to accumulated drift; no UX polish.

## Option 2 — RAMP-IN over N frames (e.g. 200-500 ms)

- Mechanism: multiply blendFraction by an acquisition envelope α(k) rising 0→1 over N
  frames after FULL_LOCK first observed (per lock episode), e.g. smoothstep. Staleness
  decay continues to apply on top.
- Pros: no single-frame lurch; perceived quality during reacquisition.
- Cons: new state (frames-since-acquisition, episode boundary detection when lock
  flickers); during the ramp the displayed attitude is a blend of a possibly-drifted
  estimate and the star truth (known-wrong frames); interacts with MARGINAL↔FULL
  transitions and with the staleness-decay math (needs its own tests); delays the
  mag-weight reduction.

## Option 3 — keep the estimate snap, ease in the PRESENTATION layer (recorded future direction)

- Mechanism: AttitudeBlender keeps the instant 0.9x acquisition (estimate always jumps
  straight to the best available attitude); any visual easing is applied downstream in
  the overlay/UI layer (e.g. animate the rendered attitude toward the estimate over
  ~200-500 ms). The ESTIMATE therefore never carries known-wrong frames - only the
  display does, deliberately and visibly.
- Pros: correctness of the estimate is never sacrificed; UX polish lives where it
  belongs (rendering); ramp parameters become a UI concern, tunable without touching
  fusion math or its tests.
- Cons: presentation-layer easing is Compose/UI work - OUT OF SCOPE for the
  remediation passes (forbidden scope: Compose layout/styling/animation); consumers
  other than the animated overlay still see the step.
- Status: RECORDED as the future direction if the snap proves objectionable in field
  data (see decision below and the field-protocol logging line).

## DECISION

**DECISION: Option 1 - KEEP the instant acquisition. [2026-09-04, owner]**
No code, config, or test change. Item status: **PARKED, not closed** - the field-test
protocol now logs the acquisition discrepancy magnitude on every FULL_LOCK acquisition
(REAL_DEVICE_FIELD_TEST_PROTOCOL.md), so the keep-vs-ease question is decided from data
later; Option 3 (presentation-layer easing) is the recorded direction if the data says
the snap must be softened.
