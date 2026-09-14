package com.zig.museum.tools.assetkit

/**
 * AssetKit CLI placeholder for M0.
 * Real implementation in M2: fetch, preprocess, tiles, encode, horizon, normal, pack, verify, atmosphere verbs.
 */
fun main(args: Array<String>) {
    println("assetkit placeholder — M2 will implement CLI verbs: fetch, preprocess, tiles, encode, horizon, normal, pack, verify, atmosphere")
    if (args.contains("--dry-run")) {
        println("Dry run: would process ${args.joinToString(" ")}")
    }
}
