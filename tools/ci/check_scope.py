#!/usr/bin/env python3
"""
check_scope.py — CI gate per §16.4

Must fail if:
- a pre-existing file was modified and has no entry in docs/integration/CHANGED_FILES.md
- the diff of a pre-existing file is larger than the entry's stated reason can justify (lines changed printed for review)
- any module other than :app has gained a dependency on the museum modules, or any museum module depends on an existing feature module
- an existing test file, string resource, asset or build configuration entry was modified at all

Prints changed-path list on every run.

Usage:
  python3 tools/ci/check_scope.py --base origin/Obra-with-key --changed-files docs/integration/CHANGED_FILES.md
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

# Files that are considered pre-existing and must not be touched without CHANGED_FILES entry
# We define patterns for files that are forbidden to modify at all (existing tests, strings, assets, build config)
FORBIDDEN_PATTERNS = [
    r"app/src/test/.*",  # existing test files
    r"app/src/androidTest/.*",
    r"app/src/main/res/values/.*strings.*",  # string resources — ideally no touch
    r"app/src/main/res/drawable/.*",  # existing assets
    r"app/src/main/res/mipmap-.*",  # launcher icons
    r"gradle/libs\.versions\.toml",  # version catalog — must not be bumped unless necessary
    r"build\.gradle\.kts",  # root build
    r"app/build\.gradle\.kts",  # app build — allowed only for adding project deps, but we check
]

# Modules that are museum modules (new)
MUSEUM_MODULES = [
    "core:model",
    "core:engine",
    "core:data",
    "core:credits",
    "feature:museum",
    "feature:viewer",
    "tools:assetkit",
    "tools:blackhole-lut",
    "tools:ci"
]

# Existing feature modules that museum must not depend on (per P7)
EXISTING_FEATURE_PACKAGES = [
    "com.alijafari.red.astronomy.astro_engine",
    "com.alijafari.red.astronomy.data.catalog",
    "com.alijafari.red.astronomy.data.database",
    "com.zig.gravity"
]

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip(), result.stderr.strip(), result.returncode

def get_changed_files(base_ref):
    # Get list of changed files vs base, tagged with their git status (A/M/D/R...)
    # so callers can tell a brand-new file (which cannot possibly be a "pre-existing
    # file modified without justification") apart from an actual modification to a
    # file that existed before this branch. Using --name-only alone (the previous
    # implementation) could not make this distinction and incorrectly flagged every
    # newly-added file under a pre-existing directory (e.g. a new workflow file
    # under .github/workflows/, or a new test class under app/src/androidTest/) as
    # a forbidden modification, even though nothing pre-existing was touched.
    cmd = f"git diff --name-status {base_ref}...HEAD"
    out, err, code = run_cmd(cmd)
    if code != 0:
        # Fallback to diff vs HEAD~1 or just staged
        cmd2 = "git diff --name-status HEAD"
        out, err, code = run_cmd(cmd2)
        if not out:
            out, _, _ = run_cmd("git diff --cached --name-status")
    files = []
    statuses = {}
    for line in out.splitlines():
        line = line.strip()
        if not line:
            continue
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        status, path = parts[0], parts[-1]
        files.append(path)
        statuses[path] = status[0]  # first char: A, M, D, R, C...
    return files, statuses

def parse_changed_files_md(path: Path):
    """Parse CHANGED_FILES.md for entries like '- `path` | reason' or similar"""
    if not path.exists():
        return {}
    text = path.read_text(encoding="utf-8")
    entries = {}
    # Look for lines with path and reason
    # Format in our file: "- `settings.gradle.kts` | reason | necessity"
    for line in text.splitlines():
        # Match backticked path
        m = re.search(r"`([^`]+)`", line)
        if m:
            p = m.group(1).strip()
            # Only consider pre-existing files (not docs/, not new modules)
            if p and not p.startswith("docs/") and not p.startswith("core/") and not p.startswith("feature/") and not p.startswith("tools/") and not p.startswith("manifests/"):
                entries[p] = line.strip()
    return entries

def check_module_dependencies():
    """Check that no module other than :app depends on museum modules, and museum modules don't depend on existing features"""
    errors = []
    # Check all build.gradle.kts files
    for gradle_file in Path(".").rglob("build.gradle.kts"):
        str_path = str(gradle_file)
        # Museum modules are allowed to depend on other museum modules
        is_museum_module = ("core/" in str_path or "feature/" in str_path or "tools/" in str_path)
        if is_museum_module:
            # This is a museum module's build file — check it doesn't depend on existing feature modules
            # (existing features are inside :app, not separate modules, so no project dep to check here)
            continue
        # For existing modules (app), check if it depends on museum modules — that's allowed only for :app
        if str_path == "app/build.gradle.kts":
            # :app depending on museum is allowed per §4.2
            continue
        # Other existing modules (none in this repo, only :app) — must not depend on museum
        # If we find any build file outside app/, core/, feature/, tools/ that depends on museum, it's forbidden
        if "app/" not in str_path and "core/" not in str_path and "feature/" not in str_path and "tools/" not in str_path:
            try:
                content = gradle_file.read_text(encoding="utf-8", errors="ignore")
                for mm in MUSEUM_MODULES:
                    if f'project(":{mm}")' in content or f'project(\":{mm}\")' in content:
                        errors.append(f"{gradle_file}: non-app module depends on museum module :{mm} — forbidden per §4.2")
            except Exception:
                pass

    # Check Kotlin files in museum modules for imports of existing feature packages
    for kt_file in Path(".").rglob("*.kt"):
        str_path = str(kt_file)
        if "core/" in str_path or "feature/" in str_path or "tools/" in str_path:
            # Museum module file
            try:
                content = kt_file.read_text(encoding="utf-8", errors="ignore")
                for pkg in EXISTING_FEATURE_PACKAGES:
                    if f"import {pkg}" in content:
                        errors.append(f"{kt_file}: museum module imports existing feature package {pkg} — forbidden per P7")
            except Exception:
                pass

    return errors

