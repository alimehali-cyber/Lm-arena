#!/usr/bin/env python3
"""
Stage 6 - the animation library: 21 named clips at 24 fps.

Craft is encoded in the timing rather than in the poses:
  anticipation   every action starts with a counter-pose, so the eye reads the wind-up
  overlap        chest lags the pelvis by 2 frames, head by 4, hands by 6-8
  asymmetry      a breath is 40% inhale / 60% exhale; smiles lead on one side
  moving holds   no pose is ever fully static; the hold drifts instead of freezing
  determinism    every "random" offset comes from a seeded random.Random

Blender 5 note: `Action.fcurves` does not exist any more. A slotted action stores its
curves under layers -> strips -> channelbags, and ONE action can hold both a bone slot
and a shape-key slot. That last part is what lets a clip keep its body and face
channels together through the glTF export instead of being split into two half-clips.

Run: .venv/bin/python blender/animate_character.py --blend working/x/character.blend \
        --out working/x/character_anim.blend --report working/x/06_anim.json
"""
from __future__ import annotations
import argparse, json, math, pathlib, random, sys
import bpy
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from scene_utils import reset_pose

FPS = 24


# --------------------------------------------------------------------------- helpers
def action_fcurves(act):
    """Blender 4.x kept curves on the action; 5.x nests them two levels deeper."""
    if hasattr(act, "fcurves"):
        return list(act.fcurves)
    out = []
    for layer in act.layers:
        for strip in layer.strips:
            for bag in strip.channelbags:
                out.extend(bag.fcurves)
    return out


def ensure_action(arm, name):
    """Reuse by name so the bone action and the shape-key action can be one action."""
    if arm.animation_data is None:
        arm.animation_data_create()
    act = bpy.data.actions.get(name)
    if act is None:
        act = bpy.data.actions.new(name)
        act.use_fake_user = True
    arm.animation_data.action = act
    return act


def key_bone(arm, bone, frame, rot=None, loc=None, scale=None):
    pb = arm.pose.bones.get(bone)
    if pb is None:
        return False
    pb.rotation_mode = "XYZ"
    if rot is not None:
        pb.rotation_euler = [math.radians(v) for v in rot]
        pb.keyframe_insert("rotation_euler", frame=frame)
    if loc is not None:
        pb.location = loc
        pb.keyframe_insert("location", frame=frame)
    if scale is not None:
        pb.scale = scale
        pb.keyframe_insert("scale", frame=frame)
    return True


def key_shape(obj, name, frame, value):
    if obj is None or obj.data is None or obj.data.shape_keys is None:
        return False
    kb = obj.data.shape_keys.key_blocks.get(name)
    if kb is None:
        return False
    kd = obj.data.shape_keys
    if kd.animation_data is None:
        kd.animation_data_create()
    kb.value = float(value)
    kb.keyframe_insert("value", frame=frame)
    return True


def set_interpolation(act, mode="BEZIER"):
    for fc in action_fcurves(act):
        for kp in fc.keyframe_points:
            kp.interpolation = mode
            kp.handle_left_type = kp.handle_right_type = "AUTO_CLAMPED"


def ease_range(act, f0, f1, sigma=0.55):
    """Gaussian-ish ease on the outermost keys: fast in, slow out, no linear ramps."""
    for fc in action_fcurves(act):
        kps = sorted(fc.keyframe_points, key=lambda k: k.co[0])
        if len(kps) < 3:
            continue
        for kp in kps:
            d = min(abs(kp.co[0] - f0), abs(kp.co[0] - f1))
            kp.easing = "EASE_IN_OUT" if d < (f1 - f0) * 0.25 else "AUTO"


