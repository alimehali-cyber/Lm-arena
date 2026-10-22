# Photo intake — what to ask for, and what to reject

Distilled from AssetFurnace `docs/prompt-guide.md`, tested by its author on Pixal3D by changing one
thing at a time, plus what our scene needs on top.

## The eight rules that decide the model

1. **Soft 3D-render look.** "Stylised 3D game character render, soft even lighting." Flat cel art has
   lines but no shading, so the model guesses the face and the paint lands slightly off it.
2. **A-pose, not T-pose.** Arms angled down about 45°, clear gap from the body. In a T-pose the hands
   sit where the model's space ends and fingers smear into claws or planks.
3. **Plain, relaxed hands.** Open, palms down. Say as little about fingers as possible; describing them
   makes them curl, and curled fingers come out merged.
4. **One subject, plain background, no props.** The background is cut out first; anything held or
   nearby is fused into the mesh.
5. **Eyes open, neutral face.** Closed or squinting eyes get carved in as slits.
6. **Realistic characters: slightly larger, clearly defined eyes** with visible eyelids. In a full-body
   shot a realistic face is small and the eyes come out as a shelf with a dark rim.
7. **Name what you want, not what you don't.** "No wrinkles" adds wrinkles. Describe empty hands
   rather than ruling out a weapon.
8. **Tidy hair in solid locks**, kept clear of face and shoulders, forehead clear, swept back or tied.
   Thin loose strands become paper-thin sheets that tear when the mesh is simplified.

Known failure modes, less proven: hoods/hats deep over the face (push them back), heavy ink outlines
(they get carved as grooves), strands across the face (stiff spikes).

## The shot list for our programme

| Shot | Required | Why |
|---|---|---|
| Front, full body, A-pose | **yes** | the only input the fallback route needs |
| 3/4 front, same pose | recommended | depth cues for the build, colour check |
| Side, same pose | recommended | nose, jaw, posture, foot length |
| Back | optional | AssetFurnace can draw one (`front_and_back`); a real one is better |
| Head crop, straight on, neutral | for `hero` detail | the head is rebuilt on its own from this |
| Full-length outfit detail | optional | pattern/trim the texture should keep |

Same session, same light, same clothes, no sunglasses, no hats over the brow, feet fully in frame,
camera at chest height (not looking down), long edge ≥ 1000 px.

## Rejection checklist

Ask for another photo when any of these is true:

- [ ] ankles or feet cut off, or the subject is smaller than 60 % of the frame height
- [ ] arms in a T-pose, or hands hidden behind the body
- [ ] fists, crossed arms, hands in pockets
- [ ] busy background, or a background the same value as the clothing
- [ ] hard flash from one side; deep shadow swallowing half the face
- [ ] eyes closed, squinting, or looking away
- [ ] more than 3/4 turn on any shot you expect to be used as "front"
- [ ] props, pets, furniture touching the subject
- [ ] heavy ink outline, flat cel shading only
- [ ] hair strands lying across the face
- [ ] under ~800 px on the long edge, or heavy JPEG artefacts on the face
- [ ] a group shot where the subject is not separable

## Rights and consent

Before building: confirm the photos are the user's own or they have permission to use them, and that
the person in them agrees to a 3D likeness being made. Record it in the provenance sidecar
(`"consent": "..."`, `"source_photos": [...]`). No photo, no avatar.

## Pre-processing we do (never the user)

- EXIF auto-orient, sRGB, alpha flattened onto white if the file is a PNG with transparency.
- Matte: flood-fill from the corners with a tolerance, then a 1 px feather and a 0.5 px erosion so the
  silhouette does not keep a halo of background.
- Measure the subject box from the matte, not from the image border.
- Palette: median colour of five regions (forehead/cheek, crown, chest, thigh, foot) after rejecting
  background-adjacent pixels.
- Everything is written to `brief.json` so the build is reproducible from the brief alone.
