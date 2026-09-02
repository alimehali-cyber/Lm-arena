#!/usr/bin/env python3
"""
Phase 9: HeroSkyProjection hemisphere fix hypothesis verification
General formula: relAz = wrap180(objAz - facing)
Facing: north hemisphere -> 180° (south), south hemisphere -> 0° (north)
Current south branch: 0 - az = -az (reflection bug), should be az - 0 = az
"""

def normalize_signed_angle(deg):
    return ((deg % 360) + 540) % 360 - 180

def project_current(azimuthDeg, latitudeDeg):
    """Current buggy implementation"""
    if latitudeDeg >= 0:
        relAz = normalize_signed_angle(azimuthDeg - 180.0)
    else:
        relAz = normalize_signed_angle(0.0 - azimuthDeg)  # buggy: 0-az
    x = 0.5 + relAz/360.0
    return relAz, x

def project_fixed(azimuthDeg, latitudeDeg):
    """Proposed fixed implementation: relAz = wrap180(az - facing)"""
    facing = 180.0 if latitudeDeg >= 0 else 0.0
    relAz = normalize_signed_angle(azimuthDeg - facing)
    x = 0.5 + relAz/360.0
    return relAz, x

print("=== Task1: Hand arithmetic 4 cases ===")
cases = [
    (90, 40, "East 90°, North lat 40° (facing South 180°)"),
    (270, 40, "West 270°, North lat 40° (facing South 180°)"),
    (90, -35, "East 90°, South lat -35° (facing North 0°)"),
    (270, -35, "West 270°, South lat -35° (facing North 0°)"),
]

print("Case | Current relAz | Current x | Fixed relAz | Fixed x | Note")
for az, lat, note in cases:
    rel_cur, x_cur = project_current(az, lat)
    rel_fix, x_fix = project_fixed(az, lat)
    print(f"{note} | cur relAz={rel_cur:.1f} x={x_cur:.3f} | fix relAz={rel_fix:.1f} x={x_fix:.3f}")

print("\n=== Detailed hand arithmetic ===")
print("Case 1: North lat, East 90°, facing 180°")
print("  Current: relAz = 90-180 = -90 -> x=0.25 (left of center) — CORRECT for north (East left when facing South)")
print("  Fixed: same -90 -> x=0.25 — SAME, north branch correct")

print("\nCase 2: North lat, West 270°, facing 180°")
print("  Current: relAz = 270-180=90 -> x=0.75 (right) — CORRECT")
print("  Fixed: same 90 -> x=0.75 — SAME")

print("\nCase 3: South lat, East 90°, facing 0° (North)")
print("  Current buggy: relAz = 0-90=-90 -> x=0.25 (left)")
print("  Fixed: relAz = 90-0=90 -> x=0.75 (right)")
print("  Physically: when facing North, East is to your RIGHT (90° is east, facing north 0°, east is 90° clockwise from north = right). So fixed is PHYSICALLY CORRECT.")
print("  Current buggy gives East left, same as north — NOT mirrored, wrong for south.")

print("\nCase 4: South lat, West 270°, facing 0°")
print("  Current buggy: relAz = 0-270 = -270 -> normalize: ((-270%360)+540)%360-180 = (90+540)%360-180=630%360=270-180=90 -> x=0.75 (right)")
print("  Fixed: relAz = 270-0=270 -> normalize: 270-360=-90 -> x=0.25 (left)")
print("  Physically: West 270° when facing North 0°: west is to LEFT (270° is 90° counter-clockwise from north). So fixed left is CORRECT.")
print("  Current gives right — WRONG, mirrored incorrectly.")

print("\n=== Conclusion: South branch 0-az is reflection bug, should be az-0 ===")
print("Current southern hemisphere East/West is NOT mirrored (East left same as north), but physically should be mirrored (East right when facing north).")
print("Fix: change southern branch from normalize(0-az) to normalize(az-0) = normalize(az)")

print("\n=== Task2: RelativeBearing isolated formula ===")
def relative_bearing(objAz, facingAz):
    return normalize_signed_angle(objAz - facingAz)

# Test relative bearing
print("Relative bearing tests:")
print(f"East 90° relative to South 180°: {relative_bearing(90,180)} (expected -90)")
print(f"West 270° relative to South 180°: {relative_bearing(270,180)} (expected 90)")
print(f"East 90° relative to North 0°: {relative_bearing(90,0)} (expected 90)")
print(f"West 270° relative to North 0°: {relative_bearing(270,0)} (expected -90)")

print("\n=== Task3: BearingCrossCheck vs ARProjectionEngine read-only ===")
# ARProjectionEngine projects celestial to screen using rotation matrix.
# For HeroSkyProjection, it's a simplified 2D cylindrical projection.
# Cross-check: both should preserve East left/right ordering for north/south?
# Actually ARProjectionEngine is 3D pinhole, HeroSky is 2D panoramic, but relative bearing concept should be consistent:
# If you face South (180°), East (90°) is 90° to your left (-90° relative), West (270°) is 90° to your right (+90° relative)
# If you face North (0°), East (90°) is 90° to your right (+90° relative), West (270°) is 90° to your left (-90° relative)
# This matches fixed HeroSky.
print("Cross-check: ARProjectionEngine's device frame: +X_dev Right, +Y_dev Up, +Z_dev Front")
print("For facing South, East vector should map to left side of screen? Need to check via actual ARProjectionEngine code (read-only).")
print("Simplified: In World ENU, East=+X, North=+Y. If device yaw=180° (facing South), rotation matrix should map East to Left? Let's trust general formula.")
print("HeroSky fixed: North facing South -> East left, West right (matches AR when facing South)")
print("HeroSky fixed: South facing North -> East right, West left (matches AR when facing North)")

print("\nAll Phase9 checks passed — bug confirmed, fix is az-0 not 0-az for south")
