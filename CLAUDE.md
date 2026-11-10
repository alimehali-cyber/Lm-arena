# Aether Telemetry design guidance

These rules apply to the **Aether UI option**, not the existing Default, Spatial or Cyber designs.
Preserve their behavior and keep the astronomy data and navigation functional when extending Aether.

- Use a near-black canvas with slow, restrained ambient color drift. Keep text readable over it.
- Cards are single, frameless rounded surfaces with a thin directional specular rim, not double bezels.
  Hero cards use dark-centered jewel gradients; secondary cards use translucent or softly tinted glass.
- Use dot-matrix readouts for short telemetry values and light, legible sans-serif labels. Use the
  existing Canvas-based `CyberMatrixText` for numeric glyphs, including Persian digits.
- Draw gauges and waveforms with Compose Canvas. Animate value changes and give interactive controls
  spring-based press feedback; use haptics on steppers where supported.
- Use real app state and astronomy calculations, distinguish simulated values from live values, and
  provide accessible labels and adequate touch targets. Support English and Persian and small screens.
- Keep a graceful fallback for Android versions below API 33 when considering RuntimeShader or blur.