def breathe(arm, f0, cycles=1, period=112, amp_chest=1.6, amp_head=0.9, seed=1):
    """
    A breath is not a sine wave: 40% inhale, 60% exhale, and the head lags the chest.

    The lag is the whole point. A chest and a head that move on the same frame look
    like a single rigid object.
    """
    rnd = random.Random(seed)
    for c in range(cycles):
        t0 = f0 + c * period
        inhale = period * 0.40
        exhale = period * 0.60
        amt = amp_chest * (1.0 + rnd.uniform(-0.06, 0.06))
        key_bone(arm, "chest", t0, rot=(-amt * 0.6, 0, 0))
        key_bone(arm, "chest", t0 + inhale, rot=(amt, 0, 0))
        key_bone(arm, "chest", t0 + inhale + exhale, rot=(-amt * 0.6, 0, 0))
        # head and shoulders follow, offset by 2 and 4 frames
        key_bone(arm, "head", t0 + 2, rot=(amt * 0.35, 0, 0))
        key_bone(arm, "head", t0 + 2 + inhale, rot=(-amt * 0.5, 0, 0))
        key_bone(arm, "head", t0 + 2 + inhale + exhale, rot=(amt * 0.35, 0, 0))
        key_bone(arm, "spine", t0 + 4, rot=(amt * 0.25, 0, 0))
        key_bone(arm, "spine", t0 + 4 + inhale, rot=(-amt * 0.2, 0, 0))


# ---------------------------------------------------------------------------- clips
def clip_idle_breathing(arm, head):
    """28 s: slow breathing plus a weight shift on a non-harmonic period."""
    act = ensure_action(arm, "Idle_Breathing")
    frames = 672
    breathe(arm, 1, cycles=6, period=112, amp_chest=1.8, amp_head=1.0, seed=11)
    rnd = random.Random(11)
    # weight shift: period deliberately not a divisor of the breath period
    f = 20
    while f < frames - 40:
        side = rnd.choice([-1, 1]) * rnd.uniform(0.6, 1.4)
        key_bone(arm, "pelvis", f, rot=(0, 0, side), loc=(side * 0.004, 0, 0))
        key_bone(arm, "pelvis", f + 60, rot=(0, 0, side * 0.3), loc=(0, 0, 0))
        f += rnd.randint(120, 190)
    key_bone(arm, "pelvis", 1, rot=(0, 0, 0))
    key_bone(arm, "pelvis", frames, rot=(0, 0, 0))
    key_bone(arm, "chest", 1); key_bone(arm, "head", 1); key_bone(arm, "spine", 1)
    key_bone(arm, "chest", frames); key_bone(arm, "head", frames); key_bone(arm, "spine", frames)
    set_interpolation(act)
    return act, frames


def clip_blink(arm, head):
    """Two blinks at different intervals and speeds: never a metronome."""
    act = ensure_action(arm, "Blink_Natural")
    for head_start, close_f, open_f in ((1, 2, 4), (58, 2, 5)):
        key_shape(head, "Blink", head_start, 0.0)
        key_shape(head, "Blink", head_start + close_f, 1.0)
        key_shape(head, "Blink", head_start + close_f + 1, 1.0)   # a hold, not a triangle
        key_shape(head, "Blink", head_start + close_f + 1 + open_f, 0.0)
    set_interpolation(act, "BEZIER")
    return act, 90


EXPRESSIONS = [
    ("Expression_Happy",      dict(smile=0.85, squint=0.35, brow_in=0.15,
                                   head_rot=(-3, 0, 2), chest=(-1, 0, 0))),
    ("Expression_Sad",        dict(frown=0.70, brow_in=0.55,
                                   head_rot=(6, 0, -3), chest=(2, 0, 0))),
    ("Expression_Surprised",  dict(jaw=0.75, brow_in=0.90,
                                   head_rot=(-7, 0, 0), chest=(-3, 0, 0))),
    ("Expression_Thinking",   dict(brow_dnL=0.55, squint=0.30,
                                   head_rot=(2, 0, 8), chest=(1, 0, -2))),
    ("Expression_Curious",    dict(brow_in=0.35, smile=0.30, head_rot=(-4, 0, -6))),
    ("Expression_Frustrated", dict(brow_dnL=0.75, brow_dnR=0.60, frown=0.45,
                                   head_rot=(4, 0, 0), chest=(2, 0, 1))),
]


