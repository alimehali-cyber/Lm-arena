"""CPU port of SkyPanoramaShaders.FRAGMENT_SHADER, used to eyeball the night sky.

Development harness, not part of the app: it runs the same maths the GLSL does so a change to the
night can be inspected as an image instead of on a device.

    python3 tools/sky-panorama/preview_night_sky.py app/src/main/assets/sky/panorama/milkyway_2020_4k.jpg

Writes one PNG per sky state to the directory it is run from. Requires numpy and pillow. Keep it in
step with SkyNightModel.kt and SkyPanoramaShaders.kt by hand; the Kotlin side is the source of truth
and SkyNightModelTest pins the numbers it depends on.
"""
import numpy as np, math, sys
from PIL import Image

PI = math.pi
GAL_POLE = np.array([-0.05487556, -0.87343709, -0.48383502])
ECL_POLE = np.array([0.0, -0.3977772, 0.9174821])

ZERO = 108000.0
def m2l(m): return ZERO * 10 ** (-0.4 * m)
BORTLE = {1:21.99,2:21.94,3:21.79,4:21.09,5:20.0,6:19.22,7:18.66,8:18.1,9:17.6}
NAT = m2l(21.95)
GAL_PEAK = m2l(18.9)
ZOD = m2l(23.0); GEG = m2l(23.8)
FULL_MOON = m2l(18.2)
GALAXY_GAMMA = 0.90
TZ = 0.83
LUMA = np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)

def airmass(h):
    h = np.maximum(h, -0.9)
    return 1.0 / (np.sin(np.radians(h)) + 0.50572 * np.power(h + 6.07995, -1.6364))

def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)

def display_key(a):
    t = np.clip((np.log10(np.maximum(a, 1e-6)) + 4.0) / 7.5, 0, 1)
    s = t * t * (3 - 2 * t)
    return 0.30 * (0.06 + 0.94 * np.power(s, 1.6))

def srgb_to_linear(c):
    return np.where(c <= 0.04045, c / 12.92, np.power((c + 0.055) / 1.055, 2.4))

def linear_to_srgb(c):
    c = np.clip(c, 0, 1)
    return np.where(c <= 0.0031308, c * 12.92, 1.055 * np.power(c, 1/2.4) - 0.055)

def aces(x):
    v = np.maximum(x, 0)
    return np.clip((v * (2.51*v + 0.03)) / (v * (2.43*v + 0.59) + 0.14), 0, 1)

def twilight_zenith(sun_alt):
    dep = max(-sun_alt, 0.0)
    return float(m2l(10.5) * 10 ** (-0.76 * (dep - 12.0)) * (1 - smoothstep(-6.0, -2.0, sun_alt)))

def night_sky(sun_alt, moon_alt, moon_illum, bortle, cam_alt):
    tauV = -math.log(TZ)
    tau = np.array([0.34, 0.62, 1.02]) * tauV
    natural = NAT
    artificial = max(0.0, m2l(BORTLE[bortle]) - natural)
    above = float(smoothstep(-3.0, 5.0, moon_alt))
    illum = min(max(moon_illum/100, 0), 1)
    if above > 0 and illum > 0:
        xm = max(airmass(max(moon_alt, -1.0)), 1.0)
        moon_sky = FULL_MOON * (0.35 + 0.65*illum) * above * TZ ** (xm - 1)
    else:
        moon_sky = 0.0
    adapt = max(min(natural + artificial, natural*1.18 + artificial*2.4) + moon_sky*0.35, 1e-7)
    xc = airmass(cam_alt)
    glare = natural*xc*TZ**(xc-1) + artificial*(0.40 + 2.5*math.exp(-max(cam_alt,0.4)/7.5)*1.28)
    key = max(0.65*adapt + 0.35*max(glare,1e-7) + 0.45*twilight_zenith(sun_alt), 1e-7)
    t = min(max((math.log10(max(key,1e-6)) + 3.0)/3.0, 0.0), 1.0)
    scotopic = 1.0 - t*t*(3-2*t)
    return dict(tau=tau, airglow=natural, lp=artificial, moon_sky=moon_sky,
                exposure=float(display_key(key)/key), scotopic=scotopic,
                # SkyNightModel.nightSkyWeight: 0 above -4, 1 below -10.
                w=float(smoothstep(-4.0, -10.0, sun_alt)))

def patch(az, alt, seed):
    a, b = np.radians(az), np.radians(alt)
    v = np.sin(3*a + 1.7*b + seed)*np.cos(2*a - 2.3*b + 0.7*seed) \
      + 0.6*np.sin(5*a + 3.1*b - 0.4 + seed*1.3)*np.cos(4*a + 1.9*b)
    return 0.5 + 0.5*(v/1.6)

