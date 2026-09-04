package androidx.camera.view

/**
 * HARNESS SHIM (tools/kotlin-harness, Z-P1) — NOT the real androidx.camera.view.PreviewView.
 * ARProjectionEngine.kt imports PreviewView at file scope but references it ONLY in
 * KDoc comments; this declaration exists solely to resolve the import. No members are
 * used, so none are provided.
 * WARNING: never compile on an Android classpath together with this file.
 */
class PreviewView