def _apply_face(arm, head, f, jaw=0.0, smile=0.0, frown=0.0, brow_in=0.0,
                brow_dnL=0.0, brow_dnR=0.0, squint=0.0,
                head_rot=(0, 0, 0), chest=(0, 0, 0), eyes=(0, 0, 0)):
    key_shape(head, "jawOpen", f, jaw)
    key_shape(head, "mouthSmile_L", f, smile)
    key_shape(head, "mouthSmile_R", f, smile * 0.92)     # one side leads
    key_shape(head, "mouthFrown", f, frown)
    key_shape(head, "browInnerUp", f, brow_in)
    key_shape(head, "browDown_L", f, brow_dnL)
    key_shape(head, "browDown_R", f, brow_dnR)
    key_shape(head, "eyeSquint_L", f, squint)
    key_shape(head, "eyeSquint_R", f, squint * 0.95)
    key_bone(arm, "head", f, rot=head_rot)
    key_bone(arm, "chest", f, rot=chest)
    if any(eyes):
        for side in ("L", "R"):
            key_bone(arm, f"eye.{side}", f, rot=eyes)


def clip_expressions(arm, head):
    """One expression per beat, for the contact sheet render."""
    act = ensure_action(arm, "Expression_Set")
    _apply_face(arm, head, 1)
    for i, (_n, kw) in enumerate(EXPRESSIONS):
        _apply_face(arm, head, 48 * (i + 1), **kw)
    _apply_face(arm, head, 336)
    set_interpolation(act)
    ease_range(act, 1, 336)
    return act, 336


def clip_expression_single(arm, head, name, kw):
    """
    A standalone 1.8 s expression: counter-pose, peak with 6% overshoot, settle,
    moving hold. The last key stays near the pose so a runtime that clamps the clip
    keeps the expression instead of snapping back to rest.
    """
    act = ensure_action(arm, name)
    counter = {}
    for k, v in kw.items():
        if isinstance(v, (int, float)):
            counter[k] = -0.25 * min(1.0, float(v))
        elif isinstance(v, tuple):
            counter[k] = tuple(-0.25 * c for c in v)
    peak = {}
    for k, v in kw.items():
        if isinstance(v, (int, float)):
            peak[k] = min(1.0, v * 1.06)
        elif isinstance(v, tuple):
            peak[k] = tuple(c * 1.06 for c in v)

    _apply_face(arm, head, 1)
    _apply_face(arm, head, 6, **counter)
    _apply_face(arm, head, 14, **peak)
    _apply_face(arm, head, 21, **kw)
    hold = {k: (v * 0.93 if isinstance(v, (int, float)) else v) for k, v in kw.items()}
    hold["head_rot"] = tuple(c + d for c, d in zip(kw.get("head_rot", (0, 0, 0)), (1.2, 0, -0.8)))
    _apply_face(arm, head, 34, **hold, eyes=(0, 0, 3.0))
    hold2 = dict(hold)
    hold2["head_rot"] = tuple(c + d for c, d in zip(hold.get("head_rot", (0, 0, 0)), (-0.6, 0, 0.4)))
    _apply_face(arm, head, 44, **hold2)
    set_interpolation(act)
    ease_range(act, 1, 44)
    return act, 44


