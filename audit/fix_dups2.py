# -*- coding: utf-8 -*-
import io, glob

EDITS = [
    # Group 1 — "Charles Messier discovered X in 1764."
    ("Charles Messier discovered M9 in 1764.",
     "Messier recorded M9 in 1764, one of several clusters he found in Ophiuchus that year."),
    ("Charles Messier discovered M10 in 1764.",
     "Messier cataloged M10 in 1764, describing it as a round nebula without stars."),
    ("Charles Messier discovered M12 in 1764.",
     "Charles Messier discovered M12 in 1764 while hunting comets through Ophiuchus."),
    ("Charles Messier discovered M14 in 1764.",
     "Messier found M14 in 1764, noting it as a faint, round patch of light."),
    ("Charles Messier discovered M18 in 1764.",
     "Charles Messier discovered M18 in 1764 in the star clouds of Sagittarius."),
    ("Charles Messier discovered M19 in 1764.",
     "Messier logged M19 in 1764, remarking on its oval, elongated appearance."),
    ("Charles Messier discovered M21 in 1764.",
     "Charles Messier discovered M21 in 1764, close to the Trifid Nebula."),
    ("Charles Messier discovered M23 in 1764.",
     "Messier cataloged M23 in 1764, one of his first Sagittarius clusters."),
    ("Charles Messier discovered M26 in 1764.",
     "Charles Messier discovered M26 in 1764 in the constellation Scutum."),
    ("Charles Messier discovered M28 in 1764.",
     "Messier found M28 in 1764, describing it as a small, faint nebula."),
    ("Charles Messier discovered M29 in 1764.",
     "Charles Messier discovered M29 in 1764 in the Milky Way of Cygnus."),
    ("Charles Messier discovered M30 in 1764.",
     "Messier cataloged M30 in 1764, noting its compact, comet-like glow."),
    # Group 2 — Virgo giant ellipticals
    ("M60 is a giant elliptical galaxy in the Virgo Cluster, about 60 million light-years away.",
     "M60 is a supergiant elliptical, one of the most massive galaxies in the Virgo Cluster."),
    ("M84 is a giant elliptical galaxy in the Virgo Cluster, about 60 million light-years away.",
     "M84 is a giant elliptical galaxy found along Markarian's Chain in the Virgo Cluster."),
    ("M86 is a giant elliptical galaxy in the Virgo Cluster, about 60 million light-years away.",
     "M86 is a giant elliptical in Virgo, about 60 million light-years from Earth."),
    # Group 3 — Méchain 1780
    ("Pierre Méchain discovered M72 in 1780.",
     "Pierre Méchain discovered M72 in 1780, one of the globular clusters he found that year."),
    ("Pierre Méchain discovered M74 in 1780.",
     "Pierre Méchain discovered M74 in 1780, cataloging it as a faint spiral."),
    ("Pierre Méchain discovered M76 in 1780.",
     "Pierre Méchain discovered M76 in 1780, one of his earliest planetary nebula finds."),
    # Group 4 — largest member
    ("M101 is the largest member of the M101 Group of galaxies.",
     "M101 is the brightest and largest galaxy in its own small group of galaxies."),
    ("M77 is the largest member of the M77 group of galaxies.",
     "M77 dominates its small galaxy group in Cetus."),
    # Group 5 — Virgo member
    ("M88 is a member of the Virgo Cluster.",
     "M88 belongs to the Virgo Cluster, though it lies in the constellation Coma Berenices."),
    ("M91 is a member of the Virgo Cluster.",
     "M91 is a Virgo Cluster galaxy in Coma Berenices."),
    ("M98 is a member of the Virgo Cluster.",
     "M98 belongs to the Virgo Cluster and approaches us at high speed."),
    ("M99 is a member of the Virgo Cluster.",
     "M99 resides in the Virgo Cluster, near its northern edge."),
    # Group 6 — Méchain 1781
    ("Pierre Méchain discovered M103 in 1781.",
     "Pierre Méchain discovered M103 in 1781, adding it as Messier's final cluster."),
    ("Pierre Méchain discovered M108 in 1781.",
     "Pierre Méchain found M108 in 1781 among the galaxies of Ursa Major."),
    # Group 7 — Lacaille 1752
    ("NGC 3532 was discovered by Nicolas-Louis de Lacaille in 1752.",
     "Nicolas-Louis de Lacaille discovered NGC 3532 in 1752 during his expedition to South Africa."),
    ("NGC 6025 was discovered by Nicolas-Louis de Lacaille in 1752.",
     "Lacaille cataloged NGC 6025 in 1752 while mapping the southern sky."),
    # Group 8 — Caroline 1783
    ("NGC 253 was discovered by Caroline Herschel in 1783.",
     "Caroline Herschel discovered NGC 253 in 1783, her first great galaxy find."),
    ("NGC 7789 was discovered by Caroline Herschel in 1783.",
     "Caroline Herschel found NGC 7789 in 1783 while sweeping Cassiopeia."),
    # Group 9 — Herschel 1785
    ("NGC 1535 was discovered by William Herschel in 1785.",
     "Herschel discovered NGC 1535 in 1785 in the constellation Eridanus."),
    ("NGC 1999 was discovered by William Herschel in 1785.",
     "William Herschel discovered NGC 1999 in 1785 near the Orion Nebula."),
    ("NGC 2359 was discovered by William Herschel in 1785.",
     "Herschel found NGC 2359 in 1785 while surveying Canis Major."),
    ("NGC 246 was discovered by William Herschel in 1785.",
     "William Herschel discovered NGC 246 in 1785 in the constellation Cetus."),
    ("NGC 3242 was discovered by William Herschel in 1785.",
     "Herschel cataloged NGC 3242 in 1785, noting its planetary appearance."),
    ("NGC 4565 was discovered by William Herschel in 1785.",
     "William Herschel discovered NGC 4565 in 1785 in Coma Berenices."),
    # Group 10 — Herschel 1784
    ("NGC 6369 was discovered by William Herschel in 1784.",
     "Herschel discovered NGC 6369 in 1784, cataloging its faint ring."),
    ("NGC 6905 was discovered by William Herschel in 1784.",
     "William Herschel discovered NGC 6905 in 1784 in Delphinus."),
    # Group 11 — Herschel 1787
    ("NGC 6818 was discovered by William Herschel in 1787.",
     "Herschel found NGC 6818 in 1787 in Sagittarius."),
    ("NGC 7008 was discovered by William Herschel in 1787.",
     "William Herschel discovered NGC 7008 in 1787 in Cygnus."),
]

files = sorted(glob.glob("audit/dso_facts_batch*.py"))
total_missing = 0
for old, new in EDITS:
    found = 0
    for path in files:
        s = io.open(path, encoding="utf-8").read()
        c = s.count(old)
        if c:
            found += c
            s = s.replace(old, new)
            io.open(path, "w", encoding="utf-8").write(s)
    if found != 1:
        print("MISSING/AMBIGUOUS (%d): %s" % (found, old[:60]))
        total_missing += 1

print("done; missing=%d" % total_missing)
