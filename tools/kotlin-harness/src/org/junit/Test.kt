package org.junit

import kotlin.reflect.KClass

/**
 * Minimal pure-Kotlin stand-in for JUnit 4's org.junit.Test annotation.
 * Supports the subset this project uses: `expected` (exact-or-subclass match,
 * see runner) and `timeout` (enforced by the runner via a watchdog thread).
 */
annotation class Test(
    val expected: KClass<out Throwable> = Test.None::class,
    val timeout: Long = 0L
) {
    /** Sentinel mirroring org.junit.Test.None: "no exception expected". */
    class None : RuntimeException()
}

/** Stand-in for org.junit.Ignore; the runner skips any test whose class or method carries it. */
annotation class Ignore(val value: String = "")

/** Stand-in for org.junit.Before: run before EVERY test method of the class. */
annotation class Before

/** Stand-in for org.junit.After: run after EVERY test method of the class (even on failure). */
annotation class After

/** Stand-in for org.junit.BeforeClass: run once before the first test (companion/static-like). */
annotation class BeforeClass

/** Stand-in for org.junit.AfterClass: run once after the last test. */
annotation class AfterClass

/**
 * Stand-in for org.junit.Rule. The real project tests that use @Rule (Robolectric/
 * Compose UI tests) cannot compile in this harness anyway; the annotation exists only so
 * pure-Kotlin files that merely import it still resolve. Rules are NOT executed.
 */
annotation class Rule
