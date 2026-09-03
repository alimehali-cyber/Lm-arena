package runner

import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Minimal JUnit-4-compatible reflection runner for the pure-Kotlin test subset.
 *
 * Deliberate semantics (documented in docs/startracker/evidence/HARNESS_DISCLOSURE.md):
 *  - Test discovery: instance methods annotated @Test on the classes named as CLI args.
 *  - @Before/@After run around EVERY test method; @BeforeClass/@AfterClass once per class
 *    (declared on a companion object OR any static-like declaration the JVM exposes).
 *  - @Ignore (on method or class) => skipped, reported on a separate "skipped" line and
 *    NOT counted in "Tests run".
 *  - `expected`: PASS iff the method throws a Throwable that is an INSTANCE OF the
 *    annotated class (subclass match, matching JUnit 4 semantics — not exact class).
 *  - `timeout`: enforced by running the test body on a worker thread and joining with
 *    the limit; on expiry the worker is interrupted and the test is reported as a
 *    FAILURE (AssertionError "test timed out"), like JUnit 4's FailOnTimeout.
 *  - Failure vs Error (JUnit 4 distinction, preserved): AssertionError => "Failure",
 *    any other Throwable (incl. timeout/Interrupted) => "Error".
 *  - A test class whose instantiation fails counts every discovered @Test as an Error.
 *  - Exceptions thrown out of @Before/@After are reported as Errors for the affected test.
 *  - Exit code: 0 iff Failures == 0 && Errors == 0, else 1.
 */

private var totalTests = 0
private var totalFailures = 0
private var totalErrors = 0
private val details = StringBuilder()

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("usage: runner.MainKt <fully.qualified.TestClass> [...]")
        kotlin.system.exitProcess(2)
    }
    for (className in args) {
        runClass(className)
    }
    println()
    println("========================================")
    println("Tests run: $totalTests, Failures: $totalFailures, Errors: $totalErrors")
    if (totalFailures == 0 && totalErrors == 0) {
        println("OK (all tests passed)")
    }
    if (details.isNotEmpty()) {
        println("--- failure details ---")
        print(details)
        println("========================================")
    }
    println()
    if (totalFailures != 0 || totalErrors != 0) kotlin.system.exitProcess(1)
}

private fun runClass(className: String) {
    val clazz = try {
        Class.forName(className)
    } catch (e: Throwable) {
        println("ERROR  cannot load class $className: $e")
        return
    }

    val classIgnore = clazz.getAnnotation(Ignore::class.java) != null
    val testMethods = clazz.methods.filter { it.getAnnotation(Test::class.java) != null }
    if (testMethods.isEmpty()) {
        println("NOTE   $className: no @Test methods discovered")
        return
    }

    val beforeMethods = clazz.methods.filter { it.getAnnotation(Before::class.java) != null }
    val afterMethods = clazz.methods.filter { it.getAnnotation(After::class.java) != null }
    val beforeClassMethods = clazz.methods.filter {
        it.getAnnotation(BeforeClass::class.java) != null && Modifier.isStatic(it.modifiers)
    }
    val afterClassMethods = clazz.methods.filter {
        it.getAnnotation(AfterClass::class.java) != null && Modifier.isStatic(it.modifiers)
    }

    beforeClassMethods.forEach { invokeStatic(it, className) }

    for (method in testMethods) {
        val testName = "$className.${method.name}"
        if (classIgnore || method.getAnnotation(Ignore::class.java) != null) {
            println("SKIP   $testName")
            continue
        }
        totalTests++
        val ann = method.getAnnotation(Test::class.java)!!
        val instance = try {
            clazz.getDeclaredConstructor().newInstance()
        } catch (e: InvocationTargetException) {
            recordError(testName, "instantiation failed: ${e.cause ?: e}")
            continue
        } catch (e: Throwable) {
            recordError(testName, "instantiation failed: $e")
            continue
        }

        var outcome: Outcome? = null
        try {
            beforeMethods.forEach { it.invoke(instance) }
            outcome = runBody(instance, method, ann)
        } catch (e: InvocationTargetException) {
            outcome = Outcome(e.cause ?: e)
        } catch (e: Throwable) {
            outcome = Outcome(e)
        } finally {
            try {
                afterMethods.forEach { it.invoke(instance) }
            } catch (e: InvocationTargetException) {
                if (outcome == null) outcome = Outcome(e.cause ?: e)
            }
        }

        report(testName, outcome, ann)
    }

    afterClassMethods.forEach { invokeStatic(it, className) }
}

private class Outcome(val thrown: Throwable?, val timedOut: Boolean = false)

private fun runBody(instance: Any, method: java.lang.reflect.Method, ann: Test): Outcome {
    if (ann.timeout <= 0L) {
        return try {
            method.invoke(instance)
            Outcome(null)
        } catch (e: InvocationTargetException) {
            Outcome(e.cause ?: e)
        }
    }
    val captured = arrayOfNulls<Throwable>(1)
    val worker = Thread {
        try {
            method.invoke(instance)
        } catch (e: InvocationTargetException) {
            captured[0] = e.cause ?: e
        } catch (e: Throwable) {
            captured[0] = e
        }
    }
    worker.isDaemon = true
    worker.start()
    worker.join(TimeUnit.MILLISECONDS.toMillis(ann.timeout))
    return if (worker.isAlive) {
        worker.interrupt()
        Outcome(TimeoutException("test timed out after ${ann.timeout} ms"), timedOut = true)
    } else {
        Outcome(captured[0])
    }
}

private fun report(testName: String, outcome: Outcome?, ann: Test) {
    val thrown = outcome?.thrown
    when {
        thrown == null -> println("PASS   $testName")
        ann.expected.java.isInstance(thrown) -> println("PASS   $testName")
        else -> {
            if (thrown is AssertionError) {
                totalFailures++
                println("FAILURE $testName -> ${thrown.message}")
                details.append("$testName: [FAILURE] ${thrown.message}\n")
            } else {
                totalErrors++
                println("ERROR  $testName -> ${thrown::class.java.name}: ${thrown.message}")
                details.append("$testName: [ERROR] ${thrown::class.java.name}: ${thrown.message}\n")
            }
        }
    }
}

private fun recordError(testName: String, msg: String) {
    totalErrors++
    println("ERROR  $testName -> $msg")
    details.append("$testName: [ERROR] $msg\n")
}

private fun invokeStatic(method: java.lang.reflect.Method, className: String) {
    try {
        method.invoke(null)
    } catch (e: InvocationTargetException) {
        println("ERROR  $className @BeforeClass/@AfterClass ${method.name}: ${e.cause ?: e}")
    }
}
