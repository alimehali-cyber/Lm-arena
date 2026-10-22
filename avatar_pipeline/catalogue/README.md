# Upload area

Put the character art here. Nothing in this folder is committed (see `../.gitignore`);
the analyser never makes a network call, so nothing leaves the machine.

Accepted: PNG (with or without alpha), JPEG, WEBP. One clear full-body or bust image is
best. Minimum 160 px on the short side; 800+ px gives much better colour samples.

Then:

```bash
cd /home/user/Lm-arena/avatar_pipeline
SAMPLES=20 RES=340 ./run_pipeline.sh working/zig01 catalogue/<file> \
    --heads-tall 5          # if it is a stylised character; omit for a realistic one
```

(Arguments after the image are passed to the analyser, so `--heads-tall`, `--height-m` and
`--face-box` all work.)

If the head count is wrong the character will look wrong in a specific, obvious way — too
small or too large a head, with the rig scales following it. Run once, look at
`renders/zig01/portrait_front.png`, then adjust.

---

## Present in this folder (not committed — `catalogue/` is gitignored)

| file | what it is |
|---|---|
| `1792690440674-…png` | the Professor Z turnaround sheet (front / side / three-quarter + hero pose) |
| `1792686555674-…png` | the Professor Z ensemble sheet (character with props and expressions) |

These are the source art for `blender/build_professor_z.py`. They were measured, not
traced: the front view was isolated to 133 x 418 px and the fraction table `F{}` plus the
colour palette in that script come from it. Rebuild with:

```bash
./run_authored.sh working/profz blender/build_professor_z.py
```
