# Field Trial Protocol (one page) — G-4.6

**Build:** debug only (`./gradlew assembleDebug`). The guide does not exist in release
builds (CI dex-proves absence of every `fieldtrial` class).
**Device:** any phone with rear camera + GPS + compass, Android 8+ (PixelCopy needs 8.0).
**Best time:** evening with a clear horizon, starting ~30 min before the levels that need
stars (the guide says which are "Not now" and when they unlock).

## Before you start (2 min)
1. Install the debug build; open the app once and grant **camera + location**.
2. Go outside, away from buildings and streetlights if possible.
3. Open the sky/AR screen (the one with the camera picture). A red **Field Test** button
   sits above the bottom bar — press it. The guide card appears at the bottom.
4. Never look straight at the Sun. Everything is done by looking at the PHONE screen.

## Running the trial (~15 min)
Follow the card. It names ONE thing at a time; when unsure press **?** (bottom sheet),
**Details** shows the raw numbers, **Skip** offers one-tap reasons (nothing to type).

| Level | You do | It measures |
|---|---|---|
| 0 Get ready | wave phone in a figure-8 | GPS fix, compass correction, camera info |
| 1 Find the Sun | tap the Sun in the picture | Sun offset (daylight check; can jump to Moon) |
| 2 Find the Moon | tap the Moon (or Jupiter/Saturn) | solar-system object offset |
| 3 A bright star | centre the ring, tap the star | star offset mid-screen |
| 4 Star at the edge | move star to screen edge, tap again | error growth toward the edges |
| 5 Seven stars | tap each named star | the seven previously-wrong stars |
| 6 Credits | check the app's About/credits | HYG credit line (expected **No** today) |
| 7 Southern sky | face north, answer Yes/Right or No | horizon flip (southern hemisphere) |
| 8 Lock on | press **Turn on**, hold phone still on stars | time to first star lock (60 s budget) |
| 9 Is it better? | tap the star wearing TWO rings | compass ring vs tracker ring distance |
| 10 Cover the camera | hand over lens → lit wall → ground, 20 s each | tracker must stay honestly "no lock" |
| 11 Slow sweep | slowly move across the sky 30 s | false locks while moving |
| 12 Finish | press **Share results** | zip: trial.json + summary.md + shots/ + frames/ |

If the Sun/Moon/star is not visible, use **Skip → Can't find it / Clouds** — the record
notes it; nothing is lost. Levels you cannot do (e.g. southern check at northern
latitude) show the reason and are marked automatically.

## Sharing the results
At Level 12 press **Share results** and send the zip anywhere (email/Drive/WhatsApp).
The zip contains: `trial.json` (every measurement, machine-readable), `summary.md`
(human summary with ticks/crosses), `shots/` (evidence pictures: target ring, tap
crosshair, offset arrow), `frames/` (raw grayscale frames captured at first lock and
whenever a covered camera was falsely claimed). You can also start a new trial — each
trial is kept separately, attempts are never overwritten.

## If something breaks
Screenshot the card (volume-down + power), note the level number, and send both.
The trial file on the device (`files/fieldtrial/trial-*.json`) survives reboots; reopening
the Field Test button resumes exactly where you were.
