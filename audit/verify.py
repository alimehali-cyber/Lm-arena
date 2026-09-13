#!/usr/bin/env python3
"""Mirror of ContentIntegrityAuditTest (Kotlin) so the audit can be iterated
without a JVM. Every check corresponds 1:1 to a @Test in the Kotlin class.
Exit code 0 = all checks pass. Also prints progress (bucket counts)."""
import json
import os
import re
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
data = json.load(open(os.path.join(ROOT, "audit", "union_list.json"), encoding="utf-8"))

BANNED = [
    "ZIG astronomical catalog",
    "Cataloged as a prominent deep-sky photometric target",
    "Instantaneous celestial coordinates",
    "Complete equatorial and scientific designations",
    "کاتالوگ زیگ",
    "موقعیت لحظه‌ای",
    "اطلاعات مختصات",
    "in messier's comet-hunting context",
    "is a milky way disk cluster whose stars formed together",
    "wide-field views help show",
    "gaia-era astrometry improves membership checks",
    "the ngc identity of",
    "proper-motion studies of",
    "rewards transparent dark skies",
    "preserve redshift, photometry, and multiwavelength identifiers",
    "belongs to the milky way globular-cluster population",
    "the crowded core of",
    "increasing aperture changes",
    "color-magnitude diagrams of",
    "nebula filters can improve contrast",
    "spiral-disk structure lets astronomers study",
    "point to a recognizable structure",
    "is a dying-star shell",
    "traces ionized oxygen",
    "multiwavelength images of",
    "emphasize old stellar populations",
    "ionized gas in",
    "as part of the virgo galaxy environment",
    "narrowband imaging of",
    "a deliberate non-messier showpiece",
    "scattering nearby starlight from dust grains",
    "reflection-dominated, dark skies usually help",
    "best understood as a stellar association",
    "can funnel gas inward",
    "lets photometric studies separate",
    "trace how milky way disk clusters",
]

LATIN_WORD = re.compile(r"[A-Za-z][A-Za-z'.\-]*")
ALLOWED_NAME_LATIN = re.compile(r"(?i)\b(M\s*\d+[A-Z]?|NGC\s*\d+[A-Z]?|IC\s*\d+[A-Z]?|C\s*\d+[A-Z]?|Melotte\s*\d+|LMC|SMC|NC)\b")
LATIN_LETTER = re.compile(r"[A-Za-z]")

DEEP_TYPES = {"DEEP_SKY", "GALAXY", "NEBULA", "STAR_CLUSTER", "GLOBULAR_CLUSTER", "BLACK_HOLE"}

log = open(os.path.join(ROOT, "docs/dso-content-research-log.md"), encoding="utf-8").read()


def facts_en(o):
    return o["factsEn"]


def facts_fa(o):
    return o["factsFa"]


def skeleton(fact, o):
    f = fact
    tokens = [o["nameEn"].split("(")[0].strip(), o["nameFa"].split("(")[0].strip()]
    tokens += list(o.get("catalogDesignations", []))
    for d in ("messierId", "ngcId", "caldwellId"):
        if o.get(d):
            tokens.append(o[d])
    for t in {t for t in tokens if t and len(t) >= 2}:
        f = re.sub(re.escape(t), "§", f, flags=re.I)
    return re.sub(r"\s+", " ", f.strip().lower())


failures = []
checks = {}

def check(name, bad):
    checks[name] = len(bad)
    if bad:
        failures.append((name, bad))


# 1. union size
check("union_size_336", [] if len(data) == 336 else ["total=%d" % len(data)])

# 2. zero-fact (bucket D)
check("no_zero_facts", [o["id"] for o in data if not facts_en(o) or not facts_fa(o)])

# 3. bilingual parity + range 1..5
mismatch = [o["id"] for o in data if len(facts_en(o)) != len(facts_fa(o))]
check("bilingual_parity", mismatch)
out_of_range = [o["id"] for o in data if len(facts_en(o)) not in range(1, 6) or len(facts_fa(o)) not in range(1, 6)]
check("fact_count_1_to_5", out_of_range)

# 4. banned/generic text
banned_hits = []
for o in data:
    for f in facts_en(o) + facts_fa(o):
        for b in BANNED:
            if b.lower() in f.lower():
                banned_hits.append("%s: %s" % (o["id"], f[:80]))
                break
check("no_generic_text", banned_hits)

# 5. cross-object skeleton duplication
owners = defaultdict(set)
for o in data:
    for f in facts_en(o):
        owners[skeleton(f, o)].add(o["id"])
dup = ["[%s] %s" % (",".join(sorted(v)), k[:80]) for k, v in owners.items() if len(v) > 1]
check("no_cross_object_duplicates", dup)

# 6. research log tracking (every object)
check("research_log_tracked", [o["id"] for o in data if "`%s`" % o["id"] not in log])

# 7. sparse (<5) must have logged reason
sparse_unlogged = [o["id"] for o in data if len(facts_en(o)) in range(1, 5) and "`%s`: Fewer than five" % o["id"] not in log]
check("sparse_logged_reason", sparse_unlogged)

# 8. rendered facts non-empty (funFacts||verifiedFacts; PhysicalData fallback == canonical for non-deep-sky)
#    -> equivalent to "no zero facts" for canonical facts; kept as its own check.
check("rendered_facts_nonempty", [o["id"] for o in data if not facts_en(o)])

# 9. facts card in section plan (factCount>0 -> present)
check("facts_card_present", [o["id"] for o in data if len(facts_en(o)) == 0])

# 10. satellite fact propagation (norad -> sat_<id>)
sat = {
    "sat_25544", "sat_48274", "sat_20580", "sat_27386",
}
dropped = [s for s in sat if not any(o["id"] == s and facts_en(o) for o in data)]
check("satellite_facts_propagated", dropped)

# 11. nameFa latin leakage
name_leaks = []
for o in data:
    stripped = ALLOWED_NAME_LATIN.sub("", o["nameFa"])
    if LATIN_LETTER.search(stripped):
        name_leaks.append("%s: %s" % (o["id"], o["nameFa"]))
check("persian_names_clean", name_leaks)

# 12. factFa English sentences (3+ latin words length>1)
fact_leaks = []
for o in data:
    for f in facts_fa(o):
        run = [w for w in LATIN_WORD.findall(f) if len(w) > 1]
        if len(run) >= 3:
            fact_leaks.append("%s: %s" % (o["id"], f[:80]))
check("persian_facts_no_english_sentences", fact_leaks)

# summary
print("== ContentIntegrityAuditTest mirror ==")
total_bad_objects = set()
for name, bad in failures:
    print(f"  FAIL {name}: {len(bad)}")
    for b in bad[:12]:
        print("       ", b)
    # collect object ids
    for b in bad:
        total_bad_objects.add(b.split(":")[0].split(" ")[0].split(",")[0])

# bucket counts (current state)
zero = set(o["id"] for o in data if not facts_en(o))
print("\nbucket D (zero facts):", len(zero))
print("objects failing >=1 check:", len(total_bad_objects))
print("objects passing all checks:", len(data) - len(total_bad_objects))
print("PASS" if not failures else "FAIL (exit 1)")
import sys
sys.exit(1 if failures else 0)
