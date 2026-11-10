# Aurora — Liquid Prism

The fourth selectable interface language, and the newest: `AppUiStyle.AURORA`.

Where **Default** is restrained flat-and-high-contrast ZIG, **Spatial** frosts
visionOS glass, and **Cyber** cuts Nothing-OS crystal, **Aurora** treats every
surface as a *refractive lens* floating in a living aurora. Light bends through
it, disperses at its rim, and pools in its belly.

It is written against the Astryx design specifications rather than against taste.
Every numbered rule quoted below is a requirement that record states, so the
system can be reviewed instead of merely liked.

---

## 1. The field

`AuroraFieldBackground` draws three curtains of light drifting across a
deterministic starfield, on one shared clock
(`AuroraMotion.AMBIENT_MS`, 26 s per cycle).

- **Motion DR1** — motion carries meaning. The drift is the only motion here that
  decorates rather than explains, so it is the slowest thing in the system and it
  never moves content.
- **Motion DR5 / reduced motion** — every animated change has an immediate form.
  Under a reduced-motion preference the phase is *pinned* at a composed position
  rather than the field being removed.
- **Elevation DR2** — the field is the reference plane. It is quieter than every
  surface on top of it and never borrows their rim or shadow.

Intensity follows the Sun: the curtains all but vanish in daylight, so a daytime
screen gets a calm gradient with the stars switched off rather than a night sky
at noon.

---

## 2. The lens

`AuroraLiquidSurface` is the one surface every Aurora screen is built from. It is
painted in five passes, each doing one job:

| Pass | What it is | What it is for |
| ---- | ---------- | -------------- |
| 1 | Body gradient | the glass itself |
| 2 | Top refraction | light bending as it enters |
| 3 | Belly caustic | light pooling as it leaves |
| 4 | Dispersion rim | the prism edge — spectral on emphasis |
| 5 | Travelling specular | the proof that it is glass and not paper |

A sixth pass composites the press overlay *over* all of it rather than swapping
the fill, so a surface keeps its identity while it is being touched
(**color-emphasis DR5**), and a disabled surface paints none of it
(**user-states DR2**). A selected surface keeps its raise for the whole gesture
(**user-states DR3**).

There is no blur in the recipe. Refraction is faked with gradients, which is both
cheaper and sharper than a real blur on a phone.

### Tiers

`AuroraSurfaceTier` chooses fill, rim and shadow as *one* decision, because
**elevation-hierarchy DR3** says a surface leads with either an edge or a
shadow — never both at full strength.

| Tier | Role | Shadow |
| ---- | ---- | ------ |
| `CANVAS` | pure layout, no paint | — |
| `INSET` | a well: a viewport, a captured frame | none (recedes) |
| `CARD` | the default tile | 2 dp |
| `RAISED` | the dock, a sheet | 10 dp |
| `OVERLAY` | modals and dialogs | 22 dp |

---

## 3. Geometry

Radii follow role and nest concentrically (**shape-relationships DR1–DR2**):

```
container  34 dp   page regions, the floating dock
card       26 dp   tiles, panels, list rows
control    18 dp   buttons, fields, segmented rails
chip       12 dp   badges, and anything nested inside a control
```

`AuroraRadius.inner(outer, gap)` returns `outer − gap` (floored at 4 dp).
Subtracting the gap is what keeps two curves parallel to the eye; that is the
whole of DR2, and it is why the scale is a function and not four constants.

---

## 4. Space

Four tiers, and nothing between them (**spatial-hierarchy DR2, DR4**):

```
local    6 dp    a label to its value, an icon to its text
group   14 dp    peer controls inside one group
section 24 dp    distinct concerns: card to card
page    40 dp    the screen edge, and the gap between page regions
```

Grouping is expressed by proximity *before* containment (DR3): the metrics row is
two tiles 14 dp apart with no card drawn around them, and the screen's own title
is ink on the field rather than a panel that repeats it.

---

## 5. Type

Seven roles, each visibly a step from its neighbour
(**typography-hierarchy DR1–DR2**):

| Role | Size / line | Weight |
| ---- | ----------- | ------ |
| `display` | 34 / 40 | SemiBold, −0.6 tracking |
| `title` | 24 / 30 | SemiBold |
| `section` | 17 / 22 | SemiBold |
| `body` | 15 / 22 | Normal |
| `label` | 13 / 18 | Medium |
| `caption` | 11.5 / 16 | Medium |
| `data` | 15 / 20 | SemiBold, +0.2 tracking |

`eyebrow` (11 sp at 1.1 sp tracking, uppercase) is the quietest label there is and
sits only above a section. `data` carries readings, coordinates and times with
tight tracking so a column of numbers does not shimmer as the values change.

---

## 6. Motion

Weight decides timing (**motion DR2**), and easing settles rather than bounces
(DR3):

| Tier | Duration | Used by |
| ---- | -------- | ------- |
| `MICRO` | 120 ms | press, toggle, a value snapping in |
| `LOCAL` | 220 ms | a chip, a section expanding, the dock underline |
| `ENTRANCE` | 420 ms | a screen or a modal arriving |
| `AMBIENT` | 26 000 ms | the field's own drift |
| `SPECULAR` | 9 000 ms | the highlight crossing a lens |

A press snaps the specular to the touch rather than waiting for the cycle, so the
feedback is immediate. Under reduced motion every duration above collapses to 0
and the end state is shown at once.

---

## 7. State

**System states.** A status is a category, not a colour. `AuroraStatus` has five
members and `AuroraStatusPill` always draws hue **+ glyph + label** together
(DR2). `AuroraSkeleton` holds the geometry of what it stands in for (DR5) and
stops its sheen under reduced motion (DR8) — the wait stays legible either way.

**User states.** Press paints the manipulated surface and is transient (DR1);
disabled never responds (DR2); a selected dock tab keeps its raise (DR3).

**Colour.** Each of the four status hues is distinct and none of them is the
canvas. `AuroraUiTest` pins that, because "colour plus icon plus label" silently
degrades to "colour" the moment two hues converge.

---

## 8. Emphasis

The spectral gradient appears in exactly two places: the **primary action** of a
group and the **selected dock tab's underline**. Everything else is a wash or ink.
That is **color-emphasis DR3** enforced by construction — there is one component
that can wear it, so a group that uses it has named its one emphasised action.

---

## 9. What ships

| File | Contents |
| ---- | -------- |
| `AuroraDesignSystem.kt` | palette (dark / light / wine), provider, geometry, motion, type, the lens |
| `AuroraField.kt` | the aurora field and the compact in-card vignette |
| `AuroraComponents.kt` | status, chips, rails, actions, readouts, disclosure, skeleton, empty |
| `AuroraNavigationBar.kt` | the Prism Dock |
| `AuroraHomeScreen.kt` | hero sky, Moon viewport, tonight, brightest, next eclipse |
| `AuroraLabScreen.kt` | the three tools |
| `AuroraSatellitesScreen.kt` | live positions and the next pass |
| `AuroraSettingsModal.kt` | every setting the other three styles expose |

The provider retunes `RedTheme.colors` as well as publishing its own palette, so
any shared dialog nobody has redrawn yet still reads as part of the style instead
of as a foreign object dropped into it. That is the same contract the Cyber and
Spatial providers honour.

---

## 10. Parity

Aurora exposes every setting the other styles expose: interface style, language,
calendar, theme, sky canvas, backdrop and Bortle. `AuroraUiTest` fails if any of
them disappears, because a style that quietly drops a setting is a style that
quietly takes the choice away.
