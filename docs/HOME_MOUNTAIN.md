# Home mountain

The photographic Home scene (Real Sky and Atmospheric Sky) uses the supplied
`Mountain.kt` massif in place of the painted far range and Dimeh-Mil massif.
The supplied geometry, photo-baked material fields, shaders and lighting model
are retained. Its original window-level GLSurfaceView is replaced by a
transparent, on-demand GLES 2 TextureView so it scrolls, clips and layers with
Home. The stylised themes and fullscreen sky viewer keep their own landscapes.

The mountain is behind the Persepolis and Mil-e Azhdaha relief and its foot is
feathered into the existing valley at the monument toe. The field, river,
vegetation and monuments remain in place. Sun and Moon directions and lunar
illumination come from the same sky frame; the trees' Persian-calendar season
selects green (spring/summer), autumn or snow on the shared mountain mesh.
The view tracks the activity lifecycle and releases its EGL resources when removed.