def clip_look_around(arm, head, frames=288, seed=23):
    """Idle_LookAround: the eyes lead, the head follows 3 frames later, then drifts back."""
    rnd = random.Random(seed)
    act = ensure_action(arm, "Idle_LookAround")
    for side in ("L", "R"):
        key_bone(arm, f"eye.{side}", 1, rot=(0, 0, 0))
    key_bone(arm, "head", 1, rot=(0, 0, 0))
    f = 12
    while f < frames - 40:
        yaw = rnd.choice([-1, 1]) * rnd.uniform(5, 13)
        pitch = rnd.uniform(-3, 4)
        for side in ("L", "R"):
            key_bone(arm, f"eye.{side}", f, rot=(pitch * 0.6, 0, yaw))
        key_bone(arm, "head", f + 3, rot=(pitch * 0.35, 0, yaw * 0.42))
        key_bone(arm, "head", f + 9, rot=(pitch * 0.30, 0, yaw * 0.36))
        key_bone(arm, "head", f + 26, rot=(0, 0, 0))
        for side in ("L", "R"):
            key_bone(arm, f"eye.{side}", f + 22, rot=(0, 0, 0))
        f += rnd.randint(46, 74)
    key_bone(arm, "head", frames, rot=(0, 0, 0))
    for side in ("L", "R"):
        key_bone(arm, f"eye.{side}", frames, rot=(0, 0, 0))
    # one blink at a deliberately unround moment
    key_shape(head, "Blink", 1, 0.0)
    key_shape(head, "Blink", 96, 0.0)
    key_shape(head, "Blink", 98, 1.0)
    key_shape(head, "Blink", 99, 1.0)
    key_shape(head, "Blink", 103, 0.0)
    set_interpolation(act)
    ease_range(act, 1, frames)
    return act, frames


def clip_gestures(arm, head):
    """Four gestures in one reel; the segments are split into named clips afterwards."""
    act = ensure_action(arm, "Gesture_Set")

    def g(f, la=(0, 0, 0), ra=(0, 0, 0), lf=(0, 0, 0), rf=(0, 0, 0),
          ch=(0, 0, 0), hd=(0, 0, 0)):
        key_bone(arm, "upperarm.L", f, rot=la)
        key_bone(arm, "upperarm.R", f, rot=ra)
        key_bone(arm, "forearm.L", f, rot=lf)
        key_bone(arm, "forearm.R", f, rot=rf)
        key_bone(arm, "chest", f, rot=ch)
        key_bone(arm, "head", f, rot=hd)

    g(1)
    # Explain: hands rise, then open outward; the dip at frame 8 is the anticipation
    g(8,  la=(8, 0, -4), ra=(8, 0, 4), ch=(2, 0, 0), hd=(3, 0, 0))
    g(20, la=(-38, 0, -16), ra=(-34, 0, 14), lf=(-40, 0, 0), rf=(-36, 0, 0),
         ch=(-2, 0, 0), hd=(-2, 0, 0))
    g(34, la=(-46, 0, -26), ra=(-40, 0, 22), lf=(-52, 0, 0), rf=(-46, 0, 0))
    g(52)
    # Greet: one arm lifts, the forearm follows two frames later
    g(66, ra=(6, 0, 8))
    g(78, ra=(-96, 0, 26), rf=(-24, 0, 0), hd=(-4, 0, -6), ch=(-1, 0, -3))
    g(88, ra=(-104, 0, 30), rf=(-18, 0, 0))
    g(104, ra=(-98, 0, 28))
    g(120)
    # Point: asymmetric, torso rotates into it
    g(134, la=(10, 0, -6), ch=(0, 0, 4))
    g(146, la=(-72, 0, -34), lf=(-30, 0, 0), ch=(-2, 0, 10), hd=(-3, 0, 8))
    g(162, la=(-78, 0, -38), lf=(-24, 0, 0))
    g(180)
    # Emphasise: two accents, the second softer than the first
    g(192, ch=(1, 0, 0))
    g(198, la=(-30, 0, -10), ra=(-30, 0, 10), ch=(-3, 0, 0), hd=(-2, 0, 0))
    g(206, la=(-20, 0, -8), ra=(-20, 0, 8))
    g(214, la=(-34, 0, -12), ra=(-34, 0, 12), ch=(-2, 0, 0), hd=(-1, 0, 0))
    g(232)
    set_interpolation(act)
    ease_range(act, 1, 232)
    return act, 232


GESTURE_SEGMENTS = {
    "Gesture_Explain": (1, 58),
    "Gesture_Greet": (58, 122),
    "Gesture_Point": (122, 186),
    "Gesture_Emphasize": (186, 232),
}


