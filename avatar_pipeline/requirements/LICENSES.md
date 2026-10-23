# Third-Party Licenses, Code vs. Model Weights Audit & Legal Ledger

Every repository, binary runtime, Python library, pretrained weight package, and generated 3D/texture asset evaluated or used in `avatar_pipeline/` is recorded separately below with **source code license separated strictly from pretrained model weight / dataset license**.

---

## 1. Installed Skill & Tool Repositories (Source Code)

| Repository | Inspected Commit SHA | Source Code License | Role in Pipeline | Redistribution / Notice Obligations |
|---|---|---|---|---|
| **`Bingeljell/image-to-3dlab`** (*AssetFurnace* `v0.5.1`) | `1a5093571075abce4d19cbd3f2c6fbc0faa9d6f9` (`2026-10-22`) | **Apache-2.0** (root) + **MIT** (`hunyuan_mlx` / `SkinTokens` subtrees) | Active CPU geometry fairing (`surface_calm.taubin`), winding check (`photo_paint.outward_sign`), multi-view projection (`photo_paint.paint_texture`, `replace_base_colour`), `KHR_materials_variants` (`glb_skins.with_variants`), and `glb_stats.file_stats` (`3,512` upstream `pytest` tests verified PASS on CPU) | Retain Apache-2.0 `LICENSE` & `NOTICE` (including repository attribution link) on source/binary redistribution. |
| **`microsoft/TRELLIS.2`** | `75fbf0183001ed9876c8dbb35de6b68552ee08bd` (`2026-06-05`) | **MIT** | Audited source candidate (`O-Voxel` 4B DiT); **blocked in this sandbox** by `>=24 GB` CUDA GPU requirement, compiled CUDA extensions (`flash-attn`, `nvdiffrast`, `nvdiffrec`, `CuMesh`, `FlexGEMM`, `o-voxel`), `3.8 GiB` host RAM, and `huggingface.co` TLS block | MIT copyright notice if redistributed. |
| **`Stability-AI/stable-fast-3d`** | `ff21fc491b4dc5314bf6734c7c0dabd86b5f5bb2` (`2025-01-22`) | **Stability AI Community License** | Audited source candidate; **rejected & blocked** in commercial mode due to `<$1M USD` annual revenue cap + registration requirement, gated `huggingface.co` weights (`TLS EOF`), and CUDA/MPS C++ `texture_baker` | Excluded from active execution (`strict_commercial_licenses_only = true`). |
| **`VAST-AI-Research/TripoSR`** | `107cefdc244c39106fa830359024f6a2f1c78871` (`2026-06-04`) | **MIT** | Audited source candidate (`--device cpu` supported in code); **blocked from weight download** in sandbox (`huggingface.co` `TLS EOF`) and produces unrigged single-mesh marching-cubes output without multi-view turnaround ingestion | MIT copyright notice if redistributed. |
| Installed Agent Skills (`avatar_pipeline/skills/REGISTRY.json`) | Verified pinned MIT commits | **MIT** (`100%` of installed skills; unlicensed repos quarantined) | Blender BMesh, rigging, and glTF QA workflows | MIT copyright notice. |

---

## 2. Sandbox Execution Binaries & Libraries (Code vs Output License)

| Package / Binary | Exact Pinned Version | Code License | License of Generated `.blend` / `.glb` / `.png` Outputs |
|---|---|---|---|
| **Blender Headless Python Module (`bpy`)** | `4.2.0` (`Cycles` CPU + `OpenImageDenoise`) | **GPL-3.0-or-later** (tool runtime only; zero Blender code linked into ZIG APK) | **100% Unencumbered Project-Owned Artwork/Data** (Blender Foundation output rule: `.blend`, `.glb`, and rendered `.png` files carry zero GPL copyleft). |
| **Python Runtime** | `3.11.2` (`Linux 6.1.158+ x86_64`) | PSF-2.0 | Unencumbered |
| **NumPy / SciPy / scikit-image** | `numpy==1.26.4`, `scipy==1.14.1`, `scikit-image==0.25.2` | BSD-3-Clause | Unencumbered |
| **OpenCV Headless / Pillow / trimesh / pygltflib** | `opencv-python-headless==4.11.0.86`, `pillow==11.3.0`, `trimesh==4.8.3`, `pygltflib==1.16.5` | Apache-2.0 / HPND / MIT / BSD-3-Clause | Unencumbered |
| **ZIG Android Runtime (`app/src/main/java/com/alijafari/red/astronomy/avatar/*.kt`)** | Pure Kotlin `2.2.10` + Android SDK `GLES30` | Project-owned first-party Kotlin source (zero third-party native engines, zero Vulkan, zero NDK/C++) | 100% Commercial-Safe |

