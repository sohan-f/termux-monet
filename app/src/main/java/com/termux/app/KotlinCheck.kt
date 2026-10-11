package com.termux.app

/**
 * Smoke test for the Kotlin toolchain. Deliberately unreferenced: its only job
 * is to prove kotlinc runs for every build until real Kotlin code lands.
 * Safe to delete once the first production Kotlin file is added.
 */
object KotlinCheck {
    @JvmStatic
    fun toolchain(): String = "kotlin-ok"
}
