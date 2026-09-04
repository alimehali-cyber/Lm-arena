#!/usr/bin/env python3
"""F-A oracle: astropy reference for the A2 case grid; diffs every Kotlin route.

Inputs  : /tmp/oracle_probe/{objects,routes,times,ladder}.csv  (Kotlin probe output)
Outputs : docs/startracker/evidence/ORACLE_CASES.csv        (oracle + app values, per case/object)
          /tmp/oracle_probe/residuals.txt                   (per-route residual tables, A4)
          /tmp/oracle_probe/refraction_ladder_oracle.csv    (A6)
Deterministic: IERS auto_download off, builtin ephemeris, fixed inputs.
"""
import csv, math, sys
from collections import defaultdict
from astropy.utils import iers
iers.conf.auto_download = False
iers.conf.auto_max_age = None   # UT1-UTC from bundled table; uncertainty <1s -> <15" az
import astropy.units as u
from astropy.time import Time
from astropy.coordinates import EarthLocation, SkyCoord, AltAz, get_body, get_sun, solar_system_ephemeris
import numpy as np

solar_system_ephemeris.set('builtin')

# app weather model (CoordinateEngineLegacy): P = 1013.25*(1-2.25577e-5*h)^5.25588 hPa, T = 288.15-0.0065h K
def app_weather(h_m):
    p = 1013.25 * (1.0 - 2.25577e-5 * h_m) ** 5.25588
    t_k = 288.15 - 0.0065 * h_m
    return p, t_k - 273.15  # hPa, degC

IN = "/tmp/oracle_probe"
objs = {r['obj_id']: r for r in csv.DictReader(open(f"{IN}/objects.csv"))}
times = list(csv.DictReader(open(f"{IN}/times.csv")))
routes = list(csv.DictReader(open(f"{IN}/routes.csv")))

# pluto EXCLUDED: not computable with the builtin ephemeris and no reachable kernel host
# (JPL ssd + raw.githubusercontent blocked; skyfield-data mirror gone). Disclosed in evidence.
PLANETS = ["mercury", "venus", "mars", "jupiter", "saturn", "uranus", "neptune"]

# ---- index routes by (loc, iso, route) ----
idx = defaultdict(dict)
for r in routes:
    idx[(r['loc'], r['iso_utc'], r['route'])][r['obj_id']] = r

loc_keys = sorted({(r['loc'], float(r['lat']), float(r['lon']), float(r['height_m'])) for r in routes})

def wrap180(d):
    return (d + 180.0) % 360.0 - 180.0

# ---- ORACLE_CASES.csv rows ----
oc_rows = []
bodies_rows = []
oc = open("docs/startracker/evidence/ORACLE_CASES.csv", "w", newline="")
oc_w = csv.writer(oc)
oc_w.writerow(["loc","lat_deg","lon_deg","height_m","iso_utc","jd_utc","obj_id","obj_type",
               "ra_in_deg","dec_in_deg","mag",
               "gmst_deg_oracle","gmst_deg_kotlin","gast_deg_oracle","gast_deg_kotlin",
               "az_oracle_geometric","alt_oracle_geometric",
               "az_oracle_refr1010","alt_oracle_refr1010",
               "az_oracle_refr_appweather","alt_oracle_refr_appweather",
               "az_kotlin_star_live","alt_kotlin_star_live",
               "az_kotlin_star_live_elev","alt_kotlin_star_live_elev",
               "az_kotlin_fte","alt_kotlin_fte"])

