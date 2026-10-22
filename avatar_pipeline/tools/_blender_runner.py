#!/usr/bin/env python3
"""Headless native Blender 4.2 CLI shim (`bpy==4.2.0` + `CYCLES` + `OPENIMAGEDENOISE`)."""
import os
import runpy
import sys

_STUBS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_x11_stubs")
if _STUBS_DIR not in os.environ.get("LD_LIBRARY_PATH", ""):
    os.environ["LD_LIBRARY_PATH"] = f"{_STUBS_DIR}:/home/user/.local/lib:{os.environ.get('LD_LIBRARY_PATH', '')}"
    os.execv(sys.executable, [sys.executable] + sys.argv)

import bpy


def main() -> None:
    args = sys.argv[1:]
    blend_path = None
    script_path = None
    script_args = []

    i = 0
    while i < len(args):
        a = args[i]
        if a in ("-v", "--version"):
            print(f"Blender {bpy.app.version_string}")
            sys.stdout.flush()
            os._exit(0)
        elif a == "--factory-startup":
            bpy.ops.wm.read_factory_settings(use_empty=False)
            i += 1
        elif a in ("-b", "--background", "--python-use-system-env", "--online-mode"):
            i += 1
        elif a in ("-P", "--python") and i + 1 < len(args):
            script_path = args[i + 1]
            i += 2
        elif a == "--":
            script_args = args[i + 1 :]
            break
        elif not a.startswith("-") and a.endswith(".blend"):
            blend_path = a
            i += 1
        else:
            i += 1

    if blend_path:
        bpy.ops.wm.open_mainfile(filepath=os.path.abspath(blend_path))

    if script_path:
        sys.argv = [script_path, "--"] + script_args if script_args else [script_path]
        script_dir = os.path.dirname(os.path.abspath(script_path))
        if script_dir not in sys.path:
            sys.path.insert(0, script_dir)
        runpy.run_path(script_path, run_name="__main__")

    sys.stdout.flush()
    sys.stderr.flush()
    os._exit(0)


if __name__ == "__main__":
    main()
