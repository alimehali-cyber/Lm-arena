# Aether Telemetry UI

Select **Settings → Interface Style → Aether Telemetry UI**. The choice persists like the other
interface styles. Aether uses a dark canvas independently of the selected light/wine theme; the
previous theme is retained when switching styles.

The dashboard adapts the proposed intake mockup to this astronomy app: illumination, lunar phase and
age come from the app's Moon calculations, sun altitude from the current sky, and the Bortle value
from settings. The arrows preview the Moon a day at a time (within the existing ±30 day limit), and
tapping the hero or dial opens the Moon details. The city and settings capsules open their existing
controls. The original Home feed remains below the dashboard, while other tabs, navigation and
settings currently reuse the Cyber implementation to preserve all app functionality.

The ambient mesh and matrix readouts use Compose Canvas, including on API 24–32; the tinted glass
surfaces are not a real background blur. Haze and API 33 RuntimeShader are not used in this version.
