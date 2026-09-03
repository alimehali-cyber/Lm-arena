@file:JvmName("Assert")

package org.junit.Assert

import kotlin.math.abs

/**
 * Minimal pure-Kotlin stand-in for JUnit 4's org.junit.Assert.
 *
 * Declared as TOP-LEVEL FUNCTIONS in package `org.junit.Assert` (NOT an object) so that
 * BOTH import styles used by this project's tests resolve:
 *   - `import org.junit.Assert.assertEquals`  (single declaration import)
 *   - `import org.junit.Assert.*`             (star import over the package)
 * A Kotlin `object Assert` would make the star import illegal ("cannot import from object"),
 * which is why this file is a package of functions rather than an object.
 *
 * Semantics mirror JUnit 4: failure => AssertionError with the given message.
 */

fun fail(message: String?): Nothing = throw AssertionError(message ?: "fail")

fun assertTrue(message: String?, condition: Boolean) {
    if (!condition) fail(message)
}

fun assertTrue(condition: Boolean) = assertTrue(null, condition)

fun assertFalse(message: String?, condition: Boolean) = assertTrue(message, !condition)

fun assertFalse(condition: Boolean) = assertFalse(null, condition)

fun assertNull(message: String?, obj: Any?) = assertTrue(message, obj == null)

fun assertNull(obj: Any?) = assertNull(null, obj)

fun assertNotNull(message: String?, obj: Any?) = assertTrue(message, obj != null)

fun assertNotNull(obj: Any?) = assertNotNull(null, obj)

fun assertEquals(message: String?, expected: Any?, actual: Any?) {
    if (expected == null && actual == null) return
    if (expected != null && expected == actual) return
    fail((message?.let { "$it " } ?: "") + "expected:<$expected> but was:<$actual>")
}

fun assertEquals(expected: Any?, actual: Any?) = assertEquals(null, expected, actual)

fun assertEquals(message: String?, expected: Double, actual: Double, delta: Double) {
    if (java.lang.Double.isInfinite(expected)) {
        assertEquals(message, expected as Any?, actual as Any?)
        return
    }
    if (abs(expected - actual) <= delta) return
    fail((message?.let { "$it " } ?: "") + "expected:<$expected> but was:<$actual> (delta $delta)")
}

fun assertEquals(expected: Double, actual: Double, delta: Double) =
    assertEquals(null, expected, actual, delta)

fun assertEquals(message: String?, expected: Float, actual: Float, delta: Float) {
    if (abs(expected - actual) <= delta) return
    fail((message?.let { "$it " } ?: "") + "expected:<$expected> but was:<$actual> (delta $delta)")
}

fun assertEquals(expected: Float, actual: Float, delta: Float) =
    assertEquals(null, expected, actual, delta)

fun assertEquals(message: String?, expected: Long, actual: Long) =
    assertEquals(message, expected as Any?, actual as Any?)

fun assertEquals(expected: Long, actual: Long) = assertEquals(null, expected as Any?, actual as Any?)

fun assertArrayEquals(message: String?, expected: Array<out Any?>, actual: Array<out Any?>) {
    assertTrue(message, expected.contentEquals(actual))
}

fun assertArrayEquals(expected: Array<out Any?>, actual: Array<out Any?>) =
    assertArrayEquals(null, expected, actual)

fun assertArrayEquals(message: String?, expected: DoubleArray, actual: DoubleArray) {
    assertTrue(message, expected.contentEquals(actual))
}

fun assertArrayEquals(expected: DoubleArray, actual: DoubleArray) =
    assertArrayEquals(null, expected, actual)
