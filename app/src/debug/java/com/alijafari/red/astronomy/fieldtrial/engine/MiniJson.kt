package com.alijafari.red.astronomy.fieldtrial.engine

/**
 * G-1.3 support: minimal JSON parser + writer (pure Kotlin) so the trial document can
 * be persisted and RESTORED (process death / reboot) without Android dependencies.
 * Handles the full JSON grammar used by trial files: objects, arrays, strings with
 * escapes, doubles, booleans, null. Longs are carried as doubles (epoch ms < 2^53,
 * exactly representable). Harness + CI tested including mutation guards.
 */
sealed class Json {
    data class JStr(val v: String) : Json()
    data class JNum(val v: Double) : Json()
    data class JBool(val v: Boolean) : Json()
    object JNull : Json()

    data class JArr(val items: List<Json>) : Json()

    data class JObj(val fields: LinkedHashMap<String, Json>) : Json() {
        operator fun get(key: String): Json? = fields[key]
    }

    companion object {
        fun parse(s: String): Json {
            val p = Parser(s)
            p.skipWs()
            val v = p.parseValue()
            p.skipWs()
            if (!p.atEnd()) throw JsonError("trailing characters at ${p.i}")
            return v
        }

        fun write(j: Json): String = StringBuilder().also { writeTo(j, it) }.toString()

        private fun writeTo(j: Json, sb: StringBuilder) {
            when (j) {
                is JStr -> writeString(j.v, sb)
                is JNum -> {
                    val v = j.v
                    when {
                        v.isNaN() -> sb.append("\"NaN\"")
                        v == Double.POSITIVE_INFINITY -> sb.append("\"Infinity\"")
                        v == Double.NEGATIVE_INFINITY -> sb.append("\"-Infinity\"")
                        v == v.toLong().toDouble() -> sb.append(v.toLong().toString())
                        else -> sb.append(v.toString())
                    }
                }
                is JBool -> sb.append(j.v)
                JNull -> sb.append("null")
                is JArr -> {
                    sb.append('[')
                    j.items.forEachIndexed { i, item ->
                        if (i > 0) sb.append(',')
                        writeTo(item, sb)
                    }
                    sb.append(']')
                }
                is JObj -> {
                    sb.append('{')
                    var first = true
                    for ((k, v) in j.fields) {
                        if (!first) sb.append(',')
                        first = false
                        writeString(k, sb)
                        sb.append(':')
                        writeTo(v, sb)
                    }
                    sb.append('}')
                }
            }
        }

        private fun writeString(s: String, sb: StringBuilder) {
            sb.append('"')
            for (c in s) {
                when (c) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '\b' -> sb.append("\\b")
                    '\u000C' -> sb.append("\\f")
                    else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            sb.append('"')
        }
    }

    class JsonError(message: String) : Exception(message)

    private class Parser(val s: String) {
        var i = 0

        fun atEnd() = i >= s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Json {
            if (atEnd()) throw JsonError("unexpected end")
            return when (s[i]) {
                '{' -> parseObj()
                '[' -> parseArr()
                '"' -> JStr(parseString())
                't' -> literal("true", JBool(true))
                'f' -> literal("false", JBool(false))
                'n' -> literal("null", JNull)
                else -> parseNum()
            }
        }

        private fun literal(word: String, v: Json): Json {
            if (!s.startsWith(word, i)) throw JsonError("bad literal at $i")
            i += word.length
            return v
        }

        private fun parseObj(): JObj {
            expect('{')
            val map = LinkedHashMap<String, Json>()
            skipWs()
            if (peek() == '}') { i++; return JObj(map) }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                map[key] = parseValue()
                skipWs()
                when (peek()) {
                    ',' -> { i++ }
                    '}' -> { i++; return JObj(map) }
                    else -> throw JsonError("expected , or } at $i")
                }
            }
        }

        private fun parseArr(): JArr {
            expect('[')
            val items = ArrayList<Json>()
            skipWs()
            if (peek() == ']') { i++; return JArr(items) }
            while (true) {
                skipWs()
                items.add(parseValue())
                skipWs()
                when (peek()) {
                    ',' -> { i++ }
                    ']' -> { i++; return JArr(items) }
                    else -> throw JsonError("expected , or ] at $i")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw JsonError("unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (atEnd()) throw JsonError("bad escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw JsonError("bad unicode escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> throw JsonError("bad escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseNum(): JNum {
            val start = i
            if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
            while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '-' || s[i] == '+')) i++
            val text = s.substring(start, i)
            val v = text.toDoubleOrNull() ?: throw JsonError("bad number '$text' at $start")
            return JNum(v)
        }

        private fun expect(c: Char) {
            if (atEnd() || s[i] != c) throw JsonError("expected $c at $i")
            i++
        }

        private fun peek(): Char {
            if (atEnd()) throw JsonError("unexpected end")
            return s[i]
        }
    }
}

// ---- convenience accessors ----

fun Json?.asObjOrNull(): Json.JObj? = this as? Json.JObj
fun Json?.asArrOrNull(): Json.JArr? = this as? Json.JArr
fun Json?.asStringOrNull(): String? = (this as? Json.JStr)?.v
fun Json?.asDoubleOrNull(): Double? = (this as? Json.JNum)?.v
fun Json?.asBoolOrNull(): Boolean? = (this as? Json.JBool)?.v
fun Json?.asLongOrNull(): Long? = (this as? Json.JNum)?.v?.toLong()

fun jobjOf(vararg pairs: Pair<String, Json>): Json.JObj = Json.JObj(LinkedHashMap<String, Json>().apply { pairs.forEach { put(it.first, it.second) } })
fun jarrOf(items: List<Json>): Json.JArr = Json.JArr(items)
fun jstr(s: String?): Json = s?.let { Json.JStr(it) } ?: Json.JNull
fun jnum(d: Double?): Json = d?.let { Json.JNum(it) } ?: Json.JNull
fun jnum(l: Long?): Json = l?.let { Json.JNum(it.toDouble()) } ?: Json.JNull
fun jbool(b: Boolean?): Json = b?.let { Json.JBool(it) } ?: Json.JNull