for trow in times:
    iso = trow['iso_utc']
    t = Time(iso, scale='utc')
    jd = float(trow['jd_utc'])
    gmst_o = t.sidereal_time('mean', 'greenwich').deg
    gast_o = t.sidereal_time('apparent', 'greenwich').deg
    print(f"[time] {iso} GMST d={wrap180(gmst_o - float(trow['gmst_deg_kotlin']))*3600:.3f}\" "
          f"({wrap180(gmst_o - float(trow['gmst_deg_kotlin']))*240:.4f}s)  "
          f"GAST d={wrap180(gast_o - float(trow['gast_deg_kotlin']))*3600:.3f}\" "
          f"({wrap180(gast_o - float(trow['gast_deg_kotlin']))*240:.4f}s)")

    for (locname, lat, lon, h) in loc_keys:
        if (locname, iso, 'star_live') not in idx:
            continue
        el = EarthLocation.from_geodetic(lon=lon * u.deg, lat=lat * u.deg, height=h * u.m)
        p_app, tc_app = app_weather(h)
        aa_geo = AltAz(obstime=t, location=el)                                   # no refraction
        aa_1010 = AltAz(obstime=t, location=el, pressure=1010*u.hPa, temperature=10*u.deg_C,
                        relative_humidity=0.5, obswl=0.55*u.micron)
        aa_appw = AltAz(obstime=t, location=el, pressure=p_app*u.hPa, temperature=tc_app*u.deg_C,
                        relative_humidity=0.0, obswl=0.55*u.micron)

        star_ids = [oid for oid in idx[(locname, iso, 'star_live')]]
        # stars + DSOs: ICRS positions = the app's OWN stored coords (A tests transform only)
        scs = {oid: SkyCoord(ra=float(objs[oid]['ra_deg'])*u.deg, dec=float(objs[oid]['dec_deg'])*u.deg,
                             frame='icrs', distance=1e9*u.pc) for oid in star_ids}
        trans = {oid: (scs[oid].transform_to(aa_geo), scs[oid].transform_to(aa_1010), scs[oid].transform_to(aa_appw))
                 for oid in star_ids}

        def body_aa(name):
            b = get_body(name, t, el)
            return (b.transform_to(aa_geo), b.transform_to(aa_1010), b.transform_to(aa_appw))

        sun_aa = get_sun(t).transform_to(aa_geo), get_sun(t).transform_to(aa_1010), get_sun(t).transform_to(aa_appw)
        moon_aa = body_aa('moon')

        for oid in star_ids:
            g, r10, raw = trans[oid]
            kl = idx[(locname, iso, 'star_live')][oid]
            ke = idx[(locname, iso, 'star_live_elev')][oid]
            kf = idx[(locname, iso, 'fte')][oid]
            oc_w.writerow([locname, lat, lon, f"{h:.1f}", iso, f"{jd:.9f}", oid, objs[oid]['obj_type'],
                           objs[oid]['ra_deg'], objs[oid]['dec_deg'], objs[oid]['mag'],
                           f"{gmst_o:.9f}", trow['gmst_deg_kotlin'], f"{gast_o:.9f}", trow['gast_deg_kotlin'],
                           f"{g.az.deg:.8f}", f"{g.alt.deg:.8f}", f"{r10.az.deg:.8f}", f"{r10.alt.deg:.8f}",
                           f"{raw.az.deg:.8f}", f"{raw.alt.deg:.8f}",
                           kl['az_deg'], kl['alt_deg'], ke['az_deg'], ke['alt_deg'], kf['az_deg'], kf['alt_deg']])
            oc_rows.append(dict(loc=locname, iso=iso, oid=oid, typ=objs[oid]['obj_type'],
                                az_o=g.az.deg, alt_o=g.alt.deg,
                                az10=r10.az.deg, alt10=r10.alt.deg, azw=raw.az.deg, altw=raw.alt.deg,
                                az1=float(kl['az_deg']), alt1=float(kl['alt_deg']),
                                az2=float(ke['az_deg']), alt2=float(ke['alt_deg']),
                                az3=float(kf['az_deg']), alt3=float(kf['alt_deg'])))

        for name, aas, route in ([('sun', sun_aa, 'sun')] + [('moon', moon_aa, 'moon')] +
                                 [(p, body_aa(p), f'planet_{p}') for p in PLANETS] +
                                 [('moon', moon_aa, 'moon_geocentric')]):
            kl = idx[(locname, iso, route)].get(name if route.startswith(('sun','moon')) else route)
            if kl is None:
                continue
            g, r10, raw = aas
            oc_rows.append(dict(loc=locname, iso=iso, oid=route, typ=route,
                                az_o=g.az.deg, alt_o=g.alt.deg,
                                az10=r10.az.deg, alt10=r10.alt.deg, azw=raw.az.deg, altw=raw.alt.deg,
                                az1=float(kl['az_deg']), alt1=float(kl['alt_deg']),
                                az2=float('nan'), alt2=float('nan'),
                                az3=float('nan'), alt3=float('nan')))
            bodies_rows.append((locname, iso, route, kl['az_deg'], kl['alt_deg'],
                                f"{g.az.deg:.8f}", f"{g.alt.deg:.8f}", f"{raw.az.deg:.8f}", f"{raw.alt.deg:.8f}"))
