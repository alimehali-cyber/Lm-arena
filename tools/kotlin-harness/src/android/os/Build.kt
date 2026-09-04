package android.os

/**
 * OFFLINE HARNESS SHIM (never ships in the APK): android.os.Build.
 * SDK_INT is `var` here so HardwareDistortionReaderTest can simulate API gates.
 * Real platform: final static int.
 */
object Build {
    object VERSION {
        var SDK_INT: Int = 34
    }
}
