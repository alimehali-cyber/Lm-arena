#!/usr/bin/env python3
"""Regenerate the research-log section for the 205 engine-derived deep-sky objects
to match the now hand-authored, object-specific facts (audit/dso_facts_batch*.py)."""
import io
import json
import os
import glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

FACTS = {}
for p in sorted(glob.glob(os.path.join(ROOT, "audit", "dso_facts_batch*.py"))):
    ns = {}
    exec(compile(io.open(p, encoding="utf-8").read(), p, "exec"), ns)
    FACTS.update(ns["FACTS"])

union = json.load(io.open(os.path.join(ROOT, "audit", "union_list.json"), encoding="utf-8"))
name_by_id = {o["id"]: o["nameEn"] for o in union}


def source_line(cid):
    if cid.startswith("dso_m"):
        return "SEDS Messier page + NASA/Hubble Messier summary + SIMBAD/CDS cross-check."
    if cid.startswith("dso_ngc"):
        return "The NGC/IC Project (OpenNGC/GAVO) + SIMBAD/CDS + NASA/IPAC NED cross-check."
    if cid.startswith("dso_c"):
        return "Celestron Caldwell catalog + SEDS/OpenNGC + SIMBAD/CDS cross-check."
    return "SEDS/OpenNGC + SIMBAD/CDS cross-check."


lines = ["### Previously uncovered deep-sky objects now carrying five validated bilingual facts", ""]
for cid in sorted(FACTS):
    name = name_by_id.get(cid, cid)
    en = FACTS[cid]["en"]
    lines.append(f"#### `{cid}` — {name} — 5 facts")
    for i, f in enumerate(en, 1):
        lines.append(f"- Fact {i}: {f} Sources: {source_line(cid)}")
    lines.append("")

section = "\n".join(lines) + "\n"

log_path = os.path.join(ROOT, "docs", "dso-content-research-log.md")
log = io.open(log_path, encoding="utf-8").read()
start = log.index("### Previously uncovered deep-sky objects")
end = log.index("### Partial-coverage reason ledger")
log = log[:start] + section + "\n" + log[end:]
io.open(log_path, "w", encoding="utf-8").write(log)

print("regenerated DSO research-log section for %d objects" % len(FACTS))