oc.close()
with open("docs/startracker/evidence/ORACLE_CASES_BODIES.csv", "w", newline="") as bf:
    bw = csv.writer(bf)
    bw.writerow(["loc","iso_utc","route","az_kotlin","alt_kotlin","az_oracle_geometric","alt_oracle_geometric","az_oracle_refr_appweather","alt_oracle_refr_appweather"])
    for r in bodies_rows:
        bw.writerow(r)

# ---- residual tables (A4) ----
def sep_arcmin(az1, alt1, az2, alt2):
    # great-circle separation in arcmin
    a1, l1, a2, l2 = map(math.radians, (az1, alt1, az2, alt2))
    cd = math.sin(l1)*math.sin(l2) + math.cos(l1)*math.cos(l2)*math.cos(a1-a2)
    return math.degrees(math.acos(max(-1.0, min(1.0, cd)))) * 60.0

out = open(f"{IN}/residuals.txt", "w")
def stats(vals):
    v = np.array(vals)
    return f"mean={v.mean():8.3f}  rms={math.sqrt((v**2).mean()):8.3f}  max={v.max():8.3f}"

def table(title, rowsel, oracle_alt, oracle_az, kaz='az1', kalt='alt1'):
    dalt, daz, dsep = [], [], []
    for r in oc_rows:
        if not rowsel(r):
            continue
        if float(r['alt_o']) < 10.0:      # acceptance window: above 10 deg altitude
            continue
        da = wrap180(r[kaz] - r[oracle_az]) * 60.0
        dd = (r[kalt] - r[oracle_alt]) * 60.0
        dalt.append(dd); daz.append(da)
        dsep.append(sep_arcmin(r[kaz], r[kalt], r[oracle_az], r[oracle_alt]))
    if not dalt:
        return
    out.write(f"\n== {title} (alt>10deg, n={len(dalt)}) ==\n")
    out.write(f"  dAz  arcmin: {stats(daz)}\n")
    out.write(f"  dAlt arcmin: {stats(dalt)}\n")
    out.write(f"  on-sky sep  : {stats(dsep)}\n")
    print(f"{title}: n={len(dalt)} sep_rms={math.sqrt((np.array(dsep)**2).mean()):.3f}' max={max(dsep):.3f}'")

# geometric (no refraction): star_live includes app refraction -> compare via sep to geometric AND refracted
table("ROUTE star_live vs oracle NO-refraction (app adds refraction; expect refraction-sized excess)",
      lambda r: r['typ'] in ('STAR',) , 'alt_o', 'az_o')
table("ROUTE star_live vs oracle refr1010", lambda r: r['typ'] in ('STAR',), 'alt10', 'az10')
table("ROUTE star_live vs oracle refr app-weather(elev0=1013.25/15C sea-level model)",
      lambda r: r['typ'] in ('STAR',), 'altw', 'azw')
table("ROUTE star_live_elev vs oracle refr app-weather", lambda r: r['typ'] in ('STAR',), 'altw', 'azw')
table("ROUTE star_live DSOs vs oracle refr app-weather", lambda r: r['typ'] not in ('STAR',) and r['typ'] in
      ('GALAXY','NEBULA','STAR_CLUSTER','GLOBULAR_CLUSTER','PLANETARY_NEBULA','DEEP_SKY','BLACK_HOLE','SUPERNOVA_REMNANT'), 'altw', 'azw')
table("ROUTE fte vs oracle refr1010", lambda r: r['typ'] == 'STAR', 'alt10', 'az10', kaz='az3', kalt='alt3')
table("ROUTE sun vs oracle no-refraction", lambda r: r['oid'] == 'sun', 'alt_o', 'az_o')
table("ROUTE sun vs oracle refr app-weather", lambda r: r['oid'] == 'sun', 'altw', 'azw')
table("ROUTE moon vs oracle no-refraction", lambda r: r['oid'] == 'moon', 'alt_o', 'az_o')
table("ROUTE moon_geocentric vs oracle (parallax defect size)",
      lambda r: r['oid'] == 'moon_geocentric', 'alt_o', 'az_o')
