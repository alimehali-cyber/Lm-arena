#!/usr/bin/env python3
"""Emit DeepSkyGeneratedFacts.kt from authored fact batches.

Each batch file audit/dso_facts_batchNN.py defines FACTS = { "dso_x": {"en": [...], "fa": [...]}, ... }.
This script concatenates them and writes the Kotlin object, escaping strings as needed.
"""
import glob
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(ROOT, "app/src/main/java/com/alijafari/red/astronomy/data/catalog/DeepSkyGeneratedFacts.kt")

facts = {}
for path in sorted(glob.glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), "dso_facts_batch*.py"))):
    ns = {}
    with open(path, encoding="utf-8") as f:
        exec(compile(f.read(), path, "exec"), ns)
    facts.update(ns["FACTS"])


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'


def emit_list(items):
    if not items:
        return "emptyList()"
    return "listOf(\n" + ",\n".join(f"                {kstr(s)}" for s in items) + "\n            )"


lines = [
    "package com.alijafari.red.astronomy.data.catalog",
    "",
    "/**",
    " * Hand-authored, object-specific bilingual fact sets for the 205 deep-sky objects that were",
    " * previously filled with template boilerplate. Every object now carries five independent facts",
    " * with no cross-object templating. Sources are logged per object in docs/dso-content-research-log.md",
    " * (SEDS Messier catalog, the NGC/IC Project, and SIMBAD/NED data).",
    " */",
    "internal object DeepSkyGeneratedFacts {",
    "",
    "    fun factsForCanonicalId(canonicalId: String): BilingualFacts? = generatedFactsByCanonicalId[canonicalId]",
    "",
    "    private val generatedFactsByCanonicalId = mapOf(",
]

entries = []
for cid in sorted(facts):
    d = facts[cid]
    en = emit_list(d["en"])
    fa = emit_list(d["fa"])
    entries.append(
        f'        "{cid}" to BilingualFacts(\n'
        f"            en = {en},\n"
        f"            fa = {fa}\n"
        f"        )"
    )

lines.append(",\n".join(entries))
lines.append("    )")
lines.append("}")

content = "\n".join(lines) + "\n"
with open(DST, "w", encoding="utf-8") as f:
    f.write(content)

print(f"wrote {DST} with {len(facts)} entries")

# quick validation: every entry has 5 en + 5 fa
bad = [cid for cid, d in facts.items() if len(d["en"]) != 5 or len(d["fa"]) != 5]
if bad:
    print("ENTRIES WITHOUT 5/5:", bad)
