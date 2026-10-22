#!/usr/bin/env python3
"""
Stage 5: `24`-Clip NLA Animation Library Builder (`animate_character.py`).

Authors `24` smooth, cyclic/pose-clean NLA tracks simultaneously driving `AvatarRig` quaternions/locations
and `Avatar_Head` 26 shape-key channels (via Blender NLA tracks exported cleanly into glTF 2.0 `animations[]`):
  - `19` core mobile avatar clips (`Idle_Breathing`, `Idle_LookAround`, `Blink_Natural`, `Talk_Gesture_01`,
    `Talk_Gesture_02`, `Wave_Hello`, `Nod_Yes`, `Shake_No`, `Emote_Happy`, `Emote_Surprised`, `Emote_Thinking`,
    `Point_Sky_Night`, `Point_Horizon`, `Shade_Eyes_Day`, `Check_Phone_Twilight`, `Walk_Cycle`, `Turn_Left`,
    `Turn_Right`, `Sit_Stargaze`)
  - `LipSync_Speech` (audio/phoneme-driven co-articulated speech + conversational head micro-motion)
  - `4` Signature Professor Z Catalogue Clips:
    1. `ProfZ_HeroGlassesAdjust` — Hero pose: left hand raises smoothly to adjust his round wire glasses with proud smile!
    2. `ProfZ_DiscoGroove` — *"Even planets need a groove."* skyward right-index point + disco hip bounce!
    3. `ProfZ_Sleepy` — Heavy sleepy stargazer eyelids + gentle head bob!
    4. `ProfZ_TongueOutWink` — Playful right wink + 3D coral-pink tongue out + head tilt!
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys
from typing import Dict, List, Tuple

import bpy
from mathutils import Euler


BONE_ALIASES = {
    "Pelvis": "Hips",
    "Spine1": "Spine01",
    "Spine2": "Spine02",
    "LowerArm_L": "Forearm_L",
    "LowerArm_R": "Forearm_R",
    "Shin_L": "Calf_L",
    "Shin_R": "Calf_R",
}


class ClipBuilder:
    def __init__(self, rig: bpy.types.Object, head_obj: bpy.types.Object, name: str, frames: int):
        self.rig = rig
        self.head_obj = head_obj
        self.name = name
        self.frames = frames
        self.rig_action = bpy.data.actions.new(name=f"{name}_Rig")
        self.sk_action = bpy.data.actions.new(name=f"{name}_Morph")
        rig.animation_data_create()
        rig.animation_data.action = self.rig_action
        head_obj.data.shape_keys.animation_data_create()
        head_obj.data.shape_keys.animation_data.action = self.sk_action
        self.reset_pose()

    def resolve_bone(self, bname: str) -> str:
        return BONE_ALIASES.get(bname, bname)

    def reset_pose(self) -> None:
        for pb in self.rig.pose.bones:
            pb.rotation_mode = "XYZ"
            pb.location = (0.0, 0.0, 0.0)
            pb.rotation_euler = (0.0, 0.0, 0.0)
            pb.scale = (1.0, 1.0, 1.0)
        for kb in self.head_obj.data.shape_keys.key_blocks[1:]:
            kb.value = 0.0

    def rot_deg(self, bname: str, frame: int, rx: float, ry: float, rz: float) -> None:
        rb = self.rig.pose.bones[self.resolve_bone(bname)]
        rb.rotation_mode = "XYZ"
        rb.rotation_euler = Euler((math.radians(rx), math.radians(ry), math.radians(rz)), "XYZ")
        rb.keyframe_insert(data_path="rotation_euler", frame=frame)

    def loc(self, bname: str, frame: int, x: float, y: float, z: float) -> None:
        rb = self.rig.pose.bones[self.resolve_bone(bname)]
        rb.location = (x, y, z)
        rb.keyframe_insert(data_path="location", frame=frame)

    def morph(self, sk_name: str, frame: int, val: float) -> None:
        kb = self.head_obj.data.shape_keys.key_blocks[sk_name]
        kb.value = float(max(0.0, min(1.0, val)))
        kb.keyframe_insert(data_path="value", frame=frame)

    def bake_to_nla(self) -> None:
        # Ensure at least one keyframe exists on both rig & shape keys so glTF merges both into a single named clip!
        if not self.rig_action.fcurves:
            self.rot_deg("Head", 1, 0.0, 0.0, 0.0)
            self.rot_deg("Head", self.frames, 0.0, 0.0, 0.0)
        if not self.sk_action.fcurves:
            self.morph("mouthSmileLeft", 1, 0.0)
            self.morph("mouthSmileLeft", self.frames, 0.0)

        t_rig = self.rig.animation_data.nla_tracks.new()
        t_rig.name = self.name
        s1 = t_rig.strips.new(self.name, 1, self.rig_action)
        s1.name = self.name

        t_sk = self.head_obj.data.shape_keys.animation_data.nla_tracks.new()
        t_sk.name = self.name
        s2 = t_sk.strips.new(self.name, 1, self.sk_action)
        s2.name = self.name

        self.rig.animation_data.action = None
        self.head_obj.data.shape_keys.animation_data.action = None
        self.reset_pose()


def author_all_24_clips(rig: bpy.types.Object, head_obj: bpy.types.Object, lipsync_data: Dict) -> List[str]:
    bpy.context.scene.render.fps = 30
    built_names: List[str] = []

    def add_clip(name: str, frames: int, fn) -> None:
        cb = ClipBuilder(rig, head_obj, name, frames)
        fn(cb)
        cb.bake_to_nla()
        built_names.append(name)

    # 1. Idle_Breathing (calm natural breath + warm smile hint)
    def c_idle(cb: ClipBuilder):
        for f, ph in ((1, 0.0), (45, 1.0), (90, 0.0)):
            cb.rot_deg("Spine01", f, -1.8 * ph, 0.0, 0.8 * ph)
            cb.rot_deg("Chest", f, -2.2 * ph, 0.0, -0.6 * ph)
            cb.rot_deg("Head", f, 1.5 * ph, 1.2 * ph, -1.5 * ph)
            cb.rot_deg("UpperArm_L", f, 0.0, 2.0 * ph, 0.0)
            cb.rot_deg("UpperArm_R", f, 0.0, -2.0 * ph, 0.0)
            cb.morph("mouthSmileLeft", f, 0.16 + 0.06 * ph)
            cb.morph("mouthSmileRight", f, 0.12 + 0.06 * ph)
    add_clip("Idle_Breathing", 90, c_idle)

    # 2. Idle_LookAround
    def c_look(cb: ClipBuilder):
        for f, yaw, pitch in ((1, 0, 0), (25, 18, -4), (55, -18, -3), (90, 0, 0)):
            cb.rot_deg("Neck", f, pitch * 0.4, 0, yaw * 0.35)
            cb.rot_deg("Head", f, pitch * 0.6, 0, yaw * 0.65)
            cb.morph("browInnerUp", f, 0.35 if abs(yaw) > 10 else 0.0)
    add_clip("Idle_LookAround", 90, c_look)

    # 3. Blink_Natural
    def c_blink(cb: ClipBuilder):
        for f, v in ((1, 0.0), (10, 0.0), (14, 1.0), (16, 1.0), (22, 0.0), (40, 0.0)):
            cb.morph("eyeBlinkLeft", f, v)
            cb.morph("eyeBlinkRight", f, v)
    add_clip("Blink_Natural", 40, c_blink)

    # 4..5. Talk_Gesture_01 & Talk_Gesture_02
    def c_talk1(cb: ClipBuilder):
        for f, p in ((1, 0.0), (20, 1.0), (40, 0.4), (60, 0.0)):
            cb.rot_deg("UpperArm_R", f, 28 * p, 0, -10 * p)
            cb.rot_deg("Forearm_R", f, 42 * p, 0, 18 * p)
            cb.rot_deg("Head", f, -3 * p, 4 * p, 6 * p)
            cb.morph("mouthSmileLeft", f, 0.35 * p)
            cb.morph("mouthSmileRight", f, 0.35 * p)
            cb.morph("browInnerUp", f, 0.30 * p)
    add_clip("Talk_Gesture_01", 60, c_talk1)

    def c_talk2(cb: ClipBuilder):
        for f, p in ((1, 0.0), (22, 1.0), (42, 0.6), (60, 0.0)):
            cb.rot_deg("UpperArm_L", f, 26 * p, 0, 12 * p)
            cb.rot_deg("UpperArm_R", f, 24 * p, 0, -12 * p)
            cb.rot_deg("Forearm_L", f, 38 * p, 0, -15 * p)
            cb.rot_deg("Forearm_R", f, 38 * p, 0, 15 * p)
            cb.rot_deg("Head", f, -4 * p, -3 * p, -5 * p)
            cb.morph("browOuterUpLeft", f, 0.45 * p)
            cb.morph("browOuterUpRight", f, 0.45 * p)
    add_clip("Talk_Gesture_02", 60, c_talk2)

    # 6. Wave_Hello
    def c_wave(cb: ClipBuilder):
        for f, up, wv in ((1, 0.0, 0.0), (15, 1.0, 18.0), (25, 1.0, -18.0), (35, 1.0, 18.0), (45, 1.0, -14.0), (60, 0.0, 0.0)):
            cb.rot_deg("UpperArm_R", f, 35 * up, -68 * up, 0.0)
            cb.rot_deg("Forearm_R", f, 45 * up, wv, 0.0)
            cb.rot_deg("Head", f, -4 * up, 5 * up, 6 * up)
            cb.morph("mouthSmileLeft", f, 0.72 * up)
            cb.morph("mouthSmileRight", f, 0.72 * up)
            cb.morph("jawOpen", f, 0.28 * up)
    add_clip("Wave_Hello", 60, c_wave)

    # 7. Nod_Yes
    def c_nod(cb: ClipBuilder):
        for f, p in ((1, 0), (10, 14), (20, -5), (30, 12), (45, 0)):
            cb.rot_deg("Head", f, p, 0, 0)
            cb.morph("mouthSmileLeft", f, 0.45)
            cb.morph("mouthSmileRight", f, 0.45)
    add_clip("Nod_Yes", 45, c_nod)

    # 8. Shake_No
    def c_no(cb: ClipBuilder):
        for f, y in ((1, 0), (12, 18), (24, -18), (36, 12), (48, 0)):
            cb.rot_deg("Head", f, 0, 0, y)
            cb.morph("browInnerUp", f, 0.4)
    add_clip("Shake_No", 48, c_no)

    # 9. Emote_Happy (`Excited` in Professor Z Catalogue!)
    def c_happy(cb: ClipBuilder):
        for f, p in ((1, 0.0), (18, 1.0), (36, 1.0), (54, 0.0)):
            cb.rot_deg("Head", f, -5 * p, 4 * p, 4 * p)
            cb.morph("mouthSmileLeft", f, 0.95 * p)
            cb.morph("mouthSmileRight", f, 0.95 * p)
            cb.morph("jawOpen", f, 0.58 * p)
            cb.morph("eyeWideLeft", f, 0.45 * p)
            cb.morph("eyeWideRight", f, 0.45 * p)
            cb.morph("browOuterUpLeft", f, 0.65 * p)
            cb.morph("browOuterUpRight", f, 0.65 * p)
            cb.morph("cheekSquintLeft", f, 0.55 * p)
            cb.morph("cheekSquintRight", f, 0.55 * p)
    add_clip("Emote_Happy", 54, c_happy)

    # 10. Emote_Surprised (`Surprised` in Professor Z Catalogue!)
    def c_surp(cb: ClipBuilder):
        for f, p in ((1, 0.0), (14, 1.0), (34, 1.0), (50, 0.0)):
            cb.rot_deg("Head", f, -7 * p, 0, 0)
            cb.morph("eyeWideLeft", f, 0.95 * p)
            cb.morph("eyeWideRight", f, 0.95 * p)
            cb.morph("browInnerUp", f, 0.85 * p)
            cb.morph("browOuterUpLeft", f, 0.80 * p)
            cb.morph("browOuterUpRight", f, 0.80 * p)
            cb.morph("mouthFunnel", f, 0.78 * p)
            cb.morph("jawOpen", f, 0.72 * p)
    add_clip("Emote_Surprised", 50, c_surp)

    # 11. Emote_Thinking (`Thinking` in Professor Z Catalogue!)
    def c_think(cb: ClipBuilder):
        for f, p in ((1, 0.0), (20, 1.0), (45, 1.0), (65, 0.0)):
            cb.rot_deg("Head", f, 4 * p, -7 * p, 10 * p)
            cb.rot_deg("Eye_L", f, -6 * p, 0, 8 * p)
            cb.rot_deg("Eye_R", f, -6 * p, 0, 8 * p)
            cb.rot_deg("UpperArm_R", f, 36 * p, 10 * p, -18 * p)
            cb.rot_deg("Forearm_R", f, 68 * p, 0, 24 * p)
            cb.morph("browOuterUpLeft", f, 0.78 * p)
            cb.morph("browDownRight", f, 0.48 * p)
            cb.morph("mouthPucker", f, 0.42 * p)
            cb.morph("mouthSmileLeft", f, 0.25 * p)
    add_clip("Emote_Thinking", 65, c_think)

    # 12..15. ZIG Sky-Observer Poses (`Point_Sky_Night`, `Point_Horizon`, `Shade_Eyes_Day`, `Check_Phone_Twilight`)
    def c_sky(cb: ClipBuilder):
        for f, p in ((1, 0.0), (24, 1.0), (56, 1.0), (80, 0.0)):
            cb.rot_deg("Chest", f, -8 * p, 0, -6 * p)
            cb.rot_deg("Head", f, -22 * p, 4 * p, 12 * p)
            cb.rot_deg("UpperArm_L", f, 48 * p, 58 * p, 10 * p)
            cb.rot_deg("Forearm_L", f, 18 * p, 0, 0)
            cb.morph("mouthSmileLeft", f, 0.65 * p)
            cb.morph("mouthSmileRight", f, 0.65 * p)
            cb.morph("eyeWideLeft", f, 0.4 * p)
            cb.morph("eyeWideRight", f, 0.4 * p)
    add_clip("Point_Sky_Night", 80, c_sky)

    def c_horiz(cb: ClipBuilder):
        for f, p in ((1, 0.0), (22, 1.0), (50, 1.0), (72, 0.0)):
            cb.rot_deg("UpperArm_R", f, 62 * p, -18 * p, 0)
            cb.rot_deg("Head", f, -6 * p, 0, -14 * p)
            cb.morph("mouthSmileLeft", f, 0.4 * p)
    add_clip("Point_Horizon", 72, c_horiz)

    def c_shade(cb: ClipBuilder):
        for f, p in ((1, 0.0), (20, 1.0), (50, 1.0), (70, 0.0)):
            cb.rot_deg("UpperArm_R", f, 58 * p, -32 * p, -18 * p)
            cb.rot_deg("Forearm_R", f, 65 * p, 0, 32 * p)
            cb.rot_deg("Head", f, -8 * p, 0, -8 * p)
            cb.morph("eyeSquintLeft", f, 0.65 * p)
            cb.morph("eyeSquintRight", f, 0.65 * p)
    add_clip("Shade_Eyes_Day", 70, c_shade)

    def c_phone(cb: ClipBuilder):
        for f, p in ((1, 0.0), (20, 1.0), (50, 1.0), (70, 0.0)):
            cb.rot_deg("Neck", f, 12 * p, 0, 0)
            cb.rot_deg("Head", f, 16 * p, 0, 0)
            cb.rot_deg("UpperArm_R", f, 28 * p, 8 * p, -16 * p)
            cb.rot_deg("Forearm_R", f, 54 * p, 0, 22 * p)
            cb.morph("mouthSmileLeft", f, 0.25 * p)
    add_clip("Check_Phone_Twilight", 70, c_phone)

    # 16. Walk_Cycle (32-frame clean opposite-limb locomotion stride!)
    def c_walk(cb: ClipBuilder):
        for f in range(1, 33, 4):
            u = 2.0 * math.pi * ((f - 1) / 32.0)
            s, c = math.sin(u), math.cos(u)
            cb.loc("Hips", f, 0.0, 0.0, 0.018 * abs(c))
            cb.rot_deg("Hips", f, 0.0, 3.0 * s, -4.5 * s)
            cb.rot_deg("Chest", f, -2.0, -2.5 * s, 5.0 * s)
            cb.rot_deg("Thigh_L", f,  26.0 * s, 0.0, 0.0)
            cb.rot_deg("Thigh_R", f, -26.0 * s, 0.0, 0.0)
            cb.rot_deg("Calf_L", f, -28.0 * max(0.0, -c), 0.0, 0.0)
            cb.rot_deg("Calf_R", f, -28.0 * max(0.0,  c), 0.0, 0.0)
            cb.rot_deg("UpperArm_L", f, -22.0 * s, 0.0, 0.0)
            cb.rot_deg("UpperArm_R", f,  22.0 * s, 0.0, 0.0)
            cb.rot_deg("Forearm_L", f, 16.0 + 10.0 * max(0.0, -s), 0.0, 0.0)
            cb.rot_deg("Forearm_R", f, 16.0 + 10.0 * max(0.0,  s), 0.0, 0.0)
    add_clip("Walk_Cycle", 32, c_walk)

    # 17..19. Turn_Left, Turn_Right, Sit_Stargaze
    def c_turn_l(cb: ClipBuilder):
        for f, p in ((1, 0.0), (16, 1.0), (32, 0.0)):
            cb.rot_deg("Hips", f, 0, 0, 22 * p)
            cb.rot_deg("Head", f, 0, 0, 14 * p)
            cb.rot_deg("Thigh_L", f, 12 * p, 0, 8 * p)
    add_clip("Turn_Left", 32, c_turn_l)

    def c_turn_r(cb: ClipBuilder):
        for f, p in ((1, 0.0), (16, 1.0), (32, 0.0)):
            cb.rot_deg("Hips", f, 0, 0, -22 * p)
            cb.rot_deg("Head", f, 0, 0, -14 * p)
            cb.rot_deg("Thigh_R", f, 12 * p, 0, -8 * p)
    add_clip("Turn_Right", 32, c_turn_r)

    def c_sit(cb: ClipBuilder):
        for f, p in ((1, 0.0), (24, 1.0), (56, 1.0), (80, 0.0)):
            cb.loc("Hips", f, 0, 0.06 * p, -0.32 * p)
            cb.rot_deg("Thigh_L", f, 68 * p, 0, 0)
            cb.rot_deg("Thigh_R", f, 68 * p, 0, 0)
            cb.rot_deg("Calf_L", f, -66 * p, 0, 0)
            cb.rot_deg("Calf_R", f, -66 * p, 0, 0)
            cb.rot_deg("Head", f, -16 * p, 0, 8 * p)
            cb.morph("mouthSmileLeft", f, 0.5 * p)
            cb.morph("mouthSmileRight", f, 0.5 * p)
    add_clip("Sit_Stargaze", 80, c_sit)

    # 20. LipSync_Speech (co-articulated speech visemes + expressive conversational head motion!)
    cues = lipsync_data.get("mouthCues", [])
    dur = max(float(lipsync_data.get("metadata", {}).get("duration", 3.0)), 1.5)
    n_sp = max(int(round(dur * 30.0)), 45)
    vis_keys = ["viseme_PP", "viseme_FF", "viseme_EE", "viseme_aa", "mouthFunnel", "mouthPucker", "jawOpen", "tongueOut"]

    def c_speech(cb: ClipBuilder):
        for f in range(1, n_sp + 1, 2):
            t_sec = (f - 1) / 30.0
            active_w: Dict[str, float] = {}
            for c in cues:
                if float(c["start"]) <= t_sec <= float(c["end"]):
                    active_w = c.get("morphWeights", {})
                    break
            for vk in vis_keys:
                cb.morph(vk, f, float(active_w.get(vk, 0.0)))
            cb.rot_deg("Head", f, -3.0 * math.sin(t_sec * 3.1), 2.5 * math.cos(t_sec * 2.2), 7.5 * math.sin(t_sec * 1.9))
            cb.morph("browInnerUp", f, 0.25 + 0.20 * max(0.0, math.sin(t_sec * 4.0)))
    add_clip("LipSync_Speech", n_sp, c_speech)

    # 21. Signature Professor Z Clip #1: `ProfZ_HeroGlassesAdjust` (Hero Pose!)
    def c_hero(cb: ClipBuilder):
        for f, p in ((1, 0.0), (22, 1.0), (48, 1.0), (70, 0.0)):
            cb.rot_deg("Hips", f, 0.0, 2.5 * p, -6.0 * p)
            cb.rot_deg("Chest", f, -3.5 * p, -2.0 * p, 6.0 * p)
            cb.rot_deg("Head", f, -4.0 * p, 5.5 * p, -8.0 * p)
            # Raise left arm forward & up so fingers rest right against the outer gold rim of his round glasses!
            cb.rot_deg("UpperArm_L", f, -62.0 * p, 12.0 * p, -22.0 * p)
            cb.rot_deg("Forearm_L", f, -78.0 * p, 18.0 * p, -32.0 * p)
            cb.rot_deg("Hand_L", f, 10.0 * p, -12.0 * p, 0.0)
            # Relaxed pocket-ready right hand posture
            cb.rot_deg("UpperArm_R", f, -10.0 * p, 4.0 * p, 8.0 * p)
            cb.rot_deg("Forearm_R", f, -22.0 * p, 0.0, -10.0 * p)
            cb.morph("mouthSmileLeft", f, 0.75 * p)
            cb.morph("mouthSmileRight", f, 0.50 * p)
            cb.morph("jawOpen", f, 0.22 * p)
            cb.morph("browOuterUpLeft", f, 0.55 * p)
    add_clip("ProfZ_HeroGlassesAdjust", 70, c_hero)

    # 22. Signature Professor Z Clip #2: `ProfZ_DiscoGroove` (*"Even planets need a groove."*)
    def c_disco(cb: ClipBuilder):
        for f, p, groove in ((1, 0.0, 0.0), (18, 1.0, 1.0), (36, 1.0, -1.0), (54, 1.0, 1.0), (72, 0.0, 0.0)):
            cb.loc("Hips", f, 0.025 * groove * p, 0.0, -0.015 * abs(groove) * p)
            cb.rot_deg("Hips", f, 0.0, 5.0 * groove * p, 8.0 * groove * p)
            cb.rot_deg("Chest", f, -6.0 * p, -4.0 * groove * p, -7.0 * groove * p)
            cb.rot_deg("Head", f, -8.0 * p, 6.0 * groove * p, 8.0 * groove * p)
            # High skyward disco finger point!
            cb.rot_deg("UpperArm_L", f, 44.0 * p, 72.0 * p, 12.0 * p)
            cb.rot_deg("Forearm_L", f, 16.0 * p, 0.0, 0.0)
            cb.rot_deg("Middle1_L", f, 65.0 * p, 0.0, 0.0)
            cb.rot_deg("Ring1_L", f, 70.0 * p, 0.0, 0.0)
            cb.rot_deg("Pinky1_L", f, 72.0 * p, 0.0, 0.0)
            # Groovy bent right arm
            cb.rot_deg("UpperArm_R", f, 24.0 * p, -22.0 * p, -12.0 * p)
            cb.rot_deg("Forearm_R", f, 58.0 * p, 0.0, 22.0 * p)
            cb.morph("mouthSmileLeft", f, 0.90 * p)
            cb.morph("mouthSmileRight", f, 0.90 * p)
            cb.morph("jawOpen", f, 0.36 * p)
            cb.morph("eyeBlinkRight", f, 0.85 * max(0.0, groove) * p)
    add_clip("ProfZ_DiscoGroove", 72, c_disco)

    # 23. Signature Professor Z Clip #3: `ProfZ_Sleepy` (`Sleepy` Midnight Dreamer!)
    def c_sleepy(cb: ClipBuilder):
        for f, p in ((1, 0.0), (24, 1.0), (54, 1.0), (76, 0.0)):
            cb.rot_deg("Neck", f, 7.0 * p, 3.0 * p, 0.0)
            cb.rot_deg("Head", f, 10.0 * p, 6.0 * p, -5.0 * p)
            cb.morph("eyeBlinkLeft", f, 0.72 * p)
            cb.morph("eyeBlinkRight", f, 0.72 * p)
            cb.morph("browInnerUp", f, 0.45 * p)
            cb.morph("mouthFunnel", f, 0.28 * p)
    add_clip("ProfZ_Sleepy", 76, c_sleepy)

    # 24. Signature Professor Z Clip #4: `ProfZ_TongueOutWink` (`Wink` + `Tongue Out`!)
    def c_wink_tongue(cb: ClipBuilder):
        for f, p in ((1, 0.0), (16, 1.0), (42, 1.0), (60, 0.0)):
            cb.rot_deg("Head", f, -4.0 * p, -8.0 * p, 7.0 * p)
            cb.morph("eyeBlinkRight", f, 1.0 * p)
            cb.morph("cheekSquintRight", f, 0.75 * p)
            cb.morph("browOuterUpLeft", f, 0.70 * p)
            cb.morph("mouthSmileLeft", f, 0.85 * p)
            cb.morph("mouthSmileRight", f, 0.85 * p)
            cb.morph("tongueOut", f, 0.95 * p)
    add_clip("ProfZ_TongueOutWink", 60, c_wink_tongue)

    assert len(built_names) == 24
    return built_names


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :] if "--" in sys.argv else []
    ap = argparse.ArgumentParser()
    ap.add_argument("--in-blend", required=True)
    ap.add_argument("--lipsync", required=True)
    ap.add_argument("--out-blend", required=True)
    args = ap.parse_args(argv)

    bpy.ops.wm.open_mainfile(filepath=str(pathlib.Path(args.in_blend).resolve()))
    rig = bpy.data.objects["AvatarRig"]
    head_obj = bpy.data.objects["Avatar_Head"]
    lipsync_data = json.loads(pathlib.Path(args.lipsync).read_text())

    clips = author_all_24_clips(rig, head_obj, lipsync_data)

    out_p = pathlib.Path(args.out_blend).resolve()
    out_p.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out_p))
    print(json.dumps({"status": "character_animated", "clips_built": len(clips), "clips": clips}, indent=2))


if __name__ == "__main__":
    main()
