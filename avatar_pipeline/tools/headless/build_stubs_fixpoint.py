#!/usr/bin/env python3
"""
Build stub shared libraries so `import bpy` works on a headless box with no X11.

Blender's Python module links against a pile of windowing libraries that a
server-class container does not ship. We cannot install them (no root), but we can
satisfy the dynamic loader with empty stand-ins: Blender only *calls* X11 functions
when it opens a window, and a headless run never does.

The trick is that the link editor wants specific symbol names, so the set has to be
discovered iteratively:

    1. try `import bpy`
    2. read "undefined symbol: NAME" out of the error
    3. add a stub definition, rebuild, repeat
    4. stop when the import succeeds (or nothing new is reported)

libEGL is deliberately NOT stubbed. Without it Blender cannot create a GPU context,
which is what we want: the renderer must fall back to Cycles on the CPU, and a silent
software-GL success would make benchmark numbers meaningless.

Output: tools/headless/stublibs/*.so  (add to LD_LIBRARY_PATH before running bpy)
"""
from __future__ import annotations
import os
import re
import shutil
import subprocess
import sys
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
STUBDIR = HERE / "stublibs"
SRCDIR = HERE / "stubsrc"

# Library SONAMEs the loader asks for by name. Anything not listed here that is missing
# will surface as "cannot open shared object file" rather than "undefined symbol", and
# needs adding to this list.
LIBS = [
    "libXrender.so.1", "libXi.so.6", "libXfixes.so.3", "libXxf86vm.so.1",
    "libXkbfile.so.1", "libX11-xcb.so.1", "libxcb-glx.so.0", "libxcb-dri2.so.0",
    "libxcb-shm.so.0", "libxcb-util.so.1", "libXcursor.so.1", "libXrandr.so.2",
    "libXinerama.so.1", "libSM.so.6", "libICE.so.6", "libXt.so.6",
    "libXmu.so.6", "libXpm.so.4", "libXext.so.6", "libX11.so.6",
    "libxkbcommon.so.0", "libxkbcommon-x11.so.0", "libwayland-client.so.0",
    "libwayland-cursor.so.0", "libwayland-egl.so.1", "libdecor-0.so.0",
]
# libGL must be stubbed or the module will not even load; it is a *fake* GL, so any
# attempt to create a real context fails loudly instead of silently going software.
# libEGL is left absent on purpose, for the same reason.
NEVER = {"libEGL.so.1"}
LIBS += ["libGL.so.1", "libGLX.so.0", "libOpenGL.so.0", "libGLdispatch.so.0"]

UNDEF = re.compile(r"undefined symbol: ([A-Za-z_][A-Za-z0-9_]*)")
CANTOPEN = re.compile(r"([\w.+-]+\.so(?:\.\d+)*): cannot open shared object file")

def symbols_for(lib: str) -> list[str]:
    f = SRCDIR / (lib + ".syms")
    return f.read_text().split() if f.exists() else []

ALL_SYMS = SRCDIR / "_all.syms"


def all_symbols() -> list[str]:
    if not ALL_SYMS.exists():
        return []
    return sorted(set(ALL_SYMS.read_text().split()))


def add_symbols(lib: str, names: list[str]) -> None:
    cur = set(all_symbols())
    before = len(cur)
    cur.update(names)
    ALL_SYMS.write_text("\n".join(sorted(cur)) + "\n")
    return len(cur) != before


def build_all() -> bool:
    """
    Compile every SONAME from one source that defines all the stub symbols.

    Two things this deliberately does NOT do:
      * symlink all names to one file -- the loader rejects that with a version-matching
        assertion, because two SONAMEs then map the same inode;
      * rely on DT_NEEDED to a shared "all" library -- modern ld drops it as unneeded,
        so the symbols never enter the global scope and resolution still fails.
    Each library therefore defines the symbols itself. They are trivial bodies, so
    compiling 30 of them costs well under a second.
    """
    syms = all_symbols()
    src = SRCDIR / "_stub_all.c"
    lines = ["/* generated: headless stubs for bpy. Do not edit by hand. */"]
    for s_ in syms:
        lines.append(f"void {s_}(void) {{ }}")
    src.write_text("\n".join(lines) + "\n")
    for lib in LIBS:
        out = STUBDIR / lib
        r = subprocess.run(["gcc", "-shared", "-fPIC", "-O0", "-o", str(out), str(src),
                            f"-Wl,-soname,{lib}"], capture_output=True, text=True)
        if r.returncode != 0:
            print(f"  compile failed for {lib}: {r.stderr.strip()[:300]}")
            return False
    return True


