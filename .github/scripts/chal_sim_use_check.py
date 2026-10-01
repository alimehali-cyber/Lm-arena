#!/usr/bin/env python3
"""Optional live check of Chal with sim-use (macOS CLI driving an Android emulator).

Follows the skill's loop: observe (`ui`) -> act (`tap @N` from that very observation) -> verify.
Screenshots are used only for visual checks. Crash protocol: if the app process disappears or a
system crash dialog is detected, STOP and report; the app is never relaunched automatically.

The app's default language is Persian, so the labels below are looked up in both languages. The
Persian strings are the exact code points (including U+200C) taken from the Kotlin sources.
"""
import argparse
import json
import pathlib
import subprocess
import sys
import time

PACKAGE = "com.alijafari.red.astronomy"

LABELS = {
    "controls": ("Controls", "\u06a9\u0646\u062a\u0631\u0644\u200c\u0647\u0627"),
    "close_controls": ("Close controls", "\u0628\u0633\u062a\u0646 \u06a9\u0646\u062a\u0631\u0644\u200c\u0647\u0627"),
    "show_ui": ("Show interface", "\u0646\u0645\u0627\u06cc\u0634 \u0631\u0627\u0628\u0637"),
    "hide_ui": ("Hide interface", "\u067e\u0646\u0647\u0627\u0646 \u06a9\u0631\u062f\u0646 \u0631\u0627\u0628\u0637"),
    "back_lab": ("Back to Lab", "\u0628\u0627\u0632\u06af\u0634\u062a \u0628\u0647 \u0622\u0632\u0645\u0627\u06cc\u0634\u06af\u0627\u0647"),
    "reset_view": ("Reset View", "\u0628\u0627\u0632\u0646\u0634\u0627\u0646\u06cc \u062f\u06cc\u062f"),
    "render_problem": ("Rendering problem", "\u0645\u0634\u06a9\u0644 \u0631\u0646\u062f\u0631"),
    "resume": ("Resume", "\u0627\u062f\u0627\u0645\u0647"),
    "pause": ("Pause", "\u062a\u0648\u0642\u0641"),
    "tab_physics": ("Physics", "\u0641\u06cc\u0632\u06cc\u06a9"),
    "tab_system": ("System", "\u0633\u0627\u0645\u0627\u0646\u0647"),
    "tab_modules": ("Modules", "\u0645\u0627\u0698\u0648\u0644\u200c\u0647\u0627"),
    "lab_nav": ("Lab", "\u0622\u0632\u0645\u0627\u06cc\u0634\u06af\u0627\u0647"),
    "chal_card": ("Chal", "\u0686\u0627\u0644"),
}
DENY = ("Don't allow", "Don\u2019t allow", "DON'T ALLOW", "DON\u2019T ALLOW", "Deny", "DENY")
INTERACTIVE = ("button", "tab", "switch", "radio", "check", "link")


class CrashError(RuntimeError):
    """The app died or a system crash dialog appeared. Never retried; never relaunched."""


class StepAbort(Exception):
    """A fatal step failed; later steps cannot be meaningful."""


def escape(text):
    return str(text).replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")[:3500]


def sh(*args, timeout=60):
    result = subprocess.run(args, text=True, capture_output=True, timeout=timeout)
    combined = result.stdout + result.stderr
    if "PROCESS DISAPPEARED" in combined or "CRASH DIALOG DETECTED" in combined:
        raise CrashError(combined)
    return result.returncode, result.stdout, combined


def run(*args, timeout=60):
    code, out, combined = sh(*args, timeout=timeout)
    if code:
        raise RuntimeError(f"Command failed ({code}): {' '.join(args)}\n{combined}")
    return out


def names(entry):
    return [s.strip() for s in (entry.get("label"), entry.get("value")) if isinstance(s, str) and s.strip()]


def has(key):
    wanted = set(LABELS[key])
    return lambda entry, _data: any(n in wanted for n in names(entry))


def starts(key):
    prefixes = LABELS[key]
    return lambda entry, _data: any(n.startswith(prefixes) for n in names(entry))


