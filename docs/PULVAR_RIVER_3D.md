# Sky Canvas Pulvar river

The supplied procedural river's bed, rocks, gravel bars, reeds, grass, hydraulic foam,
caustics, absorption, Fresnel reflections and Sun/Moon shading now render in Sky Canvas's
photographic landscape themes. Other themes keep their stylised ground.

## Integration

- A transparent, non-interactive TextureView uses an owned EGL/GLES 3 render thread,
  paced at 24 fps and paused with the host lifecycle. It clips and scrolls with Compose;
  it is not a window-level overlay. GPU resources are released on the GL thread.
- World-space geometry and normals are retained for lighting. An additional static
  vertex stream maps the corridor into the existing river's normalized landscape
  centerline and width. `landscapeRiverY` follows the monument slope's actual framing.
  This preserves the landscape's canal, earthworks and planting exclusions at every size.
- Only the outer floodplain fades (1.12–1.65 channel half-widths). The bed and wet banks
  stay opaque; premultiplied-alpha blending dissolves the dry banks into existing grass,
  rather than introducing a rectangular strip across the landscape.
- The same cached Paulownia and sugar-maple vegetation replay is drawn over the river,
  retaining its live lighting and Persian-calendar seasons. The existing painted river
  remains underneath as a fallback if EGL initialization fails.
- Sun/Moon positions and lunar illuminated fraction come directly from SkySceneFrame.

The original standalone camera remains available when `landscapeRiverY` is zero.