def preseed(venv_python: pathlib.Path) -> list[str]:
    """
    Collect every X11/GL/wayland symbol that bpy's shipped libraries leave undefined.

    The loader only reports the first missing symbol per load, so a pure fixpoint needs
    ~500 rounds. Reading the undefined symbol tables once turns that into a handful.
    """
    import re as _re
    root = venv_python.parent.parent / "lib/python3.11/site-packages/bpy"
    prefix = _re.compile(r"^(X|gl|glX|egl|wl_|xkb|xcb|Xkb|vk|dri)")
    found = set()
    for so in list(root.rglob("*.so")) + list(root.rglob("*.so.*")):
        try:
            r = subprocess.run(["nm", "-D", "--undefined-only", str(so)],
                               capture_output=True, text=True, timeout=60)
        except Exception:
            continue
        for line in r.stdout.splitlines():
            parts = line.split()
            if len(parts) < 2:
                continue
            sym = parts[-1].split("@")[0]
            if prefix.match(sym) and sym not in ("glib",):
                found.add(sym)
    return sorted(found)


def try_import(venv_python: pathlib.Path) -> tuple[bool, list[tuple[str, str]]]:
    env = dict(os.environ)
    env["LD_LIBRARY_PATH"] = str(STUBDIR) + (":" + env["LD_LIBRARY_PATH"] if env.get("LD_LIBRARY_PATH") else "")
    r = subprocess.run([str(venv_python), "-c", "import bpy; print('OK', bpy.app.version_string)"],
                       capture_output=True, text=True, env=env, timeout=300)
    if "OK " in r.stdout:
        return True, []
    text = (r.stdout or "") + (r.stderr or "")
    problems = []
    for m in UNDEF.finditer(text):
        # attribute the symbol to whichever stub lib is most plausible; the loader does
        # not say which one it wanted, so put it in every lib (duplicates are harmless)
        problems.append(("__any__", m.group(1)))
    for m in CANTOPEN.finditer(text):
        lib = m.group(1)
        if lib in NEVER:
            continue
        problems.append((lib, None))
    if not problems:
        print(text.strip()[-800:])
    return False, problems

def main():
    # this file lives in tools/headless/, so the repo root is two levels up; parents[0]
    # is tools/ and silently pointed at a venv that does not exist
    venv_python = (pathlib.Path(sys.argv[1]) if len(sys.argv) > 1
                   else HERE.parents[1] / ".venv/bin/python")
    STUBDIR.mkdir(parents=True, exist_ok=True)
    SRCDIR.mkdir(parents=True, exist_ok=True)
    ALL_SYMS.touch()
    seed = preseed(venv_python)
    add_symbols("__any__", seed)
    print(f"pre-seeded {len(seed)} candidate symbols from the shipped libraries")
    if not build_all():
        return 1

    for round_no in range(1200):
        ok, problems = try_import(venv_python)
        if ok:
            print(f"converged after {round_no} rounds")
            print(f"stub libraries: {len(LIBS)}   stub symbols: {len(all_symbols())}")
            return 0
        if not problems:
            print("import failed for a reason that is not a missing symbol; see output above")
            return 1
        added = False
        for lib, sym in problems:
            if sym:
                added |= add_symbols("__any__", [sym])
            else:
                if lib not in LIBS:
                    print(f"discovered new library {lib}; adding it")
                    LIBS.append(lib)
                    added = True
        if not added:
            print("no progress; stopping")
            return 1
        if not build_all():
            return 1
        if round_no % 50 == 0:
            print(f"round {round_no}: {len(all_symbols())} stub symbols")
    return 1


if __name__ == "__main__":
    sys.exit(main())
