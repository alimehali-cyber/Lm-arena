# Preset sheet scrolling · App icon forensic audit · ZIG launcher icon & APK rename

Branch `arena/01a059b5-lm-arena` · verified run **`33541907689`** (`tests=179 failures=0`, `BUILD SUCCESSFUL in 5m 33s`)

---

## 1 · The preset sheet did not scroll

### What was wrong

`PresetSheet.kt` rendered the scene list in a bare `Column`:

```kotlin
Column(modifier = Modifier.fillMaxWidth().padding(...)) {
    for (p in Preset.entries) { /* one row each */ }
}
```

A `Column` has no scroll container. Anything past the bottom edge is not clipped-but-reachable — it is
simply never laid out anywhere the user can touch.

**This is my regression.** The sheet held nine scenes and just fitted on a tall phone. The camera/presets
pass took the catalogue to **fifteen**, and the last six became unreachable. It would also have failed
earlier than that on a short screen, in landscape, or at a large system font scale.

### The fix

`verticalScroll(rememberScrollState())` on the content column, with the modifier ordered so the bottom
inset scrolls *with* the content:

```kotlin
.fillMaxWidth()
.verticalScroll(rememberScrollState())
.padding(horizontal = 20.dp)
.navigationBarsPadding()
.padding(bottom = 16.dp)
```

Order matters: `navigationBarsPadding()` applied *before* the scroll modifier sits outside the viewport,
and the final row stays trapped under the system navigation bar. After it, the last row scrolls clear.

### Two more sheets had the same shape

Auditing all four `ModalBottomSheet` call sites in `com.zig.gravity.ui`:

| Sheet | Before | Action |
|---|---|---|
| `PresetSheet` | plain `Column`, 15 rows | **broken** — fixed |
| `AddBodySheet` | `LazyVerticalGrid(heightIn max 380.dp)` inside a plain `Column` | grid scrolled, but the header and the trailing scale note could be pushed off a short screen — outer scroll added |
| `ChallengeSheet` (in `TeachingCard.kt`) | `LazyColumn(heightIn max 420.dp)` inside a plain `Column` | same — outer scroll added |
| `InspectorSheet` | already scrolled | unchanged |

### Regression guard — 5 new tests (`GravitySheetScrollTest`)

This project has **no Compose UI test harness and no Robolectric**, so there is no way from the JVM to lay
a sheet out and discover its last row is off-screen. These are therefore **source-level lint checks, not
behavioural tests**, and the file says so in its own header:

- `everyBottomSheetContentIsInAScrollContainer` — all four sheets must contain a scroll container.
- `theSceneListItselfScrolls` — the modifier must be on the `Column` that actually wraps the preset loop,
  not merely present somewhere in the file.
- `scrollableSheetsKeepTheirBottomInsetInsideTheScrollableArea` — asserts the modifier *ordering* above.
- `theSceneListIsLongEnoughToNeedScrolling` — documents why the guard exists.
- `everySceneInTheCatalogueRendersARowInTheSheet` — the sheet enumerates `Preset.entries`, so a new preset
  cannot be added to the engine and forgotten in the UI.

📱 **That the sheet now physically scrolls under a finger is NOT verified** — no device, no emulator.

---

## 2 · App icons and logos — what was actually wrong

### The finding

**Every PNG in the project was corrupt.** Not the icon, not some icons — all of them.

```
file                                    size    U+FFFD runs   % of bytes
mipmap-mdpi/ic_launcher.png            12919          2910        67.6%
mipmap-hdpi/ic_launcher.png            17714          3993        67.6%
mipmap-xhdpi/ic_launcher.png           24573          5628        68.7%
mipmap-xxhdpi/ic_launcher.png          42993          9763        68.1%
mipmap-xxxhdpi/ic_launcher.png         69336         15733        68.1%
  ... and all five *_round.png, identically
drawable/ic_launcher_fg.png           414866         95127        68.8%
drawable/red_app_logo.png            1261715        289839        68.9%
drawable/red_app_logo_display.png    1261715        289839        68.9%
drawable/img_splash_screen.jpg       1294493        292644        67.8%   (JPEG, unreferenced)
drawable/nebula_texture.jpg          1951779        452915        69.6%   (JPEG, unreferenced)
```

### The exact mechanism

A PNG begins with the magic bytes `89 50 4E 47`. Every corrupt file begins with `EF BF BD 50 4E 47`.

`EF BF BD` is the UTF-8 encoding of **U+FFFD REPLACEMENT CHARACTER**. The byte `0x89` is not valid UTF-8
on its own, so a decoder that read these files **as text** replaced it with U+FFFD, and re-encoding wrote
back three bytes where one had been. That happened to *every* byte in the file that was not valid UTF-8 —
about 68% of the content, consistently across all eleven files.

**This is irreversible.** The original byte values were not encoded or escaped; they were discarded and
replaced with a single sentinel. Nothing can be recovered from these files, which is why previous attempts
to reuse the existing artwork could not have worked.

The surviving JPEGs (`img_app_icon_*`, `red_logo_icon_*`, `zig_brand_concepts_*`, …) begin with a correct
`FF D8 FF E0`, so they reached the repository through a different, binary-safe path. The corruption came
from one specific tooling route, not from Git.

### Why nobody noticed at build time

Three things hid it:

1. **`assembleDebug` never validated them.** AGP disables PNG crunching for debuggable build types, so
   AAPT2 copied the files through without parsing them. The build was green the whole time the icon was
   broken. *(Corollary: a green build is still not proof that a PNG is valid — the proof in this report is
   an actual decode with `identify`.)*
