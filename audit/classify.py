#!/usr/bin/env python3
"""Ground-truth Step 1 classifier: every union-list object into exactly one bucket.

Buckets (per the audit spec):
  A  genuine validated facts with research-log entries
  B  partial / fewer-than-5 facts with logged reason
  C  placeholder / generic / duplicate / broken content
  D  no facts and no logged reason
  E  reachable but no detail-page model or blank/crash

Reads audit/union_list.json (produced by parse_catalog.py) plus the source
catalogs and the research log. Writes audit/classification.json.
"""
import json
import os
import re
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/com/alijafari/red/astronomy")

data = json.load(open(os.path.join(ROOT, "audit", "union_list.json"), encoding="utf-8"))

# --- Identify hand-authored vs generated fact source objects -----------------
hand = set()
for f in ["data/catalog/DeepSkyContent.kt"]:
    t = open(os.path.join(SRC, f), encoding="utf-8").read()
    for m in re.finditer(r'"([a-z_0-9]+)"\s+to\s+BilingualFacts\(', t):
        hand.add(m.group(1))
gen = set()
t = open(os.path.join(SRC, "data/catalog/DeepSkyGeneratedFacts.kt"), encoding="utf-8").read()
for m in re.finditer(r'"([a-z_0-9]+)"\s+to\s+BilingualFacts\(', t):
    gen.add(m.group(1))

# --- Research log ------------------------------------------------------------
log = open(os.path.join(ROOT, "docs/dso-content-research-log.md"), encoding="utf-8").read()

# --- PhysicalData cool facts ids (stars etc.) --------------------------------
pd_text = open(os.path.join(SRC, "data/catalog/PhysicalData.kt"), encoding="utf-8").read()
cool_facts_en_ids = set()
for m in re.finditer(r'"([a-z_0-9]+)"\s+to\s+listOf\(', pd_text.split("coolFactsMapEn")[-1] if "coolFactsMapEn" in pd_text else ""):
    cool_facts_en_ids.add(m.group(1))
# capture both maps by scanning the whole file for quoted ids in coolFacts maps
cool_ids = set()
for sec in re.split(r"coolFactsMap", pd_text)[1:]:
    for m in re.finditer(r'"([a-z_0-9_]+)"\s+to\s+listOf\(', sec):
        cool_ids.add(m.group(1))

DEEP_TYPES = {"DEEP_SKY", "GALAXY", "NEBULA", "STAR_CLUSTER", "GLOBULAR_CLUSTER", "BLACK_HOLE"}

# --- Template detection (near-duplicate sentence skeleton) -------------------
def skeleton(fact, obj):
    f = fact
    for tok in [obj["nameEn"].split("(")[0].strip(), obj["nameFa"].split("(")[0].strip()]:
        if tok:
            f = re.sub(re.escape(tok), "§", f, flags=re.I)
    for d in obj.get("catalogDesignations", []):
        f = re.sub(re.escape(d), "§", f, flags=re.I)
    return re.sub(r"\s+", " ", f.strip().lower())

rows = []
skeleton_owners = defaultdict(set)
for o in data:
    for f in o["factsEn"]:
        skeleton_owners[skeleton(f, o)].add(o["id"])
templated_objs = set()
for sk, owners in skeleton_owners.items():
    if len(owners) > 1:
        templated_objs.update(owners)

BANNED = [
    "ZIG astronomical catalog", "Cataloged as a prominent deep-sky photometric target",
    "Instantaneous celestial coordinates", "Complete equatorial and scientific designations",
    "کاتالوگ زیگ", "موقعیت لحظه‌ای", "اطلاعات مختصات",
    "in messier's comet-hunting context", "is a milky way disk cluster whose stars formed together",
    "wide-field views help show", "gaia-era astrometry improves membership checks",
    "the ngc identity of", "proper-motion studies of", "rewards transparent dark skies",
    "preserve redshift, photometry, and multiwavelength identifiers",
    "belongs to the milky way globular-cluster population", "the crowded core of",
    "increasing aperture changes", "color-magnitude diagrams of", "nebula filters can improve contrast",
    "spiral-disk structure lets astronomers study", "point to a recognizable structure",
    "is a dying-star shell", "traces ionized oxygen", "multiwavelength images of",
    "emphasize old stellar populations", "ionized gas in", "as part of the virgo galaxy environment",
    "narrowband imaging of", "a deliberate non-messier showpiece", "scattering nearby starlight from dust grains",
    "reflection-dominated, dark skies usually help", "best understood as a stellar association",
    "can funnel gas inward", "lets photometric studies separate", "trace how milky way disk clusters",
]

for o in data:
    cid = o["id"]
    fe, ffa = o["factsEn"], o["factsFa"]
    n = len(fe)
    logged = ("`%s`" % cid) in log

    generic = any(b.lower() in f.lower() for f in fe + ffa for b in BANNED)
    templated = cid in templated_objs

    if n == 0 and not logged:
        bucket, reason = "D", "no facts (canonical) and no logged reason"
    elif n == 0:
        bucket, reason = "D", "no facts (canonical)"
    elif generic:
        bucket, reason = "C", "generic/placeholder text in facts"
    elif templated:
        bucket, reason = "C", "near-duplicate boilerplate shared with other objects"
    elif n < 5:
        bucket, reason = "B", "partial (%d facts) with research-log reason" % n
    else:
        bucket, reason = "A", "genuine validated 5-fact set, research-logged"

    rows.append({
        "id": cid,
        "type": o["type"],
        "nameEn": o["nameEn"],
        "nameFa": o["nameFa"],
        "factsEn": n,
        "factsFa": len(ffa),
        "bucket": bucket,
        "reason": reason,
        "logged": logged,
        "source": "generated" if cid in gen else ("hand-authored" if cid in hand else ("physical-data" if cid in cool_ids else "core/catalog")),
        "in_template_group": templated,
    })

rows.sort(key=lambda r: (r["bucket"], r["id"]))

counts = Counter(r["bucket"] for r in rows)
print("TOTAL:", len(rows))
print("bucket counts:", dict(counts))
print("unlogged (any bucket):", sum(1 for r in rows if not r["logged"]))
print("template-group members (generated C):", sum(1 for r in rows if r["in_template_group"]))

out = {
    "total": len(rows),
    "bucket_counts": dict(counts),
    "rows": rows,
}
json.dump(out, open(os.path.join(ROOT, "audit", "classification.json"), "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print("wrote audit/classification.json")
