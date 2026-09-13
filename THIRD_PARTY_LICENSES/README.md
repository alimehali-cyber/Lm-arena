# Third-party licences

Files in this directory are reproduced verbatim from the upstream projects they cover.

## Vazirmatn (Persian / Arabic typeface)

* Project: Vazirmatn by Saber Rastikerdar and contributors — `https://github.com/rastikerdar/vazirmatn`
* Release used: tag `v33.003` (font `name` table version string: `Version 33.003`)
* Licence: SIL Open Font License 1.1 — see `Vazirmatn-OFL.txt` (verbatim copy of upstream `OFL.txt`)
* Author list: see `Vazirmatn-AUTHORS.txt` (verbatim copy of upstream `AUTHORS.txt`)
* Files bundled, as *static instances* (not the variable-axis font), one per weight actually used by
  the RED type scale (Normal 400 / Medium 500 / SemiBold 600 / Bold 700). They are used for Persian
  text only; English text keeps the platform default font.
* Provenance: each file was fetched from the GitHub blob API for the tag above and verified by
  recomputing its git blob SHA-1 against `git ls-tree` for that tag, so the bytes are the upstream
  ones and not a repackaging.

| file | bytes | sha256 |
|---|---|---|
| `res/font/vazirmatn_regular.ttf` | 122,752 | `b69fd4c680b8f3f225feabcc655a2c585d97627b8f5f5c0f9985e894069f3a56` |
| `res/font/vazirmatn_medium.ttf` | 122,656 | `b986623e4ddef10755e04be39f8ea7bcb1dc08bfe8dd0aa6af395736f256ad4a` |
| `res/font/vazirmatn_semibold.ttf` | 122,920 | `3f239d8364c14ee32c46b6839fa197c28958358b82a9673d2c6acc8506d78bc5` |
| `res/font/vazirmatn_bold.ttf` | 123,036 | `f635fdbea28f265de395ba83b4b1570dcf2f58d13c65469e61903b1c2d2ae723` |

They are applied in `ui/theme/Type.kt` (`VazirmatnFontFamily`) and switched on per locale in
`ui/theme/Theme.kt` (`REDTheme(isPersian = ...)`).
