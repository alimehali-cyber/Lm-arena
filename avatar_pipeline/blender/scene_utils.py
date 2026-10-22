"""Shared helpers. Kept tiny so every stage can depend on it."""
import bpy


def reset_pose(arm, reset_shapes=True):
    """
    Put the rig back to rest.

    Clearing `animation_data.action` does NOT clear the pose: bones keep the transforms
    that were last evaluated, so a scene saved at the end of an animation renders in
    whatever pose was keyed last. That looked exactly like "the body disappeared".
    """
    if arm is None:
        return
    for pb in arm.pose.bones:
        pb.location = (0, 0, 0)
        pb.rotation_quaternion = (1, 0, 0, 0)
        pb.rotation_euler = (0, 0, 0)
        pb.scale = (1, 1, 1)
    if reset_shapes:
        for o in bpy.data.objects:
            if o.type == "MESH" and o.data and o.data.shape_keys:
                for kb in o.data.shape_keys.key_blocks:
                    if kb.name != "Basis":
                        kb.value = 0.0
                o.data.shape_keys.eval_time = 0.0
