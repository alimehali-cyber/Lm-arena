#!/usr/bin/env python3
"""Optional macOS/Android live check: observe → act → verify; never relaunch after a crash."""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess
import time

PACKAGE = "com.alijafari.red.astronomy"


def run(*args):
    result = subprocess.run(args, text=True, capture_output=True, timeout=60)
    combined = result.stdout + result.stderr
    if "PROCESS DISAPPEARED" in combined or "CRASH DIALOG DETECTED" in combined:
        raise RuntimeError("CRASH: automation stopped; do not relaunch. " + combined)
    if result.returncode:
        raise RuntimeError(f"Command failed: {args}\n{combined}")
    return result.stdout


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--device", default="emulator-5554")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    serial = args.device
    initial_pid = run("adb", "-s", serial, "shell", "pidof", PACKAGE).strip()
    assert initial_pid, "The test app was not launched"

    def alive():
        current = run("adb", "-s", serial, "shell", "pidof", PACKAGE).strip()
        if current != initial_pid:
            raise RuntimeError(f"CRASH/process disappearance: {PACKAGE}, PID {initial_pid} → {current!r}. STOP; no relaunch.")

    def observe():
        alive()
        envelope = json.loads(run("sim-use", "ui", "--device", serial, "--json", "--no-raw"))
        assert envelope.get("ok"), envelope
        data = envelope["data"]
        entries = data.get("entries", [])
        for entry in entries:
            resource = str(entry.get("resource_id", "")) + str(entry.get("uniqueId", ""))
            if "aerr_close" in resource or "aerr_app_info" in resource:
                raise RuntimeError("CRASH DIALOG DETECTED. STOP; no relaunch.")
        print(data.get("outline", ""), flush=True)
        assert entries, "An empty accessibility tree is not a successful observation"
        return data

    def wait_for(predicate, timeout=25):
        until = time.monotonic() + timeout
        while True:
            data = observe()
            matches = [e for e in data["entries"] if predicate(e, data)]
            if matches:
                return data, matches
            if time.monotonic() >= until:
                raise AssertionError("Expected UI was not observed before timeout")
            # Android has no selector --wait-timeout; refresh aliases after each transition.
            time.sleep(1)

    def exact(*labels):
        return lambda e, _: e.get("label", "").strip() in labels

    def tap(predicate):
        _, entries = wait_for(predicate)
        # Prefer the explicitly interactive node; select from THIS observation's cache.
        entries.sort(key=lambda e: e.get("role", "") not in ("Button", "Switch", "Tab", "RadioButton"))
        aliases = entries[0].get("aliases", {})
        alias = aliases.get("at")
        if alias is None:
            raise AssertionError(f"Missing fresh @N alias: {entries[0]}")
        if isinstance(alias, int):
            alias = f"@{alias}"
        alive()
        run("sim-use", "tap", str(alias), "--device", serial)
        time.sleep(0.7)
        return observe()

    def screenshot(name):
        alive()
        time.sleep(0.7)  # Android screenshot API rate limit.
        path = output / name
        run("sim-use", "screenshot", "--device", serial, "--output", str(path))
        assert path.exists() and path.stat().st_size > 1000
        return path

    def back_to_app_permission_check(data):
        # System permission dialogs may cover navigation but are not a crash. Deny optional access.
        for _ in range(3):
            denial = [e for e in data["entries"] if e.get("label", "").strip() in ("Don't allow", "DON’T ALLOW", "عدم اجازه", "اجازه ندادن")]
            if not denial:
                return data
            data = tap(exact(denial[0]["label"].strip()))
        return data

    data = back_to_app_permission_check(observe())
    tap(lambda e, d: e.get("label", "").strip() in ("Lab", "آزمایشگاه") and e["frame"]["y"] > d["screen"]["height"] * 0.65)
    tap(lambda e, _: e.get("label", "").strip().startswith(("Chal", "چال")))
    wait_for(exact("CHAL"))
    wait_for(lambda e, _: "Preparing renderer" not in e.get("label", "") and e.get("label", "").strip() in ("Pause", "توقف"))
    screenshot("chal-portrait.png")

    # The compatibility renderer is active on an x86 emulator; it must have a real pause action.
    tap(exact("Pause", "توقف"))
    wait_for(exact("Resume", "ادامه"))
    before = screenshot("chal-paused-before-pinch.png")
    data = observe()
    width, height = data["screen"]["width"], data["screen"]["height"]
    run("sim-use", "gesture", "pinch-out", "--device", serial, "--center-x", str(int(width / 2)),
        "--center-y", str(int(height / 2)), "--radius", "80", "--scale", "2")
    time.sleep(1)
    wait_for(exact("Resume", "ادامه"))
    after = screenshot("chal-paused-after-pinch.png")
    assert hashlib.sha256(before.read_bytes()).digest() != hashlib.sha256(after.read_bytes()).digest(), "Paused pinch did not redraw the scene"

    # Configuration recreation must retain Chal selection and paused state, not return to Lab/Home.
    run("adb", "-s", serial, "shell", "settings", "put", "system", "accelerometer_rotation", "0")
    run("adb", "-s", serial, "shell", "settings", "put", "system", "user_rotation", "1")
    time.sleep(2)
    wait_for(exact("CHAL"))
    wait_for(exact("Resume", "ادامه"))
    screenshot("chal-landscape-restored.png")
    run("adb", "-s", serial, "shell", "settings", "put", "system", "font_scale", "1.8")
    time.sleep(2)
    wait_for(exact("CHAL"))
    tap(exact("Controls", "کنترل‌ها"))
    wait_for(exact("Physics", "فیزیک"))
    screenshot("chal-large-font-controls.png")
    tap(exact("Close controls", "بستن کنترل‌ها"))

    # Fullscreen has no outer Lab header/back action. The one reveal button stays accessible.
    tap(exact("Hide interface", "پنهان کردن رابط"))
    data, _ = wait_for(exact("Show interface", "نمایش رابط"))
    assert not any(e.get("label", "").strip() in ("CHAL", "Lab", "آزمایشگاه", "Back to Lab", "بازگشت به آزمایشگاه") for e in data["entries"])
    screenshot("chal-hidden-ui.png")
    tap(exact("Show interface", "نمایش رابط"))
    wait_for(exact("CHAL"))
    tap(exact("Back to Lab", "بازگشت به آزمایشگاه"))
    wait_for(lambda e, _: e.get("label", "").strip().startswith(("Chal", "چال")))
    observe()
    print("PASS: Chal rendering, paused pinch, recreation, large text, hide/reveal and return-to-Lab checked with sim-use.", flush=True)


if __name__ == "__main__":
    main()