def bottom_lab_nav(entry, data):
    wanted = set(LABELS["lab_nav"])
    return any(n in wanted for n in names(entry)) and entry["frame"]["y"] > data["screen"]["height"] * 0.6


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--device", default="emulator-5554")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    serial = args.device
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=True)

    def pids():
        _, out, _ = sh("adb", "-s", serial, "shell", "pidof", PACKAGE)
        return set(out.split())

    baseline = pids()
    if not baseline:
        raise AssertionError("The app is not running; nothing to verify")

    def alive():
        if not baseline <= pids():
            raise CrashError(f"{PACKAGE} process {sorted(baseline)} disappeared. STOP; no relaunch.")

    printed = {"outline": None}

    def observe(allow_empty=False):
        alive()
        envelope = json.loads(run("sim-use", "ui", "--device", serial, "--json", "--no-raw"))
        if not envelope.get("ok"):
            raise RuntimeError(f"ui failed: {envelope}")
        data = envelope["data"]
        if data.get("crashDialog"):
            raise CrashError(f"System crash dialog detected: {data['crashDialog']}")
        outline = data.get("outline", "")
        if outline != printed["outline"]:
            print(outline, flush=True)
            printed["outline"] = outline
        if not data.get("entries") and not allow_empty:
            raise AssertionError("An empty accessibility tree is not a successful observation")
        return data

    def tap_entry(entry):
        # The alias is valid only for the snapshot it came from, so tap immediately.
        run("sim-use", "tap", f"@{entry['aliases']['at']}", "--device", serial)
        time.sleep(0.8)

    def wait_for(predicate, timeout=30, scroll=False, what="expected UI"):
        deadline = time.monotonic() + timeout
        scrolls = 0
        while True:
            expired = time.monotonic() >= deadline
            data = None
            try:
                data = observe(allow_empty=True)
            except CrashError:
                raise
            except (RuntimeError, AssertionError, ValueError, KeyError, subprocess.TimeoutExpired):
                data = None  # transient: window changing, activity recreating, bridge busy
            if data and data.get("entries"):
                matches = [e for e in data["entries"] if predicate(e, data)]
                if matches:
                    return data, matches
                denial = [e for e in data["entries"] if any(n in DENY for n in names(e))]
                if denial and not expired:  # optional permission prompt: decline and continue
                    tap_entry(denial[0])
                    continue
                if scroll and scrolls < 8 and not expired:
                    scrolls += 1
                    run("sim-use", "gesture", "scroll-up", "--device", serial)
                    time.sleep(1)
                    continue
            if expired:
                raise AssertionError(f"{what} was not observed within {timeout}s")
            time.sleep(1)

    def tap(predicate, what, **kwargs):
        _, matches = wait_for(predicate, what=what, **kwargs)
        matches.sort(key=lambda e: not any(k in str(e.get("role", "")).lower() for k in INTERACTIVE))
        tap_entry(matches[0])

    def screenshot(name):
        alive()
        time.sleep(0.8)  # Android's screenshot API is rate limited
        path = output / name
        run("sim-use", "screenshot", "--device", serial, "--output", str(path))
        if not path.exists() or path.stat().st_size < 1000:
            raise AssertionError(f"Screenshot {name} was not produced")
        return path

    def setting(namespace, key, value):
        run("adb", "-s", serial, "shell", "settings", "put", namespace, key, str(value))

    state = {"native": False, "paused": False}
    results = []

    def step(name, function, fatal=False):
        try:
            function()
        except CrashError:
            raise
        except Exception as error:  # noqa: BLE001 - every failure must be reported, not hidden
            results.append((name, False, str(error)))
            print(f"::error title=FAIL {name}::{escape(error)}", flush=True)
            if fatal:
                raise StepAbort(name)
        else:
            results.append((name, True, ""))
            print(f"::notice title=PASS::{name}", flush=True)

    # ---------------------------------------------------------------------------------------
    def enter_chal():
        tap(bottom_lab_nav, "bottom Lab navigation", timeout=60)
        tap(starts("chal_card"), "Chal card in the Lab list", scroll=True)
        wait_for(lambda e, _: any(n.startswith("CHAL") for n in names(e)), timeout=40, what="Chal header")
        data, _ = wait_for(has("reset_view"), timeout=150, what="Chal ready (renderer started, controls shown)")
        problem = [e for e in data["entries"] if has("render_problem")(e, data)]
        if problem:
            raise AssertionError("Chal showed its rendering-problem panel instead of a scene")
        state["native"] = any("Vulkan" in n for e in data["entries"] for n in names(e))
        print(f"Active backend: {'native Vulkan' if state['native'] else 'OpenGL compatibility'}", flush=True)

    def single_header():
        data = observe()
        all_names = [n for e in data["entries"] for n in names(e)]
        chal = [n for n in all_names if n.startswith("CHAL")]
        if len(chal) != 1:
            raise AssertionError(f"Expected exactly one Chal header, found {chal}")
        duplicates = [n for n in all_names if n == "Back" or n in LABELS["lab_nav"]]
        if duplicates:
            raise AssertionError(f"Outer Lab header/navigation is still visible: {duplicates}")
        if not any(n in LABELS["back_lab"] for n in all_names):
            raise AssertionError("The single Back-to-Lab action is missing")

    def portrait_visual():
        screenshot("chal-portrait.png")

    def pause_and_pinch():
        if state["native"]:
            data = observe()
            if any(has("pause")(e, data) for e in data["entries"]):
                raise AssertionError("The native renderer offers a Pause button that it cannot honour")
            return
        tap(has("pause"), "Pause button")
        wait_for(has("resume"), what="Resume button after pausing")
        state["paused"] = True
        before = screenshot("chal-paused-before-pinch.png")
        data = observe()
        width, height = data["screen"]["width"], data["screen"]["height"]
        run("sim-use", "gesture", "pinch-out", "--device", serial, "--center-x", str(int(width / 2)),
            "--center-y", str(int(height / 2)), "--radius", "80", "--scale", "2")
        time.sleep(1.5)
        wait_for(has("resume"), what="Resume button after pinching")
        after = screenshot("chal-paused-after-pinch.png")
        from PIL import Image, ImageChops  # installed by the workflow into a private virtualenv
        first = Image.open(before).convert("RGB")
        second = Image.open(after).convert("RGB")
        if first.size != second.size:
            raise AssertionError("Screenshot sizes differ")
        w, h = first.size
        region = (w // 8, h // 4, w * 7 // 8, h * 13 // 20)  # the scene, away from the HUD
        if ImageChops.difference(first.crop(region), second.crop(region)).getbbox() is None:
            raise AssertionError("Pinching while paused did not redraw any scene pixel")

    def rotation_keeps_the_session():
        setting("system", "accelerometer_rotation", 0)
        setting("system", "user_rotation", 1)
        try:
            wait_for(lambda e, _: any(n.startswith("CHAL") for n in names(e)), timeout=45,
                     what="Chal header after rotating (selection lost?)")
            wait_for(has("reset_view"), timeout=90, what="Chal controls after rotating")
            if state["paused"]:
                wait_for(has("resume"), what="paused state after rotating")
            screenshot("chal-landscape-restored.png")
        finally:
            setting("system", "user_rotation", 0)
        wait_for(lambda e, _: any(n.startswith("CHAL") for n in names(e)), timeout=45,
                 what="Chal header after rotating back")
        wait_for(has("reset_view"), timeout=90, what="Chal controls after rotating back")

    def large_text_controls():
        setting("system", "font_scale", 1.8)
        try:
            wait_for(has("controls"), timeout=60, what="Controls button with 1.8x text")
            tap(has("controls"), "Controls button")
            wait_for(has("tab_physics"), timeout=20, what="control sheet tabs with 1.8x text")
            tap(has("tab_system"), "System tab")
            time.sleep(1)
            screenshot("chal-large-font-controls.png")
            tap(has("close_controls"), "Close controls button")
            wait_for(has("controls"), what="Controls button after closing the sheet")
        finally:
            setting("system", "font_scale", 1.0)
            time.sleep(2)

    def hide_and_reveal():
        wait_for(has("hide_ui"), timeout=60, what="Hide-interface button")
        tap(has("hide_ui"), "Hide-interface button")
        data, _ = wait_for(has("show_ui"), what="Show-interface button")
        hidden = {n for e in data["entries"] for n in names(e)}
        leftovers = [n for n in hidden if n.startswith("CHAL") or n in LABELS["lab_nav"] or n in LABELS["back_lab"] or n == "Back"]
        if leftovers:
            raise AssertionError(f"Chrome remained visible after hiding the interface: {leftovers}")
        screenshot("chal-hidden-ui.png")
        tap(has("show_ui"), "Show-interface button")
        wait_for(lambda e, _: any(n.startswith("CHAL") for n in names(e)), what="Chal header after revealing")

    def return_to_lab():
        tap(has("back_lab"), "Back-to-Lab button")
        wait_for(bottom_lab_nav, timeout=30, what="Lab navigation after leaving Chal")
        data = observe()
        if any(n.startswith("CHAL") for e in data["entries"] for n in names(e)):
            raise AssertionError("Chal is still on screen after Back to Lab")

    try:
        step("Open Chal from Lab and wait for the renderer", enter_chal, fatal=True)
        step("Exactly one header and one back action", single_header)
        step("Portrait screenshot", portrait_visual)
        step("Pause control and paused pinch redraw", pause_and_pinch)
        step("Rotation keeps Chal, pause state and camera", rotation_keeps_the_session)
        step("1.8x text keeps the controls usable", large_text_controls)
        step("Hide and reveal the whole interface", hide_and_reveal)
        step("Back to Lab", return_to_lab)
    except StepAbort:
        pass
    finally:
        for namespace, key, value in (("system", "font_scale", 1.0), ("system", "user_rotation", 0),
                                      ("system", "accelerometer_rotation", 1)):
            try:
                setting(namespace, key, value)
            except Exception:  # noqa: BLE001 - best-effort cleanup only
                pass

    print("\n==== Chal sim-use live check summary ====", flush=True)
    for name, ok, detail in results:
        print(("PASS  " if ok else "FAIL  ") + name + (f"\n      {detail}" if detail else ""), flush=True)
    if not results or not all(ok for _, ok, _ in results):
        sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except CrashError as crash:
        print(f"::error title=Chal crashed during the live check::{escape(crash)} STOPPED; the app was not relaunched.", flush=True)
        sys.exit(2)
    except Exception as error:  # noqa: BLE001
        print(f"::error title=sim-use live check::{escape(error)}", flush=True)
        raise
