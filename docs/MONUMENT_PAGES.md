# The two monuments: tap targets and pages

**Scope:** the full-bleed Home sky scene (`HeroSkyCanvas`, `SkyCanvasPresentation.SCENE`) and the two
pages it can open. The stone in that scene — the Apadana façade at Persepolis on the left, the
Mil-e Azhdaha tower on the right — is now something a finger can select, exactly like a star, and
the name it raises opens a full page about the place.

```bash
./gradlew :app:testDebugUnitTest --tests '*MonumentLandscapeHitTest*' --tests '*SkyCanvasInteractionRegressionTest*'
./gradlew :app:testDebugUnitTest --tests '*SkyUiPolishTest*' --tests '*ContentIntegrityAuditTest*'
```

---

## 1. Why the relief could not handle its own taps

The relief is a `TextureView` hosting a hand-owned EGL thread (`PersepolisTextureView`), and it is
drawn on top of the painted scene but *below* the interaction layer. It has no idea a finger exists:
its job is to render one frame per sky-model change. So the canvas has to answer "what did this tap
land on?" for the stone, the way it already answers it for the Sun, the Moon, the planets and the
catalogue stars.

It does not have to guess where the stone is. `PersepolisFraming` already publishes, for the exact
scene size, the projected column and row of every landmark: the great doorway, the tallest column,
the façade's bounds, the platform toe, the tower's slope and its ruined top. Those numbers place the
camera; they are equally good as hit boxes.

## 2. The boxes

`ui/monument/MonumentHitTargets.kt` — pure arithmetic, no Compose, unit-tested on the JVM.

| Box | Left | Right | Top | Bottom |
|---|---|---|---|---|
| Persepolis | `min(greatDoorwayX, facadeLeftX) − slop` | `max(tallestColumnX, facadeRightX) + slop` | `min(greatDoorwayTopY, tallestColumnTopY) − slop` | `toeY + 0.75 slop` |
| Mil-e Azhdaha | `min(milSlopeLeftX, towerCenterX − 48 px)` | `max(milSlopeRightX, left + 72 px)`, clipped to the width | `milTowerTopY − slop` | `milSlopeFrontY + 0.75 slop` |

`slop` is `HIT_SLOP_DP = 14dp` in pixels: the stone is thin, dark and low, and a fingertip is wider
than a column. Boxes are clipped to the canvas and dropped entirely when clipping leaves less than
12 px — a zero-width box would otherwise sit there swallowing taps meant for the sky. Where two boxes
overlap (only on a scene far narrower than any phone), the nearer centre wins.

**Two small boxes, never one full-size overlay.** A full-size hit layer would consume every tap
before the canvas's own `detectTapGestures` could identify a star, and it would break the
finger-time-travel gesture's start. The boxes are `Box`es with a `pointerInput` and no drawing at
all, composed after the river layer so they sit above both GL hosts in z-order.

## 3. The flow a tap produces

1. A tap on the stone sets the *existing* selection state — `SelectedCelestialInfo` — with
   `id = "monument_persepolis"` / `"monument_mil_e_azhdaha"`, the monument's own name, and a pin at
   the middle of the box's top edge. The pulse ring, the radial bloom and the four-second dismissal
   are the same code the stars use; nothing about the interaction is special-cased.
2. The name card (`SelectionCallout`) lifts over the stone with its wedge pointing at it, and shows
   the monument's name over a one-line kind ("UNESCO World Heritage", "Parthian national monument").
3. **Tapping the name** is the only way into the page. `onOpen` splits the id: a monument id calls
   `viewModel.openMonument(...)`, everything else keeps calling
   `viewModel.openObjectDetailById(...)`. A first tap therefore never navigates by accident.

`MainUiState.openMonument` holds the open page — ViewModel state, not composition state, for the same
reason `showSkyViewer` is: it is a full-screen window and a configuration change must not close it
mid-read. `MainActivity` renders it beside the other full-screen windows, so a tap from the Default,
Cyber or Spatial Home all open the same page.

## 4. The pages

`ui/monument/MonumentContent.kt` holds both languages of both pages; `MonumentScreen.kt` draws them.

The design is deliberately *not* the object modal, and deliberately not a stack of cards:

- a full-bleed photograph at the top (62 % of the screen height, cropped), with a two-stage scrim:
  black at the bottom for the title's ink, black at the top for the back control's ink;
- the title over the photograph, with an overline that says what the place *is*;
- a lede set at 18.5 sp with a 33 sp lead;
- **four figures separated by hairlines, on the theme's own background** — no card, no fill, no
  radius;
- numbered sections (`۰۱ … ۰۶`), each with a short rule above its number, a 22 sp heading and body
  text at 15 sp / 31 sp in the text margin. One section carries a full-bleed photograph with its
  caption and credit underneath;
- one quote, set large beside a 2 dp rule;
- a closing block about what the monument does in *this app's* night model — the tower's fires light
  below −2° of solar altitude; the scene is lit by the real Sun and Moon — and a quiet outlined
  button back to the sky;
- photographs' authors, licences and the historical sources in the smallest ink on the page.

The only accent is `accentGold` at low opacity on the section numbers and the quote's rule. No
second colour, no glass, no gradient panels: the photographs carry the colour.

## 5. Photographs

Five files in `app/src/main/res/drawable-nodpi/`, all Wikimedia Commons: `img_monument_persepolis_hero`,
`_columns`, `_relief`, `img_monument_mil_hero`, `img_monument_mil_detail`. Authors, licences, sizes
and the one photograph whose licence still needs confirming are recorded in
`THIRD_PARTY_LICENSES/monument-photos.md`, and the app itself repeats author + licence under every
image.

## 6. What this does **not** touch

- No catalogue object was created, so `AstroBitIdentityTest`'s baseline is unmoved.
- The relief's own rendering, framing, lighting and fires are unchanged.
- The object detail modal, the sky search and the fullscreen viewer are unchanged; the only edit to
  shared code is one branch in `SelectionCallout`'s `onOpen`.