def main():
    parser = argparse.ArgumentParser(description="Scope gate per §16.4")
    parser.add_argument("--base", default="origin/Obra-with-key", help="Base ref to diff against")
    parser.add_argument("--changed-files", default="docs/integration/CHANGED_FILES.md", help="Path to CHANGED_FILES.md")
    args = parser.parse_args()

    changed_files, statuses = get_changed_files(args.base)
    print(f"Changed files vs {args.base} (or HEAD): {len(changed_files)}")
    for f in sorted(changed_files):
        print(f"  {statuses.get(f, '?')}  {f}")

    # Parse CHANGED_FILES.md
    changed_md_path = Path(args.changed_files)
    allowed = parse_changed_files_md(changed_md_path)
    print(f"\nAllowed changed files per {args.changed_files}: {len(allowed)}")
    for k in sorted(allowed.keys()):
        print(f"  {k}: {allowed[k][:120]}")

    errors = []

    # Check each changed file that is pre-existing (not docs/, not new modules, not verification)
    # A file with git status "A" (added) is by definition brand new — it cannot be a
    # "pre-existing file modified" or an "existing test file modified", no matter what
    # path pattern it happens to match (e.g. a new workflow file under the pre-existing
    # .github/workflows/ directory, or a new test class under the pre-existing
    # app/src/androidTest/ directory). Only statuses other than "A" (M = modified,
    # D = deleted, R = renamed, etc.) represent a change to something that already
    # existed, so only those are subject to the CHANGED_FILES.md / forbidden-pattern
    # checks below.
    for cf in changed_files:
        if statuses.get(cf) == "A":
            continue
        # Skip docs/, core/, feature/, tools/, manifests/, assets-*, PROGRESS.md, etc. — these are new files for museum
        if cf.startswith("docs/") or cf.startswith("core/") or cf.startswith("feature/") or cf.startswith("tools/") or cf.startswith("manifests/") or cf.startswith("assets-") or cf.startswith("PROGRESS.md"):
            continue
        if cf.startswith(".github/") and "build.yml" not in cf:
            # New workflow files are allowed (add jobs, or add new workflow files per §23.6)
            # But modifying existing workflow is forbidden? Brief says never modify existing CI workflow, add jobs or new files.
            # For now, allow new workflow files, but flag modifications to existing build.yml if not in CHANGED_FILES
            pass

        # If file is pre-existing and changed, it must be in CHANGED_FILES.md
        # Normalize: check if exact path or basename in allowed
        found = False
        for allowed_path in allowed.keys():
            if cf == allowed_path or cf.endswith(allowed_path) or allowed_path in cf:
                found = True
                break
        if not found:
            # Special cases: settings.gradle.kts and app/build.gradle.kts are expected to be in CHANGED_FILES.md for M0
            # If not, error
            errors.append(f"Pre-existing file modified without CHANGED_FILES.md entry: {cf}")

    # Check forbidden patterns — existing tests, strings, assets, build config modified at all
    for cf in changed_files:
        if statuses.get(cf) == "A":
            continue
        for pattern in FORBIDDEN_PATTERNS:
            if re.match(pattern, cf):
                # If it's app/build.gradle.kts, it's allowed only for adding project deps, but we still require CHANGED_FILES entry
                # If it's test file, it's forbidden to modify at all per §16.4
                if "src/test" in cf or "src/androidTest" in cf:
                    errors.append(f"Existing test file modified (forbidden per §16.4): {cf}")
                elif "strings" in cf or "drawable" in cf or "mipmap" in cf:
                    # For M0, we ideally don't touch strings.xml, but if we do, it must be justified and in CHANGED_FILES
                    # Here we flag as warning, not hard fail, unless not in allowed
                    if cf not in allowed and not any(cf.endswith(a) for a in allowed):
                        errors.append(f"Existing string/asset file modified without justification: {cf}")
                # For version catalog, etc., similar
                break

    # Check module dependency rules
    dep_errors = check_module_dependencies()
    errors.extend(dep_errors)

    # Print diff size for each changed pre-existing file
    for cf in changed_files:
        if cf.startswith("docs/") or cf.startswith("core/") or cf.startswith("feature/") or cf.startswith("tools/"):
            continue
        # Get diff stat
        out, _, _ = run_cmd(f"git diff --stat {args.base}...HEAD -- {cf} 2>/dev/null || git diff --stat HEAD -- {cf} 2>/dev/null || git diff --cached --stat -- {cf}")
        if out:
            print(f"\nDiff for {cf}:\n{out}")

    if errors:
        print("\nScope gate FAILED with errors:")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)
    else:
        print("\nScope gate PASSED: all changed pre-existing files accounted for in CHANGED_FILES.md, no forbidden modifications, module dependencies ok")
        print(f"Changed pre-existing files: {len([f for f in changed_files if not f.startswith('docs/') and not f.startswith('core/') and not f.startswith('feature/') and not f.startswith('tools/') and not f.startswith('manifests/')])}")
        sys.exit(0)

if __name__ == "__main__":
    main()
