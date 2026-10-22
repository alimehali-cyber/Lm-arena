# Professor Z — Local Reference-to-3D Character Reconstruction & Mobile GLB Pipeline

Deterministic, 100% local, privacy-preserving headless 3D character reconstruction, rigging, facial blendshape authoring, multi-clip animation, Cycles+OIDN visual QA rendering, and OpenGL ES 3.0 mobile `.glb` export pipeline built for **ZIG (`com.alijafari.red.astronomy`)**.

## Directory Structure

```text
avatar_pipeline/
├── README.md                                  # Setup, CLI usage, architecture & honest trade-offs
├── config.json                                # Active budgets, render settings & commercial license guards
├── config/pipeline_config.json                # Full pinned environment & candidate ranking config mirror
├── input/                                     # Untouched SHA-256 verified reference sheets & audio fixtures
│   ├── ref_01_professor_z_lineup.png          # Original supplied astronomy lineup sheet (untouched)
│   └── ref_02_professor_z_catalogue.png       # Original supplied character turnaround sheet (untouched)
├── models/
│   ├── MODEL_MANIFEST.json                    # Audited repos, commits, licenses & sandbox hardware status
│   └── repos/                                 # Cloned upstream source trees (`image-to-3dlab`, `TRELLIS.2`, `stable-fast-3d`, `TripoSR`)
├── working/                                   # Stage checkpoints, segmented views & untouched initial source GLB
│   └── professor_z/initial_visual_hull_source.glb
├── blender/                                   # Headless `bpy==4.2.0` character builder, 60-bone rig, 24 clips, Cycles renderer & LOD exporter
├── renders/                                   # Neutral studio review plates (`01..10`), cinematic hero (`11`), & REF-vs-3D sheet (`12`)
├── exports/                                   # Separate Master (`professor_z_master.glb`, `.blend`) & Mobile (`professor_z.glb`, `_lod1.glb`)
├── reports/                                   # Empirical baseline, 4-repo audit, forensic QA report & JSON manifests
├── requirements/LICENSES.md                   # Separate source code vs. model weight license ledger
├── tools/                                     # `create_avatar.py`, `reconstruct.py`, `rhubarb_bridge.py`, `validate_assets.py`, `_x11_stubs/`
└── tests/test_pipeline.py                     # End-to-end automated verification & safeguard regression suite
```

## One-Command Reproducible Execution (100% Offline After Setup)

```bash
# 1. Bootstrap Python 3.11 + bpy==4.2.0 + X11 headless stubs (idempotent)
bash avatar_pipeline/tools/bootstrap_env.sh

# 2. Run or resume the complete 8-stage Professor Z pipeline
.venv/bin/python avatar_pipeline/tools/create_avatar.py \
  --input avatar_pipeline/input/ref_02_professor_z_catalogue.png \
  --phonemes avatar_pipeline/input/professor_z_persian_phonemes.json \
  --lang fa \
  --name professor_z \
  --resume

# 3. Reconstruct any new single portrait or full-body photo cleanly with a single flag change
.venv/bin/python avatar_pipeline/tools/create_avatar.py \
  --input path/to/new_person.jpg \
  --audio avatar_pipeline/input/synthetic_speech_en.wav \
  --lang en \
  --name custom_avatar

# 4. Run full automated QA, license, ZIG safeguard & live Kotlin runtime test suite
.venv/bin/python -m unittest discover -s avatar_pipeline/tests -v
```
