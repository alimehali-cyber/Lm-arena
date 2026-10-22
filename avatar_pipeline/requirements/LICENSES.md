# Third-Party Source Code, Binary, Pretrained Weight & Generated Asset License Ledger

Generated & audited automatically for commercial distribution inside **ZIG (`com.alijafari.red.astronomy`)**.

## 1. Installed Skill & Tool Repositories (Source Code)

| Repository | Pinned Commit SHA-1 | License | Commercial Use | Status |
| :--- | :--- | :--- | :---: | :--- |
| `EL4CTEO/blender-skills` | Recorded dynamically in `skills/REGISTRY.json` | **MIT** | Yes | Installed (`avatar_pipeline/skills/el4cteo`) |
| `ifBars/blender-agent-studio` | Recorded dynamically in `skills/REGISTRY.json` | **MIT** | Yes | Installed (`avatar_pipeline/skills/blender-agent-studio`) |
| `mdj128/facial-animation` | Recorded dynamically in `skills/REGISTRY.json` | **MIT** | Yes | Installed (`avatar_pipeline/skills/facial-animation`) |
| `RobLe3/cc-blender-skill` | Recorded dynamically in `skills/REGISTRY.json` | **MIT** | Yes | Installed (`avatar_pipeline/skills/cc-blender-skill`) |
| `grapeot/gpt_3d_skill` | `d35e84ab193258e802c690c42dc39d90324a1e52` | **None (All Rights Reserved default)** | **Blocked** | **Quarantined / Excluded from disk** |

## 2. Sandbox Execution Binaries & Libraries (Code vs Output License)

- **Blender `bpy == 4.2.0` (`GPL-2.0-or-later` build tool)**: Executed strictly offline on the build workstation to author clean `.blend`, `.png`, and `.glb` data assets. Per the official Blender Foundation FAQ, `.blend` files, rendered images, and exported `.glb` models are the sole copyright of the asset creator and carry **zero GPL viral copyleft obligation** inside the Android APK/AAB. No Blender code is compiled into or shipped with the mobile app.
- **Python Build Dependencies (`.venv`, unshipped offline tooling)**:
  - `numpy == 1.26.4` (`BSD-3-Clause`)
  - `scipy == 1.15.2` (`BSD-3-Clause`)
  - `Pillow == 11.1.0` (`HPND` / `MIT-CMU`)
  - `opencv-python-headless == 4.11.0.86` (`Apache-2.0`)
  - `mediapipe == 0.10.21` (`Apache-2.0`)
  - `trimesh == 4.6.4` (`MIT`)
  - `pygltflib == 1.16.3` (`MIT`)
  - `pocketsphinx == 5.1.1` (`BSD-2-Clause`)
  - `jdk4py == 21.0.8.0` (`GPL-2.0-with-classpath-exception`, build-time JVM verification only)
- **Android Runtime Prototype (`avatar_pipeline/android_prototype/`)**:
  - `100%` first-party pure Kotlin (`2.2.10`) targeting Android platform SDK `android.opengl.GLES30`, `android.opengl.EGL14`, and `android.view.TextureView` (`Apache-2.0`). Zero third-party native `.so` binaries, zero Vulkan code, zero NDK/C++.

## 3. Pretrained Model Weights Audit (Evaluated & Installed)

- **MediaPipe FaceMesh & Pose Landmarker (`mediapipe == 0.10.21`)**: Bundled `Apache-2.0` weights — commercially permitted for offline landmark extraction.
- **PocketSphinx Acoustic / Phoneme Models (`pocketsphinx == 5.1.1`)**: Bundled `BSD-2-Clause` weights — commercially permitted for offline speech alignment.
- **Basel Face Model (`cleardusk/3DDFA_V2` / BFM-2009)**: **Explicitly disabled & blocked** (`"allow_noncommercial_bfm": false` in `avatar_pipeline/config.json`) due to academic/non-commercial restrictions on BFM weights.

## 4. Input Character Artwork & Generated 3D Deliverables

- **Input Character Sheet**: User-supplied `Professor Z — Character Catalogue` (`avatar_pipeline/input/professor_z_catalogue.png`, `1536×1024`).
- **Generated 3D Meshes, procedural PBR Atlases (`1024×1024` & `512×512`), `60`-Bone Rig, `26` Morph Targets & `24` Animations**: `100%` clean-room procedurally synthesized from catalogue anthropometrics & swatches; zero third-party stock meshes, scans, or textures included.
