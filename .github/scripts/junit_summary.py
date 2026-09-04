#!/usr/bin/env python3
"""
T1 (gate pass): convert Gradle's JUnit XML (app/build/test-results/testDebugUnitTest/)
into (a) a markdown table in $GITHUB_STEP_SUMMARY and (b) ::notice annotations so the
per-file results are readable from the GitHub API without downloading artifacts.
"""
import glob
import os
import xml.etree.ElementTree as ET

RESULTS_GLOB = "app/build/test-results/testDebugUnitTest/*.xml"

rows = []
failures = []
for f in sorted(glob.glob(RESULTS_GLOB)):
    try:
        root = ET.parse(f).getroot()
    except Exception:
        continue
    cls = root.get("name", "?")
    n = int(root.get("tests", 0))
    fl = int(root.get("failures", 0))
    er = int(root.get("errors", 0))
    sk = int(root.get("skipped", 0))
    rows.append((cls, n, n - fl - er - sk, fl, er, sk))
    for tc in root.iter("testcase"):
        for kind in ("failure", "error"):
            for x in tc.findall(kind):
                msg = (x.get("message") or (x.text or "")).replace("\n", " ")[:420]
                failures.append((tc.get("classname", "?"), tc.get("name", "?"), kind, msg))

# 1) step summary (for humans in the browser)
summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    with open(summary, "a") as fh:
        fh.write("\n## Unit-test JUnit summary (T1 gate)\n\n")
        fh.write("| class | run | pass | fail | error | skip |\n|---|---|---|---|---|---|\n")
        for cls, n, p, fl, er, sk in rows:
            fh.write(f"| {cls} | {n} | {p} | {fl} | {er} | {sk} |\n")
        fh.write("\n### Failures / errors\n\n")
        if not failures:
            fh.write("(none)\n")
        for cn, tn, kind, msg in failures:
            fh.write(f"- **{cn}.{tn}** [{kind}]: {msg}\n")

# 2) annotations (for machines): <=10 notices with per-file lines
def notice(title, body):
    body = body.replace("%", "%25").replace("\r", "").replace("\n", "%0A").replace(":", "%3A")[:1800]
    print(f"::notice title={title}::{body}")

per_file = [f"{cls.split('.')[-1]}:r{n}/p{p}/f{fl}/e{er}/s{sk}" for cls, n, p, fl, er, sk in rows]
if not per_file:
    per_file = ["NO-XML-FOUND (compile failure? see unit-test-log artifact)"]
CH = 12
chunks = [per_file[i:i + CH] for i in range(0, len(per_file), CH)]
for i, ch in enumerate(chunks[:9]):
    notice(f"junit-files-{i + 1}", " | ".join(ch))

fail_lines = [f"{cn.split('.')[-1]}.{tn}: {msg}" for cn, tn, kind, msg in failures]
CHF = 3
fchunks = [fail_lines[i:i + CHF] for i in range(0, len(fail_lines), CHF)]
budget = 10 if len(chunks) < 2 else 10 - len(chunks[:9]) + 1
for i, ch in enumerate(fchunks[:max(budget, 0) if budget > 0 else 0]):
    notice(f"junit-failures-{i + 1}", " || ".join(ch))

print(f"summarized classes={len(rows)} failures={len(failures)}")
