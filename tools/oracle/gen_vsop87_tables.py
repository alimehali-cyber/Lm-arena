#!/usr/bin/env python3
"""F-A5 generator: regenerates the VSOP87 term tables inside VSOP87Engine.kt from the
official VSOP87D series via the independent pymeeus data files (LGPL, machine-generated
from the original vsop87.dat), scaled to JULIAN CENTURIES (the engine's time argument).

Per-level amplitude floors chosen so the truncated series stays within ~1 arcsecond of
the full series over 1992-2030 (verified below + in the oracle diff).

Usage: python3 tools/oracle/gen_vsop87_tables.py   (prints verification; rewrites the
`private val <planet>L0/B0/R0...` arrays inside VSOP87Engine.kt)
"""
import re, math, sys
from pymeeus import Mercury, Venus, Earth, Mars, Jupiter, Saturn, Uranus, Neptune

MODULES = {"Mercury": Mercury, "Venus": Venus, "Earth": Earth, "Mars": Mars,
           "Jupiter": Jupiter, "Saturn": Saturn, "Uranus": Uranus, "Neptune": Neptune}
# amplitude floors per series letter and level, in 1e-8 rad (or 1e-8 AU for R)
FLOORS = {
    "L": {0: 100.0, 1: 100.0, 2: 60.0, 3: 30.0, 4: 10.0, 5: 5.0},
    "B": {0: 40.0, 1: 40.0, 2: 10.0, 3: 5.0, 4: 2.0},
    "R": {0: 100.0, 1: 60.0, 2: 20.0, 3: 10.0, 4: 5.0, 5: 2.0},
}
# the engine only HAS L0/L1/B0/R0 slots per planet (no higher levels)
ENGINE_LEVELS = {"L": (0, 1), "B": (0,), "R": (0,)}
KT = "app/src/main/java/com/alijafari/red/astronomy/astro_engine/VSOP87Engine.kt"

def parse_pymeeus(module, letter):
    src = open(module.__file__).read()
    m = re.search(rf"VSOP87_{letter} = \[\n(.*?)\n\]", src, re.S)
    parts = re.split(r"# (L\d|B\d|R\d)", m.group(1))
    out = {}
    for i in range(1, len(parts), 2):
        lvl = int(parts[i][1])
        terms = re.findall(r"\[([-\d.eE+]+),\s*([-\d.eE+]+),\s*([-\d.eE+]+)\]", parts[i+1])
        out[lvl] = [(float(a), float(b), float(c)) for a, b, c in terms]
    return out

def to_century(terms, level):
    # pymeeus: tau in 100 Julian centuries; engine t in centuries => t = 10*tau
    # A_i tau^i = A_i (t/10)^i -> coefficient of t^i is A_i / 10^i; C scaled /10.
    return [(a / (10.0 ** level), b, c / 10.0) for a, b, c in terms]

def truncate(terms, floor):
    return [t for t in terms if abs(t[0]) >= floor]

def fmt(v):
    s = f"{v:.10g}"
    if "e" not in s and "E" not in s and "." not in s:
        s += ".0"
    return s

def kotlin_block(name, terms):
    lines = [f"    private val {name} = arrayOf("]
    for a, b, c in terms:
        lines.append(f"        Term({fmt(a)}, {fmt(b)}, {fmt(c)}),")
    lines.append("    )")
    return "\n".join(lines), len(terms)

def evaluate(series_by_level, t):
    # series_by_level: {level: [(A,B,C)]}; engine formula: sum_i sumSeries(Li)*t^i
    tot = 0.0
    for i, terms in sorted(series_by_level.items()):
        s = sum(a * math.cos(b + c * t) for a, b, c in terms)
        tot += s * t ** i
    return tot

def main():
    newsrc = open(KT).read()
    total_terms = 0
    verification = []
    for planet, mod in MODULES.items():
        for letter in "LBR":
            full = parse_pymeeus(mod, letter)
            maxlvl = max(full)
            for lvl in ENGINE_LEVELS[letter]:
                if lvl > maxlvl:
                    continue
                terms_c = to_century(full[lvl], lvl)
                kept = truncate(terms_c, FLOORS[letter].get(lvl, 2.0))
                name = f"{planet.lower()}{letter}{lvl}"
                blk, n = kotlin_block(name, kept)
                total_terms += n
                # replace existing block
                pat = re.compile(rf"    private val {name} = arrayOf\((?:.|\n)*?\n    \)")
                if not pat.search(newsrc):
                    print(f"MISSING BLOCK {name}", file=sys.stderr)
                    continue
                newsrc = pat.sub(blk.replace("\\", "\\\\"), newsrc, count=1)
            # verification vs full series over 1992-2030
        if letter == "L":
            pass
    # verify L,B,R truncated vs full for each planet at sample epochs
    import datetime
    epochs = [(1992.8,), (2000.0,), (2010.5,), (2026.7,), (2030.5,)]
    worst = 0.0
    for planet, mod in MODULES.items():
        for letter, unit in (("L", "arcsec"), ("B", "arcsec"), ("R", "AU")):
            full = {l: to_century(v, l) for l, v in parse_pymeeus(mod, letter).items()}
            trunc = {l: truncate(v, FLOORS[letter].get(l, 2.0)) for l, v in full.items() if l in ENGINE_LEVELS[letter]}
            for (yr,) in epochs:
                t = (yr - 2000.0) / 100.0
                d = evaluate(full, t) - evaluate(trunc, t)
                if letter in "LB":
                    err = abs(d) * 1e-8 * 206264.806  # arcsec
                else:
                    err = abs(d) * 1e-8  # AU
                scale = 1.0 if letter in "LB" else 206265 * 1.496e8  # AU -> arcsec-ish parallax? keep AU
                worst = max(worst, err if letter in "LB" else err * 1e3)  # mAU
                verification.append((planet, letter, yr, err))
    lb = [v for p, l, y, v in verification if l in "LB"]
    r_ = [v for p, l, y, v in verification if l == "R"]
    print(f"truncation error vs full VSOP87D, 1992-2030: worst L/B = {max(lb):.3f} arcsec; worst R = {max(r_)*1e3:.1f} mAU")
    print(f"total emitted terms: {total_terms}")
    open(KT, "w").write(newsrc)
    print("VSOP87Engine.kt tables regenerated")

if __name__ == "__main__":
    main()
