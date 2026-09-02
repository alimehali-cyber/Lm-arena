package com.zig.gravity.sim

/**
 * Persistent "has the user seen the sandbox tutorial?" flag.
 *
 * ### Why this shape
 *
 * The sandbox already persists its session into `SharedPreferences("zig_gravity_sandbox")`, so
 * this reuses **that exact store** rather than introducing DataStore, a second preferences file or
 * a new dependency for a single boolean.
 *
 * The interface is deliberately tiny and Android-free so the first-launch logic can be unit-tested
 * on the JVM against [InMemoryTutorialStore]; the real implementation is a two-line adapter over
 * `SharedPreferences` created at the call site.
 *
 * ### One flag, one meaning
 *
 * "Seen" means *the user has either completed or skipped the tutorial at least once, ever*. It is
 * not per-activity, per-session or per-process. It is written on skip, on completion, and on back;
 * it is **never** written back to false, and opening the tutorial by hand from the `?` button does
 * not disturb it (§15/§26).
 */
interface TutorialStore {
    fun isTutorialSeen(): Boolean
    fun markTutorialSeen()
}

/** Test double, and the reference for what the real store must do. */
class InMemoryTutorialStore(seen: Boolean = false) : TutorialStore {
    var seen: Boolean = seen
        private set

    override fun isTutorialSeen(): Boolean = seen

    override fun markTutorialSeen() {
        seen = true
    }
}

/**
 * The first-launch decision, extracted from Compose so it can be tested.
 *
 * Returns true only when the tutorial has never been completed or skipped. Everything else — the
 * `?` button, re-opening, going back — is an explicit user action and does not consult this.
 */
object TutorialGate {
    const val PREF_KEY: String = "tutorial_seen_v1"

    fun shouldAutoShow(store: TutorialStore): Boolean = !store.isTutorialSeen()
}