def extract_subclip(source_name, new_name, f0, f1):
    """
    Duplicate a reel and trim it, rather than rebuilding the poses.

    Rebuilding was the obvious approach and silently produced empty clips: in Blender 5
    a slotted action's curves live inside a channelbag, so copying curve-by-curve into a
    fresh action gets nothing. Duplicating and trimming keeps them.
    """
    src = bpy.data.actions.get(source_name)
    if src is None:
        return None
    old = bpy.data.actions.get(new_name)
    if old is not None:
        bpy.data.actions.remove(old)
    dst = src.copy()
    dst.name = new_name
    dst.use_fake_user = True
    for fc in action_fcurves(dst):
        outside = [kp for kp in fc.keyframe_points
                   if not (f0 - 0.001 <= kp.co[0] <= f1 + 0.001)]
        for kp in sorted(outside, key=lambda k: -k.co[0]):
            fc.keyframe_points.remove(kp)
        for kp in fc.keyframe_points:
            kp.co = (kp.co[0] - f0 + 1.0, kp.co[1])
            kp.handle_left = (kp.handle_left[0] - f0 + 1.0, kp.handle_left[1])
            kp.handle_right = (kp.handle_right[0] - f0 + 1.0, kp.handle_right[1])
        fc.update()
    return dst, int(f1 - f0 + 1)


def clip_talk(arm, head, name="Talk_Neutral", seed=5, amplitude=1.0):
    """Speech-shaped mouth motion plus head accents; phoneme timing stays a separate file."""
    rnd = random.Random(seed)
    act = ensure_action(arm, name)
    visemes = ["viseme_AA", "viseme_E", "viseme_I", "viseme_O", "viseme_U", "viseme_MBP"]
    f = 1
    last = 240
    while f < last:
        v = rnd.choice(visemes)
        open_amt = rnd.uniform(0.35, 0.95) * amplitude
        key_shape(head, v, f, open_amt)
        for other in visemes:
            if other != v:
                key_shape(head, other, f, 0.0)
        key_bone(arm, "head", f, rot=(rnd.uniform(-1.5, 1.5), 0, rnd.uniform(-2.5, 2.5)))
        f += rnd.randint(4, 9)
    for v in visemes:
        key_shape(head, v, last, 0.0)
    key_bone(arm, "head", last, rot=(0, 0, 0))
    set_interpolation(act)
    return act, last