2. **`release { isCrunchPngs = false }`** is set in `app/build.gradle.kts`. That flag is almost certainly a
   workaround for this exact bug: crunching a release build would have parsed the PNGs and failed. I have
   **left the flag alone** — now that the PNGs are valid it could be re-enabled to shrink the APK, but that
   changes release-build behaviour I cannot test here.
3. **`SafeAppLogo.kt` swallowed the in-app symptom.** It calls `ContextCompat.getDrawable` in a
   `try/catch (Throwable)` and silently substitutes a generic `Icons.Default.Stars` when decoding fails.
   So the app has been showing a **generic star** where the ZIG logo should be, with no error anywhere.

### What the user actually saw

| Surface | Path | Result |
|---|---|---|
| Launcher, API 26+ | `ic_launcher.xml` → `@drawable/ic_launcher_foreground` (layer-list) → `@drawable/red_app_logo` **(corrupt)** | foreground fails to decode — blank icon over the background vector |
| Launcher, API 24–25 | `mipmap-*/ic_launcher.png` **(all corrupt)** | broken icon |
| Round icon | `ic_launcher_round` — same two paths | broken |
| Themed icon (13+) | `<monochrome>` pointed at the same broken foreground | broken |
| In-app logo | `SafeAppLogo` → `red_app_logo_display` **(corrupt)** | silently replaced by a generic star |

The launcher **label** was never broken: `app_name` is already `ZIG` / `زیگ`.

---

## 3 · The new ZIG launcher icon

Built from the uploaded master, now stored as **`assets/brand/zig_logo_master.png`** (1254×1254, verified
`89 50 4E 47`, decodes cleanly) rather than left at the repository root under its upload name.

### Composition

The Z does not sit at the centre of the source image — it is low and slightly left. Everything is therefore
centred on **the Z's own bounding box**, not the image's, and the padding uses the logo's own backdrop
colour `#F7F6F7` so the join is invisible.

- **Adaptive foreground** (108dp): Z height = **58.1%** of the canvas, inside the 66/108 safe zone with a
  little breathing room. Verified against the safe circle and the mask square.
- **Adaptive background**: flat `#F7F6F7` vector — the same colour as the foreground's backdrop, so the
  launcher can cut any mask shape without exposing a seam.
- **Legacy icons**: Z at 68% (legacy icons carry their own shape, so they want less padding), rounded-rect
  for `ic_launcher`, true circle for `ic_launcher_round`.
- **Monochrome** for Android 13+ themed icons: this is new — it previously pointed at the broken foreground.
  A brightness threshold cannot separate the glyph from its drop shadow, but the Z's body is distinctly
  blue-tinted (B−R = +26) while the shadow is neutral (B−R = 0), so the silhouette is traced from
  **chroma**, then hole-filled (the clouds and exhaust inside the Z are neutral white) and smoothed.

### Files

`mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` × `{ic_launcher, ic_launcher_round, ic_launcher_foreground,
ic_launcher_monochrome}.png` — 20 files, all newly generated and all verified decodable.
Plus `drawable/red_app_logo.png` and `red_app_logo_display.png` regenerated at 512×512 (which also drops
2.4 MB of corrupt bytes from the APK, and makes `SafeAppLogo` show the real logo instead of its fallback).

Deleted: `drawable/ic_launcher_foreground.xml` (the layer-list that pointed at the corrupt PNG) and
`drawable/ic_launcher_fg.png` (corrupt, unreferenced).

A rendered preview across all four launcher mask shapes plus the themed variant, at both large and 48 px
sizes, is at **`docs/assets/zig_launcher_icon_preview.png`**.

### Verification

Every generated file was decoded with `identify`, and alpha coverage was probed per pixel — centre opaque,
corner transparent — after an initial pass came out inverted (ImageMagick 6's `CopyOpacity` semantics).
The mask polarity bug was caught by measurement, not by assumption.

📱 **Not verified:** how the icon looks on a real launcher, on a real wallpaper, under a real theme engine.

---

## 4 · APK renamed to ZIG

```kotlin
base { archivesName.set("ZIG") }
```

`rootProject.name` was already `ZIG`, but the artefact is named after the Gradle *module* (`:app`), which is
why it was `app-debug.apk`.

**Verified from the CI build log**, not inferred:

```
Generated APKs:
app/build/outputs/apk/debug/ZIG-debug.apk
```

Release builds will produce `ZIG-release.apk`. The workflow uploads via the glob `**/*.apk`, so the rename
does not break artifact upload.

---

## 5 · Remaining, honest

1. **Two corrupt JPEGs remain**: `img_splash_screen.jpg` and `nebula_texture.jpg` (3.2 MB of unrecoverable
   bytes). Both are **referenced nowhere** in the project. I left them rather than delete assets outside
   the scope of this request — say the word and they go.
2. **`release { isCrunchPngs = false }` left as found.** Now safe to re-enable; untested here.
3. **No device.** The scrolling, the icon on a launcher, and the in-app logo are all unverified by
   observation. The icon *artwork* was verified by rendering it here; its *installation* was not.
4. **The monochrome silhouette is auto-traced**, not hand-drawn. It is clean and clearly a Z, but a
   designer redrawing it as a vector would get crisper curves.
5. **`SafeAppLogo`'s silent fallback is still silent.** It will now succeed, but if a logo resource ever
   breaks again it will quietly show a star rather than tell anyone. I did not change that behaviour.
