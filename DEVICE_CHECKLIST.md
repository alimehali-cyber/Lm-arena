# Satellite redesign · device review checklist

These checks are **pending**. No emulator or physical device was available for this implementation. Record device, Android/API, density, locale, font scale, build SHA and result for each run; screenshots of the stub are not device evidence.

- [ ] Launch on a narrow phone, a larger phone and tablet; inspect Dark, Light and Wine for Default, Spatial and Aether, with English and Persian at 100%, 115% and 130% text.
- [ ] Confirm status/navigation insets, 48 dp targets, focus-card/nav clearance and globe/recenter keep-outs on the actual native globe, including with cutouts and gesture navigation.
- [ ] Rotate, pinch, double-tap blank space, select/cycle overlapping satellites and tap blank space to dismiss the label. Check physical direction, orbit alignment, horizon occlusion and contextual Recenter during and after gestures.
- [ ] Open/close Catalogue, search, filters, collections, reorder and editor; restart and verify DataStore persistence and first-run hints. Check TalkBack actions, focus order, focus restoration, RTL semantics and contrast.
- [ ] Open Passes, vary window and visibility, select a pass, set/change/remove a reminder, turn automatic monitoring on/off, leave and return. Verify notification permission and deep-link routing against the correct satellite.
- [ ] Scrub past/future time, check Live/Sim announcements and Earth illumination, dismiss/reopen without resetting time; check reduced-motion and animation-disabled settings.
- [ ] Change manual and GPS observer location, toggle connectivity, cold-start offline and verify TLE age/source, unknown last-fix behavior and empty/error states.
- [ ] Inspect all five Detail examples, page and fullscreen media interactions once genuinely licensed imagery is admitted; verify captions, source links, crop, cache behavior and the vector-only fallback.
- [ ] Inspect the actual frame rate, memory use, cancellation and battery impact of catalogue propagation, long-window pass scanning, backdrop sampling and starlink train overlays.

The Shape manifest currently flags all references as unverified. Obtain independent spacecraft-reference and native-speaker reviews before considering the artwork or Persian copy approved.
