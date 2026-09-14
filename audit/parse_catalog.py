#!/usr/bin/env python3
"""Ground-truth audit harness for the ZIG astronomy app (v2).

Faithfully ports the deterministic catalog-merge logic in
CanonicalAstroCatalog.kt (token-based identity merge + coordinate dedup)
against the actual Kotlin source, then classifies every reachable object.
"""
import re
import json
import os
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "java", "com", "alijafari", "red", "astronomy")


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def parse_kotlin_string_list(text):
    i = text.find("listOf(")
    if i < 0:
        return []
    i += len("listOf(")
    depth = 1
    j = i
    in_str = False
    esc = False
    while j < len(text) and depth > 0:
        ch = text[j]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
        j += 1
    inner = text[i:j - 1]
    return [s.replace('\\"', '"').replace("\\n", "\n") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', inner)]


def extract_balanced_paren(text, open_pos):
    """Return text inside the parentheses opened at open_pos (balanced, string-aware)."""
    depth = 0
    in_str = False
    esc = False
    i = open_pos
    while i < len(text):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return text[open_pos + 1:i]
        i += 1
    return None


def field_string_list(text, name):
    m = re.search(re.escape(name) + r"\s*=\s*listOf\(", text)
    if not m:
        return []
    return parse_kotlin_string_list(text[m.start():])


def field_string(text, name, default=None):
    m = re.search(name + r'\s*=\s*"((?:[^"\\]|\\.)*)"', text)
    return m.group(1) if m else default


def field_enum(text, name):
    m = re.search(name + r"\s*=\s*ObjectType\.([A-Z_]+)", text)
    return m.group(1) if m else None


def field_num(text, name, default=None):
    m = re.search(name + r"\s*=\s*([-0-9.eE]+)", text)
    return float(m.group(1)) if m else default


# ---------------------------------------------------------------------------
# 1. Engine deep-sky catalog
# ---------------------------------------------------------------------------
engine_text = read(os.path.join(SRC, "astro_engine", "DeepSkyCatalog.kt"))
engine_objects = []
for m in re.finditer(
    r'DeepSkyObject\(\s*"([^"]*)"\s*,\s*(?:"([^"]*)"|null)\s*,\s*(?:"((?:[^"\\]|\\.)*)"|null)\s*,\s*ObjectType\.([A-Z_]+)\s*,\s*'
    r'([0-9.]+)\s*,\s*(-?[0-9.]+)\s*,\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,\s*"([A-Za-z]+)"\s*,\s*(?:null|([0-9.]+))\s*,\s*"((?:[^"\\]|\\.)*)"\s*\)',
    engine_text,
):
    g = m.groups()
    engine_objects.append(dict(
        catalogId=g[0], altCatalogId=g[1] or None, commonName=g[2] or None, type=g[3],
        raHours=float(g[4]), decDeg=float(g[5]), magnitude=float(g[6]), sizeArcmin=float(g[7]),
        constellation=g[8], distanceLy=float(g[9]) if g[9] else None, description=g[10],
    ))


# ---------------------------------------------------------------------------
# 2. CelestialObject catalogs
# ---------------------------------------------------------------------------
def parse_celestial_objects(path):
    text = read(path)
    out = []
    for b in re.findall(r'CelestialObject\((.*?)\n\s*\)\s*,?\s*(?:\n|//)', text, re.S):
        oid = field_string(b, "id")
        if not oid:
            continue
        out.append(dict(
            id=oid, type=field_enum(b, "type"),
            nameEn=field_string(b, "nameEn", ""), nameFa=field_string(b, "nameFa", ""),
            raDeg=field_num(b, "raDeg"), decDeg=field_num(b, "decDeg"),
            magnitude=field_num(b, "magnitude"),
            constellationEn=field_string(b, "constellationEn", ""), constellationFa=field_string(b, "constellationFa", ""),
            distanceLightYears=field_num(b, "distanceLightYears", 0.0),
            category=field_string(b, "category", ""),
            descriptionEn=field_string(b, "descriptionEn", ""), descriptionFa=field_string(b, "descriptionFa", ""),
            observationTipEn=field_string(b, "observationTipEn", ""), observationTipFa=field_string(b, "observationTipFa", ""),
            spectralType=field_string(b, "spectralType", ""),
            bayerDesignation=field_string(b, "bayerDesignation", ""),
            flamsteedNumber=field_string(b, "flamsteedNumber", ""),
            hipId=field_string(b, "hipId"), hdId=field_string(b, "hdId"),
            temperatureK=field_num(b, "temperatureK", 0),
            angularSizeArcmin=field_num(b, "angularSizeArcmin"),
            funFactsEn=field_string_list(b, "funFactsEn"),
            funFactsFa=field_string_list(b, "funFactsFa"),
            bestViewingMonthEn=field_string(b, "bestViewingMonthEn", ""),
            bestViewingMonthFa=field_string(b, "bestViewingMonthFa", ""),
            zhr=field_num(b, "zhr", 0),
        ))
    return out


hand_dso = parse_celestial_objects(os.path.join(SRC, "data", "catalog", "DeepSkyCatalog.kt"))
stars = parse_celestial_objects(os.path.join(SRC, "data", "catalog", "StarCatalog.kt"))
meteors = parse_celestial_objects(os.path.join(SRC, "data", "catalog", "MeteorShowerCatalog.kt"))
asterisms = parse_celestial_objects(os.path.join(SRC, "data", "catalog", "AsterismCatalog.kt"))


# ---------------------------------------------------------------------------
# 3. Constellations
# ---------------------------------------------------------------------------
const_text = read(os.path.join(SRC, "data", "catalog", "ConstellationCatalog.kt"))
constellations = []
for b in re.findall(r'ConstellationData\((.*?)\n\s*\)', const_text, re.S):
    area = re.search(r'areaSqDeg\s*=\s*([0-9.]+)', b)
    constellations.append(dict(
        code=field_string(b, "code"), nameEn=field_string(b, "nameEn"), nameFa=field_string(b, "nameFa"),
        seasonEn=field_string(b, "seasonEn"), seasonFa=field_string(b, "seasonFa"),
        historicalInfoEn=field_string(b, "historicalInfoEn"), historicalInfoFa=field_string(b, "historicalInfoFa"),
        areaSqDeg=float(area.group(1)) if area else 0.0,
    ))


# ---------------------------------------------------------------------------
# 4. Satellites
# ---------------------------------------------------------------------------
sat_text = read(os.path.join(SRC, "astro_engine", "SatelliteCatalog.kt"))
satellites = []
for m in re.finditer(r'SatelliteItem\(', sat_text):
    b = extract_balanced_paren(sat_text, m.end() - 1)
    if b is None or field_string(b, "id") is None:
        continue  # skip the data class declaration
    satellites.append(dict(
        id=field_string(b, "id"), noradId=int(field_num(b, "noradId", 0)),
        nameEn=field_string(b, "nameEn"), nameFa=field_string(b, "nameFa"),
        descriptionEn=field_string(b, "descriptionEn"), descriptionFa=field_string(b, "descriptionFa"),
        verifiedFactsEn=field_string_list(b, "verifiedFactsEn"),
        verifiedFactsFa=field_string_list(b, "verifiedFactsFa"),
    ))


# ---------------------------------------------------------------------------
# 5/6. Facts maps
# ---------------------------------------------------------------------------
dsc = read(os.path.join(SRC, "data", "catalog", "DeepSkyContent.kt"))
gen_text = read(os.path.join(SRC, "data", "catalog", "DeepSkyGeneratedFacts.kt"))


def parse_bilingual_facts_map(text):
    out = {}
    for m in re.finditer(r'"([a-z_0-9]+)"\s+to\s+BilingualFacts\(', text):
        cid = m.group(1)
        block = text[m.start():]
        i = block.find("(")
        depth = 1
        j = i + 1
        while j < len(block) and depth > 0:
            if block[j] == "(":
                depth += 1
            elif block[j] == ")":
                depth -= 1
            j += 1
        body = block[i:j]
        out[cid] = dict(en=field_string_list(body, "en"), fa=field_string_list(body, "fa"))
    return out


hand_facts = parse_bilingual_facts_map(dsc)
generated_facts = parse_bilingual_facts_map(gen_text)


def facts_for_canonical_id(cid):
    return hand_facts.get(cid) or generated_facts.get(cid)


def map_slice(text, start_marker, end_marker):
    a = text.index(start_marker)
    b = text.index(end_marker, a)
    return text[a:b]


common_name_fa = dict(re.findall(r'"((?:[^"\\]|\\.)*)"\s*to\s*"((?:[^"\\]|\\.)*)"', map_slice(dsc, "commonNameFa = mapOf", "private val translatedWords")))
type_name_fa = dict(re.findall(r'ObjectType\.([A-Z_]+)\s*->\s*"((?:[^"\\]|\\.)*)"', map_slice(dsc, "fun typeNameFa", "fun persianNameForEngineObject")))
translated_words = dict(re.findall(r'"([a-z]+)"\s*to\s*"((?:[^"\\]|\\.)*)"', map_slice(dsc, "private val translatedWords", "private fun transliterateLatinPhrase")))

special_map = {"a": "ا", "b": "ب", "c": "ک", "d": "د", "e": "ه", "f": "ف", "g": "گ", "h": "ه", "i": "ی", "j": "ج", "k": "ک", "l": "ل", "m": "م", "n": "ن", "o": "و", "p": "پ", "q": "ک", "r": "ر", "s": "س", "t": "ت", "u": "و", "v": "و", "w": "و", "x": "کس", "y": "ی", "z": "ز"}


def transliterate_word(w):
    return "".join(special_map.get(ch, ch) for ch in w.lower())


def transliterate_phrase(v):
    v = v.replace("'s", "").replace("(", " ").replace(")", " ").replace("-", " ")
    return " ".join(translated_words.get(p.lower(), transliterate_word(p)) for p in [x for x in re.split(r"\s+", v) if x])


def persian_name_for_engine(obj, designation_text):
    clean = (obj["commonName"] or "").replace(" Alt", "").strip() if obj["commonName"] else None
    common_fa = common_name_fa.get(clean) if clean else None
    if clean and not common_fa:
        common_fa = transliterate_phrase(clean)
    base = common_fa or type_name_fa[obj["type"]]
    return f"{base} ({designation_text})" if designation_text else base


# ---------------------------------------------------------------------------
# 7. CatalogTextLocalizer
# ---------------------------------------------------------------------------
loc_text = read(os.path.join(SRC, "data", "catalog", "CatalogTextLocalizer.kt"))
exact_cat_fa = dict(re.findall(r'"((?:[^"\\]|\\.)*)"\s*to\s*"((?:[^"\\]|\\.)*)"', map_slice(loc_text, "exactCategoryFa = mapOf", "orderedReplacements = listOf")))
ordered_repl = re.findall(r'"((?:[^"\\]|\\.)*)"\s*to\s*"((?:[^"\\]|\\.)*)"', map_slice(loc_text, "orderedReplacements = listOf", ")\n}"))


def persian_category(cat_en):
    if cat_en in exact_cat_fa:
        return exact_cat_fa[cat_en]
    r = cat_en
    for en, fa in ordered_repl:
        r = r.replace(en, fa)
    return re.sub(r"\s+", " ", r).strip()


# ---------------------------------------------------------------------------
# 8. PhysicalData cool facts
# ---------------------------------------------------------------------------
pd_text = read(os.path.join(SRC, "data", "catalog", "PhysicalData.kt"))
cool_fa_keys = set(re.findall(r'"([a-z_0-9]+)"\s*to\s*[A-Z_]+_FACTS_FA', map_slice(pd_text, "coolFactsMap: Map", "coolFactsMapEn: Map")))
cool_en_keys = set(re.findall(r'"([a-z_0-9]+)"\s*to\s*[A-Z_]+_FACTS_EN', map_slice(pd_text, "coolFactsMapEn: Map", "fun getPhysicalProperties")))
fact_constants = {}
for m in re.finditer(r'(SIRIUS|VEGA|BETELGEUSE|POLARIS)_FACTS_(EN|FA)\s*=\s*listOf\(', pd_text):
    fact_constants[m.group(1) + "_FACTS_" + m.group(2)] = parse_kotlin_string_list(pd_text[m.start():])


# ---------------------------------------------------------------------------
# 9. Core objects (facts + metadata from CanonicalAstroCatalog)
# ---------------------------------------------------------------------------
canonical_text = read(os.path.join(SRC, "data", "catalog", "CanonicalAstroCatalog.kt"))


def extract_balanced_paren(text, open_pos):
    """Return text inside the parentheses opened at open_pos (balanced, string-aware)."""
    depth = 0
    in_str = False
    esc = False
    i = open_pos
    while i < len(text):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
        else:
            if ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return text[open_pos + 1:i]
        i += 1
    return None


def parse_core_object(name):
    m = re.search(r"val\s+" + name + r"\s*=\s*CanonicalAstroObject\(", canonical_text)
    if not m:
        return None
    b = extract_balanced_paren(canonical_text, m.end() - 1)
    if b is None:
        return None
    return dict(
        canonicalId=field_string(b, "canonicalId"),
        type=field_enum(b, "type"),
        nameEn=field_string(b, "nameEn", ""), nameFa=field_string(b, "nameFa", ""),
        verifiedFactsEn=field_string_list(b, "verifiedFactsEn"),
        verifiedFactsFa=field_string_list(b, "verifiedFactsFa"),
        funFactsEn=field_string_list(b, "funFactsEn"),
        funFactsFa=field_string_list(b, "funFactsFa"),
        categoryEn=field_string(b, "categoryEn", ""), categoryFa=field_string(b, "categoryFa", ""),
        descriptionEn=field_string(b, "descriptionEn", ""), descriptionFa=field_string(b, "descriptionFa", ""),
        observationTipEn=field_string(b, "observationTipEn", ""), observationTipFa=field_string(b, "observationTipFa", ""),
        legacyIds=field_string_list(b, "legacyIds"),
        catalogDesignations=field_string_list(b, "catalogDesignations"),
        messierId=field_string(b, "messierId"), ngcId=field_string(b, "ngcId"), caldwellId=field_string(b, "caldwellId"),
        noradId=field_string(b, "noradId"),
        raDeg=field_num(b, "raDeg"), decDeg=field_num(b, "decDeg"),
        magnitude=field_num(b, "magnitude", 0.0),
    )


CORE_NAMES = ["SUN", "EARTH", "MOON", "MERCURY", "VENUS", "MARS", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO",
              "IO", "EUROPA", "GANYMEDE", "CALLISTO", "ELARA", "SAGITTARIUS_A_STAR", "ISS"]
core_objects = [o for o in (parse_core_object(n) for n in CORE_NAMES) if o]


# ---------------------------------------------------------------------------
# 9b. ExtendedFactCatalog (constellations / showers / asterisms / stars)
# ---------------------------------------------------------------------------
def parse_extended_fact_file(path):
    text = read(path)
    out = {}
    for m in re.finditer(r'"([a-z_0-9]+)"\s+to\s+BilingualFacts\(', text):
        cid = m.group(1)
        block = extract_balanced_paren(text, m.end() - 1)
        if block is None:
            continue
        out[cid] = dict(en=field_string_list(block, "en"), fa=field_string_list(block, "fa"))
    return out


extended_facts = {}
for _f in ("ConstellationFacts.kt", "MeteorShowerFacts.kt", "AsterismFacts.kt", "StarFacts.kt"):
    extended_facts.update(parse_extended_fact_file(os.path.join(SRC, "data", "catalog", _f)))


# ---------------------------------------------------------------------------
# 10. Merge port
# ---------------------------------------------------------------------------
def normalized_catalog_token(value):
    if not value:
        return None
    clean = re.sub(r"[^a-z0-9]+", "", value.strip().lower())
    return clean or None


def is_catalog_identity_token(token):
    if token.startswith("ngc") and token[3:].isdigit():
        return True
    if token.startswith("ic") and token[2:].isdigit():
        return True
    if token.startswith("m") and token[1:].isdigit():
        return True
    if token.startswith("c") and token[1:].isdigit():
        return True
    if token.startswith("melotte") and token[7:].isdigit():
        return True
    if token.startswith("mel") and token[3:].isdigit():
        return True
    if token.startswith("norad") and token[5:].isdigit():
        return True
    return False


def catalog_identity_tokens(obj):
    raw = list(obj.get("catalogDesignations", [])) + list(obj.get("legacyIds", []))
    for k in ("messierId", "ngcId", "caldwellId"):
        v = obj.get(k)
        if v:
            raw.append(v)
    if obj.get("noradId"):
        raw.append("NORAD %s" % obj["noradId"])
    toks = set()
    for r in raw:
        t = normalized_catalog_token(r)
        if t and is_catalog_identity_token(t):
            toks.add(t)
    return toks


def catalog_tokens_from_text(value):
    tokens = set()
    if not value:
        return tokens
    upper = value.upper()
    for pat in (r"\bM\s*\d+[A-Z]?\b", r"\bNGC\s*\d+[A-Z]?\b", r"\bIC\s*\d+[A-Z]?\b", r"\bC\s*\d+[A-Z]?\b", r"\bMEL(?:OTTE)?\s*\d+[A-Z]?\b"):
        for m in re.finditer(pat, upper):
            t = normalized_catalog_token(m.group(0))
            if t:
                tokens.add(t)
    return tokens


def hand_authored_dso_tokens(obj):
    tokens = set()
    for v in [obj["id"], obj["nameEn"], obj["nameFa"], obj["category"]]:
        tokens |= catalog_tokens_from_text(v)
    for name in [obj["nameEn"], obj["nameFa"]]:
        head = name.split("(")[0].strip().lower()
        if len(head) > 2:
            tokens.add(head)
            t = normalized_catalog_token(head)
            if t:
                tokens.add(t)
    return tokens


def engine_dso_tokens(obj):
    out = set()
    for v in [obj["catalogId"], obj["altCatalogId"], obj["commonName"]]:
        if not v:
            continue
        t = normalized_catalog_token(v)
        if t:
            out.add(t)
        elif len(v.lower()) > 2:
            out.add(v.lower())
    return out


def is_canonical_deep_sky(t):
    return t in ("DEEP_SKY", "GALAXY", "NEBULA", "STAR_CLUSTER", "GLOBULAR_CLUSTER", "BLACK_HOLE")


def map_engine_deep_sky_type(t):
    if t in ("GALAXY", "SPIRAL_GALAXY", "ELLIPTICAL_GALAXY", "IRREGULAR_GALAXY", "LENTICULAR_GALAXY"):
        return "GALAXY"
    if t in ("DIFFUSE_NEBULA", "PLANETARY_NEBULA", "SUPERNOVA_REMNANT"):
        return "NEBULA"
    if t == "GLOBULAR_CLUSTER":
        return "GLOBULAR_CLUSTER"
    return "STAR_CLUSTER"


def clean_engine_type_name(t):
    return " ".join(p.capitalize() for p in t.lower().split("_"))


HAND_DOUBLE_CLUSTER_ID = "dso_double_cluster"
split_double = {"ngc869", "ngc884"}
skipped_alias = {"c14"}
coordinate_allowlist = {frozenset(["dso_m102", "dso_ngc_3115"]), frozenset(["dso_ngc_6960", "dso_ngc_6992"])}

constellation_code_overrides = {
    "andromeda": "AND", "dorado / mensa": "DOR/MEN", "tucana": "TUC", "triangulum": "TRI",
    "taurus": "TAU", "cancer": "CNC", "perseus": "PER", "coma berenices": "COM",
    "centaurus": "CEN", "hercules": "HER", "orion": "ORI", "sagittarius": "SGR", "carina": "CAR",
}
constellation_by_code = {c["code"]: c for c in constellations}
constellation_by_name_en = {c["nameEn"].lower(): c for c in constellations}


def constellation_names_for_code(code):
    c = constellation_by_code.get(code.upper())
    return (c["nameEn"], c["nameFa"]) if c else (code, code)


def angular_sep_deg(a_ra, a_dec, b_ra, b_dec):
    import math
    ra1, dc1, ra2, dc2 = map(math.radians, [a_ra, a_dec, b_ra, b_dec])
    cosv = (math.sin(dc1) * math.sin(dc2) + math.cos(dc1) * math.cos(dc2) * math.cos(ra1 - ra2))
    return math.degrees(math.acos(max(-1.0, min(1.0, cosv))))


def is_coordinate_duplicate(a, b):
    if a["type"] != b["type"]:
        return False
    if frozenset([a["canonicalId"], b["canonicalId"]]) in coordinate_allowlist:
        return False
    if a.get("raDeg") is None or b.get("raDeg") is None:
        return False
    sep = angular_sep_deg(a["raDeg"], a["decDeg"], b["raDeg"], b["decDeg"])
    dmag = abs(a.get("magnitude", 0.0) - b.get("magnitude", 0.0))
    return sep <= 0.05 and dmag <= 0.5


def merge_facts(existing, candidate):
    """Port of withMergedCatalogIdentity for facts only."""
    for k in ("verifiedFactsEn", "verifiedFactsFa", "funFactsEn", "funFactsFa"):
        if not existing.get(k) and candidate.get(k):
            existing[k] = candidate[k]
    # merge identity tokens
    existing["catalogDesignations"] = sorted(set(existing.get("catalogDesignations", []) + candidate.get("catalogDesignations", [])))
    existing["legacyIds"] = sorted(set(existing.get("legacyIds", []) + candidate.get("legacyIds", [])))
    return existing


def add_or_merge(catalog_list, candidate):
    ct = catalog_identity_tokens(candidate)
    match_idx = None
    if ct:
        for i, ex in enumerate(catalog_list):
            if catalog_identity_tokens(ex) & ct:
                match_idx = i
                break
    if match_idx is None:
        for i, ex in enumerate(catalog_list):
            if is_coordinate_duplicate(ex, candidate):
                match_idx = i
                break
    if match_idx is not None:
        catalog_list[match_idx] = merge_facts(catalog_list[match_idx], candidate)
    elif not any(c["canonicalId"] == candidate["canonicalId"] for c in catalog_list):
        catalog_list.append(candidate)


def base_object(**kw):
    o = dict(canonicalId=None, legacyIds=[], catalogDesignations=[], messierId=None, ngcId=None, caldwellId=None,
             noradId=None, raDeg=None, decDeg=None, magnitude=0.0, type=None, nameEn="", nameFa="",
             verifiedFactsEn=[], verifiedFactsFa=[], funFactsEn=[], funFactsFa=[],
             categoryEn="", categoryFa="", descriptionEn="", descriptionFa="",
             observationTipEn="", observationTipFa="")
    o.update(kw)
    return o


catalog_list = []
core_ids = {o["canonicalId"] for o in core_objects}
for o in core_objects:
    catalog_list.append(o)

# satellites
for sat in satellites:
    canon = "sat_25544" if sat["noradId"] == 25544 else f"sat_{sat['noradId']}"
    if canon in core_ids or any(c["canonicalId"] == canon for c in catalog_list):
        continue
    add_or_merge(catalog_list, base_object(
        canonicalId=canon, legacyIds=[sat["id"], f"sat_{sat['id']}", f"norad_{sat['noradId']}"],
        noradId=str(sat["noradId"]), type="SATELLITE", nameEn=sat["nameEn"], nameFa=sat["nameFa"],
        descriptionEn=sat["descriptionEn"], descriptionFa=sat["descriptionFa"],
        verifiedFactsEn=sat["verifiedFactsEn"], verifiedFactsFa=sat["verifiedFactsFa"],
    ))

# stars + hand DSOs + meteors + asterisms
extra = stars + hand_dso + meteors + asterisms
hand_tokens_all = set()
for o in hand_dso:
    hand_tokens_all |= hand_authored_dso_tokens(o)

for obj in extra:
    if obj["id"] == HAND_DOUBLE_CLUSTER_ID:
        continue
    if obj["id"] in core_ids or any(c["canonicalId"] == obj["id"] for c in catalog_list):
        continue
    curated = facts_for_canonical_id(obj["id"])
    pd_en = pd_fa = []
    star_name = next((s for s in ("sirius", "vega", "betelgeuse", "polaris") if s in obj["id"]), None)
    if star_name:
        pd_en = fact_constants.get(star_name.upper() + "_FACTS_EN", [])
        pd_fa = fact_constants.get(star_name.upper() + "_FACTS_FA", [])
    ext = extended_facts.get(obj["id"], {})
    facts_en = obj["funFactsEn"] or (curated["en"] if curated else (pd_en or ext.get("en", [])))
    facts_fa = obj["funFactsFa"] or (curated["fa"] if curated else (pd_fa or ext.get("fa", [])))
    matched_engine = None
    if is_canonical_deep_sky(obj["type"]) and obj["id"] != HAND_DOUBLE_CLUSTER_ID:
        for eo in engine_objects:
            if engine_dso_tokens(eo) & hand_authored_dso_tokens(obj):
                matched_engine = eo
                break
    add_or_merge(catalog_list, base_object(
        canonicalId=obj["id"], legacyIds=[obj["id"]], type=obj["type"],
        nameEn=obj["nameEn"], nameFa=obj["nameFa"],
        raDeg=obj["raDeg"], decDeg=obj["decDeg"], magnitude=obj["magnitude"],
        verifiedFactsEn=facts_en, verifiedFactsFa=facts_fa,
        categoryEn=obj["category"], categoryFa=persian_category(obj["category"]),
        descriptionEn=obj["descriptionEn"], descriptionFa=obj["descriptionFa"],
        observationTipEn=obj["observationTipEn"], observationTipFa=obj["observationTipFa"],
        hipId=obj["hipId"], hdId=obj["hdId"], bayerDesignation=obj["bayerDesignation"],
        spectralType=obj["spectralType"],
    ))


# engine deep sky
def should_skip_engine(obj, hand_tokens):
    t = normalized_catalog_token(obj["catalogId"])
    if t in split_double:
        return False
    if t in skipped_alias:
        return False
    return bool(engine_dso_tokens(obj) & hand_tokens)


for eo in engine_objects:
    if normalized_catalog_token(eo["catalogId"]) in skipped_alias:
        continue
    if should_skip_engine(eo, hand_tokens_all):
        continue
    normalized_catalog = re.sub(r"[^a-z0-9]+", "_", eo["catalogId"].lower()).strip("_")
    canon = f"dso_{normalized_catalog}"
    if canon in core_ids:
        continue
    designations = [eo["catalogId"]] + ([eo["altCatalogId"]] if eo["altCatalogId"] else [])
    if normalized_catalog_token(eo["catalogId"]) == "ngc869":
        designations = designations + ["C14"]
    designation_text = " / ".join(designations)
    name_en = (f'{eo["commonName"]} ({designation_text})') if eo["commonName"] else designation_text
    name_fa = persian_name_for_engine(eo, designation_text)
    const_en, const_fa = constellation_names_for_code(eo["constellation"])
    curated = facts_for_canonical_id(canon)
    type_fa = type_name_fa[eo["type"]]
    clean_common = (eo["commonName"] or "").replace(" Alt", "").strip() if eo["commonName"] else None
    named = (common_name_fa.get(clean_common) or transliterate_phrase(clean_common)) if clean_common else None
    subject = named or f"{type_fa} {designation_text}".strip()
    desc_fa = (f"{subject} یک {type_fa} در صورت فلکی {const_fa} است و با شناسه‌های {designation_text} در فهرست‌های رصدی شناخته می‌شود."
               if designation_text else f"{subject} یک {type_fa} در صورت فلکی {const_fa} است.")
    norm_designations = [t for t in (normalized_catalog_token(d) for d in designations) if t]
    legacy = [d.lower() for d in (designations + norm_designations + [f"deep_sky_{normalized_catalog}"])]
    messier = eo["catalogId"] if eo["catalogId"].startswith("M") else None
    ngc = next((d for d in [eo["catalogId"], eo["altCatalogId"]] if d and d.startswith("NGC")), None)
    caldwell = eo["catalogId"] if eo["catalogId"].startswith("C") and eo["catalogId"][1:].isdigit() else None
    add_or_merge(catalog_list, base_object(
        canonicalId=canon, legacyIds=list(dict.fromkeys(legacy)),
        catalogDesignations=designations, messierId=messier, ngcId=ngc, caldwellId=caldwell,
        type=map_engine_deep_sky_type(eo["type"]), nameEn=name_en, nameFa=name_fa,
        raDeg=eo["raHours"] * 15.0, decDeg=eo["decDeg"], magnitude=eo["magnitude"],
        verifiedFactsEn=curated["en"] if curated else [], verifiedFactsFa=curated["fa"] if curated else [],
        categoryEn=clean_engine_type_name(eo["type"]), categoryFa=type_fa,
        descriptionEn=eo["description"], descriptionFa=desc_fa,
        observationTipEn="Best observed from a dark-sky site with binoculars or a telescope when high above the horizon.",
        observationTipFa="برای رصد بهتر، این جرم را در آسمان تاریک و هنگام ارتفاع زیاد با دوربین دوچشمی یا تلسکوپ دنبال کنید.",
    ))

# constellations
for c in constellations:
    canon = f"const_{c['code'].lower()}"
    if canon in core_ids or any(x["canonicalId"] == canon for x in catalog_list):
        continue
    ext = extended_facts.get(canon, {})
    add_or_merge(catalog_list, base_object(
        canonicalId=canon, legacyIds=[c["code"].lower(), f"constellation_{c['code'].lower()}"],
        type="CONSTELLATION", nameEn=f"{c['nameEn']} Constellation", nameFa=f"صورت فلکی {c['nameFa']}",
        categoryEn=f"Constellation ({c['seasonEn']})", categoryFa=f"صورت فلکی ({c['seasonFa']})",
        descriptionEn=(f"{c['historicalInfoEn']} Area: {c['areaSqDeg']:g} sq deg." if c["historicalInfoEn"] else ""),
        descriptionFa=(f"{c['historicalInfoFa']} مساحت: {c['areaSqDeg']:g} درجه مربع." if c["historicalInfoFa"] else ""),
        verifiedFactsEn=ext.get("en", []), verifiedFactsFa=ext.get("fa", []),
    ))


# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
def all_facts(o):
    return dict(en=o["verifiedFactsEn"] + o["funFactsEn"], fa=o["verifiedFactsFa"] + o["funFactsFa"])


deep_sky_types = {"DEEP_SKY", "GALAXY", "NEBULA", "STAR_CLUSTER", "GLOBULAR_CLUSTER", "BLACK_HOLE"}
type_counts = Counter(o["type"] for o in catalog_list)
dso = [o for o in catalog_list if o["type"] in deep_sky_types]
zero = [o for o in catalog_list if not all_facts(o)["en"] and not all_facts(o)["fa"]]

print(f"engine_objects={len(engine_objects)} hand_dso={len(hand_dso)} stars={len(stars)} meteors={len(meteors)} "
      f"asterisms={len(asterisms)} constellations={len(constellations)} satellites={len(satellites)} core={len(core_objects)}")
print(f"hand_facts={len(hand_facts)} generated_facts={len(generated_facts)}")
print(f"FINAL CANONICAL COUNT={len(catalog_list)}")
print(f"duplicates={[i for i, n in Counter(o['canonicalId'] for o in catalog_list).items() if n > 1]}")
print(f"DEEP_SKY_COUNT={len(dso)}")
print(f"ZERO_FACT_COUNT={len(zero)}")
for o in zero:
    print(f"  ZERO {o['canonicalId']} ({o['type']})")

# duplicate-fact check: exact + near (normalize out names/designations)
def norm_fact(s):
    s = re.sub(r"\s+", " ", s.strip().lower())
    return s

owners = defaultdict(list)
for o in catalog_list:
    for f in all_facts(o)["en"]:
        owners[norm_fact(f)].append(o["canonicalId"])
exact_dup = {k: sorted(set(v)) for k, v in owners.items() if len(set(v)) > 1}
print(f"\nEXACT duplicate English facts across objects: {len(exact_dup)}")
for k, v in list(exact_dup.items())[:30]:
    print(f"  [{','.join(v)}] {k[:110]}")

# near-duplicate: strip the object's own name + designation tokens from each fact, then re-group
def strip_identity(fact, o):
    f = fact
    for tok in [o["nameEn"].split("(")[0].strip(), o["nameFa"].split("(")[0].strip()]:
        if tok:
            f = re.sub(re.escape(tok), "§", f, flags=re.I)
    for d in o.get("catalogDesignations", []):
        f = re.sub(re.escape(d), "§", f, flags=re.I)
    return re.sub(r"\s+", " ", f.strip().lower())

near_owners = defaultdict(set)
for o in catalog_list:
    for f in all_facts(o)["en"]:
        near_owners[strip_identity(f, o)].add(o["canonicalId"])
near_dup = {k: sorted(v) for k, v in near_owners.items() if len(v) > 1}
print(f"\nNEAR-duplicate English fact templates across objects: {len(near_dup)}")
for k, v in sorted(near_dup.items(), key=lambda kv: -len(kv[1]))[:40]:
    print(f"  [{','.join(v[:8])}{'…' if len(v) > 8 else ''}] {k[:100]}")

with open(os.path.join(ROOT, "audit", "union_list.json"), "w", encoding="utf-8") as f:
    json.dump([dict(id=o["canonicalId"], type=o["type"], nameEn=o["nameEn"], nameFa=o["nameFa"],
                    factsEn=all_facts(o)["en"], factsFa=all_facts(o)["fa"],
                    descriptionEn=o["descriptionEn"], descriptionFa=o["descriptionFa"],
                    observationTipEn=o["observationTipEn"], observationTipFa=o["observationTipFa"],
                    categoryEn=o["categoryEn"], categoryFa=o["categoryFa"],
                    catalogDesignations=o.get("catalogDesignations", []),
                    messierId=o.get("messierId"), ngcId=o.get("ngcId"), caldwellId=o.get("caldwellId"))
               for o in catalog_list], f, ensure_ascii=False, indent=1)
print(f"\nWrote audit/union_list.json ({len(catalog_list)} objects)")
