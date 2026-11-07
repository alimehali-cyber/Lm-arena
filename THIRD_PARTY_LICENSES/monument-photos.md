# Monument pages — photographs

Five photographs ship with the two monument pages (`ui/monument/MonumentScreen.kt`), in
`app/src/main/res/drawable-nodpi/`. Four are Wikimedia Commons files with a verified free licence; the
fifth is flagged below and must be confirmed (or replaced) before a store release.

Each file was resized and, where noted, cropped for the layout. No other edit was made: no retouching,
no colour grading, no content removed. Every file is `.jpg` at quality 80–82, stripped of EXIF.

| App file | Bytes | Pixels | Source (Commons file) | Author | Licence |
|---|---|---|---|---|---|
| `img_monument_persepolis_hero.jpg` | 205,454 | 1080×1440 | `File:Sunset in Persepolis.jpg` | Aminhosseini722 | CC BY-SA 4.0 |
| `img_monument_persepolis_columns.jpg` | 80,910 | 720×1087 | `File:The stone columns of the Gate of All Nations, they were 16½ meters high and were topped with capitals in the form of a double bull, Persepolis, Iran (47779521811).jpg` | Carole Raddato | CC BY-SA 2.0 |
| `img_monument_persepolis_relief.jpg` | 311,360 | 1280×853 | `File:Medes and Persians at eastern stairs of the Apadana, Persepolis.JPG` | Bontenbal | CC BY-SA 3.0 |
| `img_monument_mil_hero.jpg` | 204,115 | 1000×1157 | `File:دیمه میل(میل اژدها)نورآباد ممسنی.png` (cropped above the in-image credit band) | uploader Mohamad2448; the photograph is stamped "Photo: Mohammadreza Atabak" | CC0 |
| `img_monument_mil_detail.jpg` | 255,767 | 1400×1050 | Commons upload "نورآباد روبروی رودی میل اژدها" (the tower in its valley) | see note below | **to be confirmed** |

## Licence texts

* CC BY-SA 4.0 — <https://creativecommons.org/licenses/by-sa/4.0/>
* CC BY-SA 3.0 — <https://creativecommons.org/licenses/by-sa/3.0/>
* CC BY-SA 2.0 — <https://creativecommons.org/licenses/by-sa/2.0/>
* CC0 1.0 — <https://creativecommons.org/publicdomain/zero/1.0/>

Attribution is carried **in the app**, on the page itself: every photograph has its caption and its
credit line beneath it (author, licence, source), and the closing "Photographs and sources" block
repeats the terms. Sharing a modified version of the CC BY-SA images (they are cropped and resized
here) therefore keeps the same licence, as those licences require.

## The one open item

`img_monument_mil_detail.jpg` was taken from a Commons-served file whose exact description-page title
could not be resolved during the build (the upload's filename uses characters that do not round-trip
through the search API). The image itself is freely served by `upload.wikimedia.org` and is the
standard photograph of the tower in its valley, but **its licence has not been verified line by line**.

Before a store release, either:

1. open the file's Commons description page — search Commons for `میل اژدها` and match the picture —
   and record its author and licence in the table above; or
2. replace it with `File:چشم انداز میل اژدها.jpg` or `File:نمای روبروی روی میل اژدها.jpg` (both
   Commons uploads of the same monument, ID 356), confirming their licences the same way.

Deleting the row and the `R.drawable.img_monument_mil_detail` reference is also safe: the Mil-e
Azhdaha page renders without that photograph, with one gap less.