table("ROUTE moon vs oracle refr app-weather (acceptance <3')", lambda r: r['oid'] == 'moon', 'altw', 'azw')
for p in PLANETS:
    table(f"ROUTE planet_{p} vs oracle no-refraction", lambda r, p=p: r['oid'] == f'planet_{p}', 'alt_o', 'az_o')
for p in PLANETS:
    table(f"ROUTE planet_{p} vs oracle refr app-weather", lambda r, p=p: r['oid'] == f'planet_{p}', 'altw', 'azw')

# year-dependence of star_live error (precession signature)
out.write("\n== star_live on-sky error vs instant (precession signature; vs no-refraction oracle) ==\n")
for iso in sorted({r['iso'] for r in oc_rows}):
    seps = [sep_arcmin(r['az1'], r['alt1'], r['az_o'], r['alt_o']) for r in oc_rows
            if r['typ'] == 'STAR' and r['iso'] == iso and r['alt_o'] > 10]
    out.write(f"  {iso}: median={np.median(seps):8.3f}'  max={max(seps):8.3f}'\n")

# ---- A6 ladder ----
lad = open(f"{IN}/refraction_ladder_oracle.csv", "w", newline="")
lw = csv.writer(lad)
lw.writerow(["loc","height_m","iso_utc","target_alt_deg","az_used_deg",
             "R_astropy_1010_arcmin","R_astropy_appweather_arcmin",
             "R_legacy_elev0_arcmin","R_legacy_elevH_arcmin","R_fte_bennett_arcmin"])
for row in csv.DictReader(open(f"{IN}/ladder.csv")):
    t = Time(row['iso_utc'], scale='utc')
    h = float(row['height_m']); p_app, tc_app = app_weather(h)
    el = EarthLocation.from_geodetic(lon=51.39*u.deg if row['loc']=="Tehran" else -157.86*u.deg,
                                     lat=35.69*u.deg if row['loc']=="Tehran" else 21.31*u.deg,
                                     height=h*u.m)
    ta = float(row['target_alt_deg']); azu = float(row['az_used_deg'])
    # astropy: apparent = transform a coord placed at TRUE (target) alt/az with refraction on
    # use ICRS coord crafted same way as the probe? Simpler: take probe's crafted ICRS ra/dec and
    # transform with refraction; the geometric result equals target alt by construction.
    sc = SkyCoord(ra=float(row['ra_deg'])*u.deg, dec=float(row['dec_deg'])*u.deg, frame='icrs', distance=1e9*u.pc)
    g = sc.transform_to(AltAz(obstime=t, location=el))
    r10 = sc.transform_to(AltAz(obstime=t, location=el, pressure=1010*u.hPa, temperature=10*u.deg_C,
                                relative_humidity=0.5, obswl=0.55*u.micron))
    raw = sc.transform_to(AltAz(obstime=t, location=el, pressure=p_app*u.hPa, temperature=tc_app*u.deg_C,
                                relative_humidity=0.0, obswl=0.55*u.micron))
    lw.writerow([row['loc'], row['height_m'], row['iso_utc'], row['target_alt_deg'], row['az_used_deg'],
                 f"{(r10.alt.deg - g.alt.deg)*60:.4f}", f"{(raw.alt.deg - g.alt.deg)*60:.4f}",
                 row['legacy_elev0_R_arcmin'], row['legacy_elevH_R_arcmin'], row['fte_bennett_R_arcmin']])
    print(f"ladder {row['loc']} alt={ta}: oracle1010={(r10.alt.deg-g.alt.deg)*60:8.3f}' "
          f"appw={(raw.alt.deg-g.alt.deg)*60:8.3f}' legacy0={row['legacy_elev0_R_arcmin']}' legacyH={row['legacy_elevH_R_arcmin']}'")
lad.close()
out.close()
print("ORACLE_CASES.csv rows:", len(oc_rows))
