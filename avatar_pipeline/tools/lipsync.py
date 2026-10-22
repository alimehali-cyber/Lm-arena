#!/usr/bin/env python3
"""
Stage 6b - speech to visemes, offline, no cloud service.

Two modes:
  energy    Built-in fallback. Measures the amplitude envelope of a WAV and drives an
            open/close mouth set. Timing is real; phoneme identity is not claimed.
            This is the mode that works for any language, including Persian.
  rhubarb   Reads Rhubarb Lip Sync output (JSON or TSV) and maps its 9 mouth shapes
            onto the rig's visemes. Rhubarb's recogniser is English-only, so for
            Persian it produces generic shapes; the path exists so a *corrected*
            phoneme file can be dropped in later without changing the rig.

Output: a neutral timeline the Blender animator and the Android runtime both read.
Speech timing and facial controls stay separable on purpose.

  python tools/lipsync.py --audio voice.wav --out visemes.json --mode energy
  python tools/lipsync.py --rhubarb-json rhubarb.json --out visemes.json
"""
from __future__ import annotations
import argparse, json, math, pathlib, struct, wave

RHUBARB_MAP = {"A": "viseme_AA", "B": "viseme_MBP", "C": "viseme_E", "D": "viseme_AA",
               "E": "viseme_O", "F": "viseme_U", "G": "viseme_MBP", "H": "viseme_AA",
               "X": None}


def read_wav(path):
    """(sample_rate, mono float samples). 16- and 32-bit PCM."""
    with wave.open(str(path), "rb") as w:
        n, sr, ch, sw = w.getnframes(), w.getframerate(), w.getnchannels(), w.getsampwidth()
        raw = w.readframes(n)
    if sw == 2:
        vals = struct.unpack("<%dh" % (len(raw) // 2), raw); scale = 32768.0
    elif sw == 4:
        vals = struct.unpack("<%di" % (len(raw) // 4), raw); scale = 2147483648.0
    else:
        raise SystemExit(f"unsupported sample width {sw * 8} bit")
    if ch > 1:
        vals = [sum(vals[i:i + ch]) / ch for i in range(0, len(vals), ch)]
    return sr, [v / scale for v in vals]


def energy_timeline(wav, fps=24, floor_db=-42.0, ceil_db=-12.0):
    sr, s = read_wav(wav)
    win = max(1, int(sr / fps))
    frames = []
    for i in range(0, len(s), win):
        chunk = s[i:i + win]
        if not chunk:
            break
        rms = math.sqrt(sum(v * v for v in chunk) / len(chunk))
        db = 20 * math.log10(rms) if rms > 1e-9 else -120.0
        frames.append(max(0.0, min(1.0, (db - floor_db) / (ceil_db - floor_db))))
    out = []
    for i, v in enumerate(frames):
        a = frames[i - 1] if i else v
        b = frames[i + 1] if i + 1 < len(frames) else v
        out.append(round((a + 2 * v + b) / 4, 4))     # smooth: a mouth must not strobe
    return out, len(s) / sr


def rhubarb_parse(path):
    txt = path.read_text(errors="replace")
    events = []
    if txt.lstrip().startswith("{"):
        for m in json.loads(txt).get("mouthCues", []):
            events.append((float(m["start"]), float(m["end"]), m["value"]))
    else:
        for line in txt.splitlines():
            parts = line.split("\t")
            if len(parts) >= 3:
                try:
                    events.append((float(parts[0]), float(parts[1]), parts[2].strip()))
                except ValueError:
                    continue
    return events


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--audio")
    ap.add_argument("--rhubarb-json")
    ap.add_argument("--out", required=True)
    ap.add_argument("--mode", default="energy", choices=["energy", "rhubarb"])
    ap.add_argument("--fps", type=int, default=24)
    a = ap.parse_args()

    if a.rhubarb_json:
        keys, rest = [], 0
        for start, end, shape in rhubarb_parse(pathlib.Path(a.rhubarb_json)):
            vis = RHUBARB_MAP.get(shape.upper())
            if vis is None:
                rest += 1          # "X" is silence: no morph, the mouth simply rests
                continue
            keys.append({"t": round(start, 3), "t_end": round(end, 3), "shape": shape,
                         "viseme": vis, "weight": 1.0})
        result = {"mode": "rhubarb", "source": a.rhubarb_json,
                  "viseme_count": len(keys), "rest_intervals_dropped": rest,
                  "neutral_fallback": not keys, "keys": keys,
                  "note": "Rhubarb's recogniser is English-only. Persian phoneme identity "
                          "is NOT claimed; supply a corrected file to improve it."}
    else:
        if not a.audio:
            raise SystemExit("--audio is required for --mode energy")
        env, dur = energy_timeline(pathlib.Path(a.audio), fps=a.fps)
        keys = []
        for i, v in enumerate(env):
            vis = None if v < 0.06 else ("viseme_MBP" if v < 0.22 else
                                         ("viseme_E" if (i // 4) % 2 else "viseme_AA"))
            if vis is None:
                continue           # below the noise floor: keep the mouth at rest
            keys.append({"frame": i + 1, "t": round(i / a.fps, 3), "viseme": vis,
                         "weight": round(v, 3)})
        result = {"mode": "energy", "source": a.audio, "duration_s": round(dur, 3),
                  "fps": a.fps, "viseme_count": len(keys),
                  "neutral_fallback": not keys, "keys": keys,
                  "note": "Amplitude envelope only: language-agnostic timing, no phoneme "
                          "claim. Use --rhubarb-json for phoneme-accurate speech."}

    out = pathlib.Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, indent=2))
    print(json.dumps({k: v for k, v in result.items() if k != "keys"}, indent=2))


if __name__ == "__main__":
    main()