def render(out, plate_path, width, height, lst, lat, sun_alt, sun_az, moon_alt, moon_az,
           moon_illum, bortle, fovY, cam_alt, cam_az):
    N = night_sky(sun_alt, moon_alt, moon_illum, bortle, cam_alt)
    plate = np.asarray(Image.open(plate_path).convert('RGB'), dtype=np.float32) / 255.0
    ph, pw, _ = plate.shape

    p = np.radians(lst); phi = np.radians(lat)
    n = np.array([-np.sin(phi)*np.cos(p), -np.sin(phi)*np.sin(p), np.cos(phi)])
    e = np.array([-np.sin(p), np.cos(p), 0.0])
    z = np.array([np.cos(phi)*np.cos(p), np.cos(phi)*np.sin(p), np.sin(phi)])
    def d(az, alt):
        a, h = np.radians(az), np.radians(alt)
        return np.cos(h)*np.cos(a)*n + np.cos(h)*np.sin(a)*e + np.sin(h)*z
    forward = d(cam_az, cam_alt)
    hint = z if abs(np.dot(forward, z)) < 0.9 else e
    right = np.cross(forward, hint); right /= np.linalg.norm(right)
    up = np.cross(right, forward)

    tanY = math.tan(math.radians(fovY)/2)
    tanX = tanY * width / height
    xs = (np.arange(width)+0.5)/width*2 - 1
    ys = 1 - (np.arange(height)+0.5)/height*2
    gx, gy = np.meshgrid(xs, ys)
    dirv = (forward[None,None,:] + gx[...,None]*tanX*right[None,None,:] + gy[...,None]*tanY*up[None,None,:])
    dirv /= np.linalg.norm(dirv, axis=2, keepdims=True)

    alt = np.degrees(np.arcsin(np.clip(dirv @ z, -1, 1)))
    az = np.degrees(np.arctan2(dirv @ e, dirv @ n)); az = np.where(az < 0, az+360, az)

    ra = np.arctan2(dirv[...,1], dirv[...,0]); dec = np.arcsin(np.clip(dirv[...,2], -1, 1))
    u = np.mod(0.5 - ra/(2*PI), 1.0); v = 0.5 - dec/PI
    px = np.clip((u*pw).astype(int), 0, pw-1); py = np.clip((v*ph).astype(int), 0, ph-1)
    tex = plate[py, px]

    galLin = srgb_to_linear(np.clip((tex - 0.105)/0.895, 0, 1))
    galLum = np.clip(galLin @ LUMA, 0, 1)
    galLat = np.degrees(np.arcsin(np.clip(dirv @ GAL_POLE, -1, 1)))
    band = 0.50 + 0.50*np.exp(-0.5*(galLat/12.0)**2)
    halo = np.exp(-0.5*(galLat/34.0)**2)
    warm = np.clip(galLum*1.9, 0, 1)*np.exp(-0.5*(galLat/16.0)**2)
    cool = halo*(1 - np.clip(galLum*3.2, 0, 1))
    tint = np.stack([np.full_like(galLum,0.97), np.full_like(galLum,0.99), np.full_like(galLum,1.04)], -1)
    tint = tint + (np.array([1.10,1.00,0.86]) - np.array([0.97,0.99,1.04]))[None,None,:]*warm[...,None]
    tint = tint + (np.array([0.86,0.95,1.12])[None,None,:] - tint)*0.45*cool[...,None]

    ext = np.exp(-N['tau']*airmass(np.maximum(alt, 0.0))[...,None])
    galaxy = GAL_PEAK*np.power(np.maximum(galLum, 0), GALAXY_GAMMA)[...,None]*band[...,None]*tint
    galaxy = galaxy*ext*smoothstep(-0.8, 2.6, alt)[...,None]

    x = airmass(alt)
    glow = N['airglow']*x*np.power(np.clip(TZ, 0.02, 1), x-1)
    glow = glow*smoothstep(-1.2, 3.0, alt)*(0.84 + 0.32*patch(az, alt, 0.0))
    domes = np.ones_like(az)
    for caz, w, s in ((28.0, 1.00, 38.0), (146.0, 0.50, 24.0), (262.0, 0.28, 62.0)):
        dAz = np.abs(np.mod(az - caz + 180, 360) - 180)
        domes = domes + w*np.exp(-0.5*(dAz/s)**2)
    lp = N['lp']*(0.40 + 2.5*np.exp(-np.maximum(alt, 0.4)/7.5)*(0.28+domes))
    lp = lp*(0.85 + 0.30*patch(az, alt, 3.1))*smoothstep(-2.2, 2.2, alt)

    sunDir = d(sun_az, sun_alt); moonDir = d(moon_az, moon_alt)
    sunAngle = np.degrees(np.arccos(np.clip(dirv @ sunDir, -1, 1)))
    eclLat = np.degrees(np.arcsin(np.clip(dirv @ ECL_POLE, -1, 1)))
    zl = ZOD*np.exp(-0.5*(eclLat/9.0)**2)*(0.25 + 0.75*smoothstep(25, 100, sunAngle))
    zl = zl + GEG*np.exp(-0.5*(eclLat/5.5)**2)*np.exp(-0.5*((sunAngle-180)/17.0)**2)
    zodiacal = zl[...,None]*ext*smoothstep(-1.2, 2.5, alt)[...,None]*np.array([1.0,0.95,0.86])

    rho = np.degrees(np.arccos(np.clip(dirv @ moonDir, -1, 1)))
    cr = np.cos(np.radians(rho))
    fRho = 1.0e5*(1.06+cr*cr) + 1.0e6*np.exp(-rho/40.0)
    fZero = 1.0e5*2.06 + 1.0e6
    moon = (N['moon_sky']*(fRho/fZero))[...,None]*ext*(1-np.exp(-x*1.2))[...,None]
    moon = moon*smoothstep(-1.2, 3.0, alt)[...,None]*np.array([0.82,0.95,1.28])

    G_TW = 0.72
    hg = (1-G_TW*G_TW)/(4*PI*np.power(1+G_TW*G_TW-2*G_TW*np.cos(np.radians(sunAngle)), 1.5))
    hg0 = (1-G_TW*G_TW)/(4*PI*(1+G_TW*G_TW-2*G_TW)**1.5)
    tw = twilight_zenith(sun_alt)*(0.55 + 0.45*np.exp(-np.maximum(alt, 0.0)/9.0))*(0.25 + 1.50*(hg/hg0))
    rayl = 0.75*(1+np.cos(np.radians(sunAngle))**2)/1.5
    warmMix = np.clip(rayl*0.55*np.exp(-np.maximum(alt,0.0)/26.0), 0, 1)
    twColour = (np.array([0.44,0.60,1.0])[None,None,:]
                + (np.array([1.30,0.72,0.42])-np.array([0.44,0.60,1.0]))[None,None,:]*warmMix[...,None])
    twilight = tw[...,None]*twColour
    anti = np.exp(-0.5*((180-sunAngle)/45.0)**2)*np.exp(-0.5*((alt-5.0)/9.0)**2)
    twilight = twilight*(1 - 0.6*anti)[...,None]

    night = (galaxy + glow[...,None]*np.array([0.86,1.00,1.14])
             + lp[...,None]*np.array([1.14,0.96,0.78])
             + zodiacal + moon + twilight)

    c = np.maximum(night, 0)*N['exposure']
    scot = 0.36*c[...,1] + 0.62*c[...,2] + 0.02*c[...,0]
    c = c + (scot[...,None]*np.array([0.62,0.80,1.18]) - c)*(0.55*N['scotopic'])
    y = c @ LUMA
    yd = aces(y)
    c = c*(yd/np.maximum(y,1e-12))[...,None]
    m = c.max(axis=2)
    sat = np.clip((1-yd)/np.maximum(m-yd, 1e-9), 0, 1)
    c = np.where((m > 1)[...,None], yd[...,None] + (c-yd[...,None])*sat[...,None], c)
    o = np.clip(c, 0, 1)
    l = o @ LUMA
    o = l[...,None] + (o - l[...,None])*1.0
    o = (o-0.5)*1.0 + 0.5
    night_srgb = linear_to_srgb(np.clip(o, 0, 1))
    night_srgb = night_srgb*(0.06 + 0.94*smoothstep(-1.5, 0.5, alt))[...,None]

    day = np.broadcast_to(np.array([0.02,0.05,0.13], dtype=np.float32), night_srgb.shape)
    sky = day + (night_srgb - day)*N['w']
    Image.fromarray((np.clip(sky,0,1)*255).astype(np.uint8)).save(out)
    print(f"{out} exposure {N['exposure']:.2f} moon_sky {N['moon_sky']:.2e} weight {N['w']:.3f} "
          f"twilight {twilight_zenith(sun_alt):.2e} scotopic {N['scotopic']:.2f}")