def clip_walk(arm, head, cycle=24, n=3):
    """Three strides: double bounce, pelvis dip, contralateral arm swing."""
    act = ensure_action(arm, "Walk")
    frames = cycle * n + 1
    for i in range(n):
        t0 = 1 + i * cycle
        half = cycle // 2
        for s, phase in ((1, 0), (-1, half)):
            bl = "L" if s > 0 else "R"
            br = "R" if s > 0 else "L"
            key_bone(arm, f"thigh.{bl}", t0 + phase, rot=(-22, 0, 0))
            key_bone(arm, f"shin.{bl}", t0 + phase + 4, rot=(18, 0, 0))
            key_bone(arm, f"thigh.{br}", t0 + phase, rot=(16, 0, 0))
            key_bone(arm, f"shin.{br}", t0 + phase + 2, rot=(8, 0, 0))
            # arms swing opposite to the legs
            key_bone(arm, f"upperarm.{bl}", t0 + phase, rot=(16, 0, -2))
            key_bone(arm, f"upperarm.{br}", t0 + phase, rot=(-16, 0, 2))
        # pelvis: one dip per step, plus a slight bounce
        key_bone(arm, "pelvis", t0, loc=(0, 0, -0.012))
        key_bone(arm, "pelvis", t0 + half // 2, loc=(0, 0, 0.008))
        key_bone(arm, "pelvis", t0 + half, loc=(0, 0, -0.012))
    key_bone(arm, "pelvis", frames, loc=(0, 0, -0.012))
    key_bone(arm, "chest", 1, rot=(0, 0, 0))
    key_bone(arm, "chest", frames, rot=(0, 0, 0))
    set_interpolation(act)
    return act, frames


def clip_turn(arm, head):
    """A 90-degree turn read through the head: the head leads, the chest follows."""
    act = ensure_action(arm, "Turn")
    # The root bone is vertical, so its local Y is world +Z: a world yaw is a local Y
    # rotation. (0, 0, 88) was a pitch and folded the character face-down 88 degrees.
    key_bone(arm, "root", 1, rot=(0, 0, 0))
    key_bone(arm, "head", 1, rot=(0, 0, 0))
    key_bone(arm, "head", 6, rot=(0, 0, 18))
    key_bone(arm, "chest", 10, rot=(0, 0, 24))
    key_bone(arm, "root", 16, rot=(0, 88, 0))
    key_bone(arm, "chest", 24, rot=(0, 0, 4))
    key_bone(arm, "head", 30, rot=(0, 0, 0))
    key_bone(arm, "root", 38, rot=(0, 90, 0))
    set_interpolation(act)
    return act, 38


def clip_react(arm, head, name, positive=True):
    """A 1.3 s reaction: recoil, settle, small secondary motion."""
    act = ensure_action(arm, name)
    s = 1 if positive else -1
    key_bone(arm, "chest", 1, rot=(0, 0, 0))
    key_bone(arm, "head", 1, rot=(0, 0, 0))
    key_bone(arm, "chest", 4, rot=(-7 * s, 0, 0))          # anticipation
    key_bone(arm, "head", 8, rot=(-12 * s, 0, 3 * s))
    key_bone(arm, "chest", 10, rot=(5 * s, 0, 0))
    key_bone(arm, "head", 14, rot=(6 * s, 0, -2 * s))
    key_bone(arm, "chest", 22, rot=(1 * s, 0, 0))
    key_bone(arm, "head", 24, rot=(2 * s, 0, 0))
    key_bone(arm, "head", 32, rot=(0, 0, 0))
    key_bone(arm, "chest", 32, rot=(0, 0, 0))
    # the face carries the reaction too
    if positive:
        key_shape(head, "mouthSmile_L", 8, 0.7)
        key_shape(head, "mouthSmile_R", 9, 0.65)
        key_shape(head, "eyeSquint_L", 10, 0.3)
        key_shape(head, "browInnerUp", 8, 0.4)
    else:
        key_shape(head, "mouthFrown", 8, 0.6)
        key_shape(head, "browDown_L", 9, 0.5)
        key_shape(head, "browDown_R", 9, 0.45)
    set_interpolation(act)
    ease_range(act, 1, 32)
    return act, 32


# ----------------------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--blend", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--report", required=True)
    a = ap.parse_args()

    bpy.ops.wm.open_mainfile(filepath=a.blend)
    bpy.context.scene.render.fps = FPS
    arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    head = next(o for o in bpy.data.objects if o.type == "MESH")
    if arm.animation_data is None:
        arm.animation_data_create()
    if head.data.shape_keys and head.data.shape_keys.animation_data is None:
        head.data.shape_keys.animation_data_create()

    report = {"fps": FPS, "clips": {}, "warnings": [],
              "shape_keys": len(head.data.shape_keys.key_blocks) if head.data.shape_keys else 0}

    builders = [
        ("Idle_Breathing",   lambda: clip_idle_breathing(arm, head)),
        ("Idle_LookAround",  lambda: clip_look_around(arm, head)),
        ("Blink_Natural",    lambda: clip_blink(arm, head)),
        ("Expression_Set",   lambda: clip_expressions(arm, head)),
        *[(nm, (lambda nm=nm, kw=kw: clip_expression_single(arm, head, nm, kw)))
          for nm, kw in EXPRESSIONS],
        ("Gesture_Set",      lambda: clip_gestures(arm, head)),
        ("Talk_Neutral",     lambda: clip_talk(arm, head, "Talk_Neutral", seed=5, amplitude=1.0)),
        ("Talk_Expressive",  lambda: clip_talk(arm, head, "Talk_Expressive", seed=17, amplitude=1.15)),
        ("Walk",             lambda: clip_walk(arm, head)),
        ("Turn",             lambda: clip_turn(arm, head)),
        ("React_Positive",   lambda: clip_react(arm, head, "React_Positive", True)),
        ("React_Negative",   lambda: clip_react(arm, head, "React_Negative", False)),
    ]

    for name, fn in builders:
        # create/attach FIRST, so bone keys and morph keys land in the same action
        ensure_action(arm, name)
        if head.data.shape_keys:
            head.data.shape_keys.animation_data.action = bpy.data.actions[name]
        act, last = fn()
        curves = action_fcurves(act)
        bones = sum(1 for fc in curves if "pose.bones" in fc.data_path)
        morphs = sum(1 for fc in curves if "key_blocks" in fc.data_path)
        report["clips"][name] = {
            "frames": last, "seconds": round((last - 1) / FPS, 2),
            "fcurves": len(curves), "bone_curves": bones, "morph_curves": morphs,
            "keys": sum(len(fc.keyframe_points) for fc in curves),
        }
        if len(curves) == 0:
            report["warnings"].append(f"{name}: no fcurves")

    # split the gesture reel into individually named clips
    reported = {n for n, _ in builders}
    for new_name, (f0, f1) in GESTURE_SEGMENTS.items():
        res = extract_subclip("Gesture_Set", new_name, f0, f1)
        if res:
            curves = action_fcurves(res[0])
            report["clips"][new_name] = {
                "frames": res[1], "seconds": round((res[1] - 1) / FPS, 2),
                "fcurves": len(curves), "bone_curves": len(curves), "morph_curves": 0,
                "keys": sum(len(fc.keyframe_points) for fc in curves),
                "derived_from": "Gesture_Set",
            }
            reported.add(new_name)

    # leave every clip on its own NLA track: that is how the exporter sees them all
    for o in bpy.data.objects:
        if o.animation_data:
            o.animation_data.action = None
    kd = head.data.shape_keys
    if kd and kd.animation_data:
        kd.animation_data.action = None
        for tr in list(kd.animation_data.nla_tracks):
            kd.animation_data.nla_tracks.remove(tr)
    for tr in list(arm.animation_data.nla_tracks):
        arm.animation_data.nla_tracks.remove(tr)

    def slot_for(act, kind):
        for sl in act.slots:
            if (sl.identifier or "").startswith(kind):
                return sl
        return None

    tracks = []
    for name in sorted(reported):
        act = bpy.data.actions.get(name)
        if act is None:
            continue
        curves = action_fcurves(act)
        has_bones = any("pose.bones" in fc.data_path for fc in curves)
        has_morphs = any("key_blocks" in fc.data_path for fc in curves)
        kinds = []
        if has_bones:
            ad = arm.animation_data
            tr = ad.nla_tracks.new()
            tr.name = name
            strip = tr.strips.new(name, 1, act)
            sl = slot_for(act, "OB")
            if sl is not None and hasattr(strip, "action_slot"):
                strip.action_slot = sl
            tr.mute = True
            kinds.append("bones")
        if has_morphs and kd:
            ad = kd.animation_data
            tr = ad.nla_tracks.new()
            tr.name = name
            strip = tr.strips.new(name, 1, act)
            sl = slot_for(act, "KE")
            if sl is not None and hasattr(strip, "action_slot"):
                strip.action_slot = sl
            tr.mute = True
            kinds.append("morphs")
        tracks.append({"clip": name, "tracks": kinds, "fcurves": len(curves)})
    report["nla_tracks"] = tracks

    # remove anything that is not one of our clips (probes and primitives leave debris)
    for act in list(bpy.data.actions):
        if act.name not in reported and act.users == 0:
            bpy.data.actions.remove(act)

    bpy.context.scene.frame_start = 1
    bpy.context.scene.frame_end = max([v["frames"] for v in report["clips"].values()] + [672])
    reset_pose(arm)                     # never save a master scene mid-pose

    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out))
    report["actions_in_file"] = sorted(x.name for x in bpy.data.actions)
    report["blend"] = str(out)
    pathlib.Path(a.report).write_text(json.dumps(report, indent=2))
    print(json.dumps({"clips": len(report["clips"]), "warnings": report["warnings"],
                      "actions_in_file": report["actions_in_file"]}, indent=2))


if __name__ == "__main__":
    main()