---

## 3. Pretrained Model Weights Audit (Evaluated & Installed)

| Weight / Motion Asset | Source / Upstream Repo | Weight License | Status in Sandbox & ZIG Export |
|---|---|---|---|
| **MediaPipe Bundled TFLite Landmarkers (`face_landmark_with_attention.tflite`, `pose_landmark_full.tflite`)** | `mediapipe==0.10.21` offline Python wheel | **Apache-2.0** | **Active locally offline** (face & pose landmark analysis only; zero weights shipped in APK). |
| **NVIDIA Kimodo Motion Clips (`image_to_3dlab/motions/*.npz`)** | `Bingeljell/image-to-3dlab` (`NOTICE`) | **NVIDIA Open Model License** | Audited locally on CPU (`(150, 77, 4)` quaternions); all 24 exported glTF clips in `professor_z.glb` are clean first-party procedural keyframe & phoneme curves (`100%` unencumbered). |
| **Basel Face Model (`BFM 2009 / 2019`)** | University of Basel | **Non-Commercial Research Only** | **STRICTLY DISABLED (`allow_noncommercial_bfm = false`)** — never downloaded or used. |
| **`microsoft/TRELLIS.2-4B` + `facebook/dinov3-vitl16-pretrain-lvd1689m`** | Hugging Face Hub | MIT (`TRELLIS.2-4B`) + **Meta DINOv3 License / Gated** | **Blocked in sandbox** (`huggingface.co` TLS EOF + `>=24 GB` CUDA GPU requirement). |
| **`stabilityai/stable-fast-3d`** | Hugging Face Hub (Gated) | **Stability AI Community License (`<$1M` revenue cap)** | **Blocked & Excluded** (`huggingface.co` TLS EOF + non-unrestricted commercial license). |
| **`stabilityai/TripoSR`** | Hugging Face Hub | **MIT** | **Blocked from download in sandbox** (`huggingface.co` TLS EOF; cache hook supported offline if pre-staged under `avatar_pipeline/models/triposr/`). |

---

## 4. Reference Input Privacy & Provenance Guarantee

- **Supplied Reference Sheets**:
  - `avatar_pipeline/input/ref_01_professor_z_lineup.png` (`sha256: 19a055b164d683257fcc66005fa57ea221cd4347f92f788de977a4998870b143`)
  - `avatar_pipeline/input/ref_02_professor_z_catalogue.png` (`sha256: 098d1902057d18a9f346438d5f5bb3378b7a21f49894c72d01d6acd35be6b797`)
- **100% Local Processing**: Neither reference image was ever uploaded, transmitted, encoded into an external request, or sent to any third-party/cloud API. Original files remain byte-for-byte untouched.

## Iteration-3 additions (2026-10-23)

| Component | License | Notes |
|---|---|---|
| `ForJiang/image-to-splat` @ `05d10fc34056a825192c88cc243c9a592f5d0ff1` (ONNX packaging of Depth-Anything-V2-Small) | MIT | Only `model_fp16.onnx` is fetched via git sparse checkout; weights remain gitignored locally |
| Depth Anything V2 model weights (upstream `DepthAnything/Depth-Anything-V2`) | Apache-2.0 | Inference-only local usage; not redistributed in this repository |
| `onnxruntime==1.31.0` | MIT | CPUExecutionProvider only |
| `pymeshlab==2025.7.post1` | GPL-2.0-or-later (isolated) | Attempted for smoothing; NOT imported by any committed pipeline file (replaced by Taubin via `trimesh` + `fast-simplification`), NOT shipped in the APK |
| `fast-simplification==0.2.0` | MIT | QEM decimation used in v8/v9 mesh pipeline (offline tooling only) |
| `trimesh==4.6.4` | MIT | Mesh assembly/repair |