if __name__ == '__main__':
    plate = sys.argv[1]
    W, H = 900, 560
    render('/home/user/preview/night_b4_moon.png', plate, W, H, lst=250.0, lat=35.7,
           sun_alt=-25.0, sun_az=300.0, moon_alt=40.0, moon_az=140.0, moon_illum=80.0,
           bortle=4, fovY=104.0, cam_alt=40.0, cam_az=180.0)
    render('/home/user/preview/night_b2_nomoon.png', plate, W, H, lst=250.0, lat=35.7,
           sun_alt=-30.0, sun_az=320.0, moon_alt=-30.0, moon_az=140.0, moon_illum=5.0,
           bortle=2, fovY=104.0, cam_alt=40.0, cam_az=180.0)
    render('/home/user/preview/night_b8_city.png', plate, W, H, lst=250.0, lat=35.7,
           sun_alt=-30.0, sun_az=320.0, moon_alt=-30.0, moon_az=140.0, moon_illum=5.0,
           bortle=8, fovY=104.0, cam_alt=40.0, cam_az=180.0)
    render('/home/user/preview/twilight_deep.png', plate, W, H, lst=250.0, lat=35.7,
           sun_alt=-14.0, sun_az=290.0, moon_alt=-20.0, moon_az=200.0, moon_illum=10.0,
           bortle=3, fovY=104.0, cam_alt=40.0, cam_az=250.0)
    render('/home/user/preview/twilight_mid.png', plate, W, H, lst=250.0, lat=35.7,
           sun_alt=-9.0, sun_az=290.0, moon_alt=15.0, moon_az=200.0, moon_illum=30.0,
           bortle=3, fovY=104.0, cam_alt=40.0, cam_az=250.0)
