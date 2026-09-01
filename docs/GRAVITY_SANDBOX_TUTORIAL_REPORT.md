# Gravity Sandbox — First-Launch Interactive Tutorial

**Forensic audit + implementation report**
Branch `arena/01a059b5-lm-arena` · verified run `33537286680`

Shares a build and a test suite with the camera/presets pass; see
`GRAVITY_SANDBOX_CAMERA_PRESETS_AUDIT.md` for the shared build and suite figures.

---

## A. Files changed

| File | Δ | What |
|---|---|---|
| `edu/TutorialContent.kt` | +139 (new) | 7 steps, bilingual, plus every button label and a11y string |
| `sim/TutorialStore.kt` | +50 (new) | `TutorialStore` port, `InMemoryTutorialStore`, `TutorialGate` |
| `ui/TutorialOverlay.kt` | +293 (new) | scrim, bottom card, step dots, gesture animation |
| `ui/GravitySandboxRoot.kt` | (shared) | `SharedPrefsTutorialStore`, auto-show gate, `?` button, Back precedence |
| `test/GravityCameraFollowTest.kt` | (shared) | 11 tutorial tests |

---

## B. Audit findings that shaped the design

- The sandbox **already owned a `SharedPreferences` file** (`zig_gravity_sandbox`, keys restored at
  `GravitySandboxRoot.kt:108` and saved on dispose at `:127`). There is no DataStore anywhere in
  `com/zig/gravity`. So the tutorial adds **one boolean to the existing file** rather than a second
  persistence mechanism.
- The top HUD row was: back circle → title/preset column → presets pill → challenges pill. The `?`
  goes **before the presets pill**, so it sits next to the title where help belongs and does not
  displace anything.
- The sandbox has **no second locale system** — it follows the app locale via `vm.persian`, which is
  exactly what the tutorial had to inherit rather than duplicate.

---

## C. Behaviour

**Seven steps** — welcome · camera · select · add · drag · time · discover. Under a minute end to
end. Deliberately not a slide deck: the scrim is light (0.42 dark / 0.24 light), the card sits at the
bottom, and **the simulation keeps running and stays visible behind it**, so every sentence points at
an interface the user can actually see.

- **Auto-show**: `TutorialGate.shouldAutoShow(store)` at first composition, once per install.
  "First launch" means *never completed or skipped*, not "first time this composable ran" —
  `tutorialChecked` is `rememberSaveable`, so a rotation does not retrigger it.
- **Skip** is always present, top-end, styled as secondary. Skip, Finish and Back all run the same
  `dismissTutorial`, which closes the overlay **and** marks it seen. There is no path that leaves the
  user having dismissed the tutorial only to be shown it again next launch.
- **Back** closes the tutorial before it leaves the sandbox (single `BackHandler`, tutorial checked
  first).
- **`?` button** (38 dp, tag `open_tutorial`, bilingual `contentDescription`) reopens it at any time.
  Reopening does not consult the gate and never clears the flag — the store's only mutation is
  one-way.
- **The speed step quotes `EngineConstants.SPEED_LABELS` at render time**, converted to Persian
  digits when Persian. No step hard-codes a rung, so the tutorial can never advertise a speed that
  does not exist — asserted by `theTutorialNeverHardCodesASpeedLadder`.
- **Gesture demos**: one `rememberInfiniteTransition` driving one float, drawn as circles on a
  `Canvas`. No image assets, no per-frame allocation, no new dependency.

### It cannot touch the simulation

`TutorialOverlay(persian: Boolean, onDismiss: () -> Unit)` takes **no `SimulationViewModel`**. It
cannot add a body, move one, change a mass, change the speed or advance the clock, because it holds
no reference with which to do so. That is enforced by the signature, not by discipline.

---

## D. Tests — 11

`aFreshInstallAutoShowsTheTutorial` · `skippingPersistsAndStopsTheTutorialAutoShowing` ·
`completingPersistsAndStopsTheTutorialAutoShowing` · `aStoreThatAlreadySawItNeverAutoShowsAgain` ·
`theSeenFlagIsNeverWrittenBackToFalse` · `theTutorialIsShortAndEveryStepIsBilingual` (also asserts
the Persian copy differs from the English and contains Persian characters, so a forgotten translation
fails the build) · `theTutorialCoversTheControlsItPromisesTo` ·
`theTutorialNeverHardCodesASpeedLadder` · `theTutorialOnlyAdvertisesBodyTypesThatExist` (every noun
in the add step maps to a real `BodyCatalog` entry) · `thereIsNoTutorialLocalLanguageState` ·
`tutorialStepsDeclareWhereTheyPoint` · `theTutorialCannotTouchTheSimulation`.

The gate logic is tested through `InMemoryTutorialStore`, a real implementation of the same
interface the production `SharedPrefsTutorialStore` implements — so the tested logic is the shipped
logic, with only the storage swapped.

---

## E. Limitations

1. **📱 The overlay itself has never been rendered.** No device, no emulator, no screenshots. Layout,
   legibility, the gesture animations, RTL mirroring of the step dots, and whether the card
   overlaps anything on a small screen are all **UNVERIFIED**.
2. **`SharedPrefsTutorialStore` is not unit-tested.** It needs an Android `Context`; this project has
   no Robolectric and no instrumentation tests. It is four lines over the existing preferences file,
   and the logic it feeds is fully tested — but the adapter itself is verified by reading only.
3. **The spotlight does not cut a hole.** `TutorialFocus` is declared per step and the copy names the
   control, but the scrim is uniform; there is no punched-out highlight around the real button.
4. **No first-run analytics**, so there is no evidence about where users actually drop out.
5. **Seven steps is a judgement, not a finding.** It is within the 5–7 the spec asked for, and the
   test enforces that band, but whether seven is one too many is not something a unit test can say.
